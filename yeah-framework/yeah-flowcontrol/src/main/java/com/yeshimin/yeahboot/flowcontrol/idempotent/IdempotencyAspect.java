package com.yeshimin.yeahboot.flowcontrol.idempotent;

import com.alibaba.fastjson2.JSON;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.common.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * 面向返回 {@code R} 的控制器方法的 Redis 幂等状态机。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    /** 客户端提交意图唯一标识的请求头名称。 */
    public static final String HEADER_NAME = "Idempotency-Key";

    /** Redis 幂等状态记录键前缀。 */
    private static final String CACHE_KEY_PREFIX = "flow:idempotency:";

    /** 限制幂等键为 UUID，避免无界或包含特殊字符的 Redis 键输入。 */
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$");

    /** 幂等状态记录的 Redis 访问服务。 */
    private final CacheService cacheService;

    /**
     * 抢占提交意图、执行首次业务请求并记录完成状态；后续同键请求不会再次进入业务方法。
     */
    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = this.currentRequest();
        if (request == null) {
            throw new IllegalStateException("idempotency requires an HTTP request");
        }
        // 请求头仅承载提交意图标识，不接受任意长度或格式的客户端输入。
        String idempotencyKey = request.getHeader(HEADER_NAME);
        if (idempotencyKey == null || !UUID_PATTERN.matcher(idempotencyKey).matches()) {
            throw new BaseException("缺少或无效的 Idempotency-Key");
        }

        // 相同幂等键只能用于同一路由和同一份控制器参数。
        String route = request.getMethod() + ' ' + request.getRequestURI();
        String requestHash = this.sha256(JSON.toJSONString(joinPoint.getArgs()));
        String cacheKey = CACHE_KEY_PREFIX + this.sha256(route + "|" + idempotencyKey);

        IdempotencyRecord processing = new IdempotencyRecord();
        processing.setRequestHash(requestHash);
        processing.setStatus(IdempotencyRecord.Status.PROCESSING);

        // 原子抢占成功的请求才可进入业务方法，其余请求读取现有状态。
        Boolean acquired = cacheService.setIfAbsent(cacheKey, JSON.toJSONString(processing),
                Duration.ofSeconds(idempotent.ttlSeconds()));
        if (!Boolean.TRUE.equals(acquired)) {
            return this.resolveExisting(cacheKey, requestHash);
        }

        try {
            Object result = joinPoint.proceed();
            if (!(result instanceof R<?>)) {
                throw new IllegalStateException("@Idempotent methods must return R<?>");
            }

            IdempotencyRecord completed = new IdempotencyRecord();
            completed.setRequestHash(requestHash);
            completed.setStatus(IdempotencyRecord.Status.COMPLETED);
            cacheService.set(cacheKey, JSON.toJSONString(completed), idempotent.ttlSeconds());
            return result;
        } catch (Throwable e) {
            // 业务失败时释放占用，允许客户端修正参数后使用同一幂等键重试。
            cacheService.delete(cacheKey);
            throw e;
        }
    }

    /**
     * 读取同一幂等键的状态，校验请求摘要后返回处理中或已提交提示。
     */
    private Object resolveExisting(String cacheKey, String requestHash) {
        String raw = cacheService.get(cacheKey);
        IdempotencyRecord record = raw == null ? null : JSON.parseObject(raw, IdempotencyRecord.class);
        if (record == null || !requestHash.equals(record.getRequestHash())) {
            throw new BaseException("幂等键已用于其他请求");
        }
        if (record.getStatus() != IdempotencyRecord.Status.COMPLETED) {
            return R.fail("请求处理中，请勿重复提交");
        }
        return R.fail("请求已提交，请刷新后查看结果");
    }

    /**
     * 获取当前 HTTP 请求；幂等切面不支持脱离 Web 上下文直接调用。
     */
    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest();
        }
        return null;
    }

    /**
     * 计算固定长度摘要，用于 Redis 键和明文请求体一致性校验。
     */
    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(String.format("%02x", item));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

}
