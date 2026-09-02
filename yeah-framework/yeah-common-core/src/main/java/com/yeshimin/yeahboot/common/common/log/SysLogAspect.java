package com.yeshimin.yeahboot.common.common.log;

import com.alibaba.fastjson2.JSON;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveDataUtils;
import com.yeshimin.yeahboot.common.domain.entity.SysLogEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 系统日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SysLogAspect {

    @Around("@annotation(sysLog)")
    public Object around(ProceedingJoinPoint joinPoint, SysLog sysLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysLogEntity logEntity = new SysLogEntity();

        // 设置注解信息
        logEntity.setEvent(sysLog.value());
        logEntity.setTriggerType(sysLog.triggerType());
        logEntity.setCategory(sysLog.category());

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
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            success = false;
            logEntity.setSuccess("0");
            logEntity.setExtra("{\"exception\": \"" + e.getMessage() + "\"}");
            throw e;
        } finally {
            long end = System.currentTimeMillis();
            logEntity.setTime((int) (end - start));
            logEntity.setSuccess(success ? "1" : "0");

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

    private boolean isLoggable(Object result) {
        if (result == null) return true;

        // 快速排除常见类型
        if (result instanceof java.io.InputStream ||
                result instanceof org.springframework.core.io.Resource ||
                result instanceof javax.servlet.ServletRequest ||
                result instanceof javax.servlet.ServletResponse ||
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
}
