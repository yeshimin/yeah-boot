package com.yeshimin.yeahboot.common.common.log;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveDataUtils;
import com.yeshimin.yeahboot.common.common.utils.ClientIpUtils;
import com.yeshimin.yeahboot.common.common.utils.WebContextUtils;
import com.yeshimin.yeahboot.common.domain.entity.SysLogEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 系统日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SysLogAspect {

    /**
     * 环绕记录带 {@link SysLog} 的接口调用，并在 finally 中统一写入耗时、结果和请求审计上下文。
     */
    @Around("@annotation(sysLog)")
    public Object around(ProceedingJoinPoint joinPoint, SysLog sysLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysLogEntity logEntity = new SysLogEntity();

        // 设置注解信息
        logEntity.setEvent(sysLog.value());
        logEntity.setTriggerType(sysLog.triggerType().getIntValue());
        logEntity.setCategory(sysLog.category().getIntValue());

        // 方法名、类名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        logEntity.setComment(method.getDeclaringClass().getName() + "#" + method.getName());

        // 收集输入参数
        try {
            Object[] args = joinPoint.getArgs();
            String inputJson = SensitiveDataUtils.toLogJson(args);
            logEntity.setInput(inputJson);
        } catch (Exception e) {
            log.warn("参数序列化异常", e);
        }

        Object result = null;
        boolean success = true;
        String exceptionMessage = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            logEntity.setSuccess(0);
            exceptionMessage = e.getMessage();
            throw e;
        } finally {
            long end = System.currentTimeMillis();
            logEntity.setTime((int) (end - start));
            logEntity.setSuccess(success ? 1 : 0);
            this.appendRequestContext(logEntity, exceptionMessage);

            // 收集输出结果
            try {
                // 优化：java.io.FileNotFoundException: InputStream resource [resource loaded through InputStream] cannot be resolved to URL
                if (result != null) {
                    boolean loggable = this.isLoggable(result);
                    if (loggable) {
                        String outputJson = SensitiveDataUtils.toLogJson(result);
                        logEntity.setOutput(outputJson);
                    } else {
                        logEntity.setOutput("[非文本响应，已跳过日志输出]");
                    }
                }

                // 发布或保存日志
                logEntity.insert();
            } catch (Exception e) {
                log.warn("结果序列化异常", e);
            }
        }
    }

    /**
     * 判断响应是否可安全序列化到审计日志；流和 Servlet 对象仅记录跳过标记。
     */
    private boolean isLoggable(Object result) {
        if (result == null) return true;

        // 快速排除常见类型
        if (result instanceof java.io.InputStream ||
                result instanceof Resource ||
                result instanceof jakarta.servlet.ServletRequest ||
                result instanceof jakarta.servlet.ServletResponse ||
                result instanceof java.io.OutputStream ||
                result instanceof java.io.Writer) {
            return false;
        }

        // 再尝试序列化（可选）
        try {
            JSON.toJSONString(result);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 写入与请求关联的审计上下文，避免将完整请求头或未脱敏参数保存到日志表。
     */
    private void appendRequestContext(SysLogEntity logEntity, String exceptionMessage) {
        JSONObject extra = new JSONObject();
        String requestId = MDC.get(MdcLogFilter.MDC_REQ_ID);
        if (requestId != null) {
            extra.put("requestId", requestId);
        }

        String terminal = WebContextUtils.getTerminal();
        if (terminal != null) {
            extra.put("terminal", terminal);
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logEntity.setClientIp(ClientIpUtils.getClientIp(request));
            extra.put("method", request.getMethod());
            extra.put("path", request.getRequestURI());
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                extra.put("userAgent", this.truncate(userAgent, 256));
            }
        }

        if (exceptionMessage != null) {
            extra.put("exception", this.truncate(exceptionMessage, 512));
        }
        if (!extra.isEmpty()) {
            logEntity.setExtra(extra.toJSONString());
        }
    }

    /**
     * 截断日志元数据，避免异常信息或 User-Agent 无限增长。
     */
    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
