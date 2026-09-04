package com.yeshimin.yeahboot.common.common.log;

import com.alibaba.fastjson2.JSON;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveDataUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

/**
 * API日志切面
 */
@Slf4j
@Aspect
@Component
public class ApiLogAspect {

    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Pointcut("execution(public * com.yeshimin.yeahboot..controller..*.*(..))")
    public void pointcut() {
    }

    @Before("pointcut()")
    public void doBefore() {
        log.debug("doBefore()");
        startTime.set(System.currentTimeMillis());
    }

    @AfterThrowing(pointcut = "pointcut()", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Throwable e) {
        log.debug("afterThrowing()");

        String errMsg = String.format("{%s, %s}", e.getClass().getName(), e.getMessage());
        this.generateLog(joinPoint, errMsg);
    }

    @AfterReturning(pointcut = "pointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        log.debug("afterReturning()");

        this.generateLog(joinPoint, result);
    }

    private void generateLog(JoinPoint joinPoint, Object result) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();

        // 获取方法参数的所有注解
        Method method = methodSignature.getMethod();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        // 判断是否有请求体（由注解@RequestBody标记的对象）
        int bodyIdx = -1;
        for (int i = 0; i < parameterAnnotations.length; ++i) {
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().equals(RequestBody.class)) {
                    bodyIdx = i;
                    break;
                }
            }
        }

        // 获取方法参数
        Object[] args = joinPoint.getArgs();

        StringBuilder sbOutput = new StringBuilder();
        // HTTP方法
        sbOutput.append(request.getMethod());
        // HTTP路径
        sbOutput.append(" - ").append(request.getRequestURI());
        // HTTP表单参数（包含查询参数），有则输出
        List<String> parameters = new ArrayList<>();
        Enumeration<String> eNames = request.getParameterNames();
        while (eNames.hasMoreElements()) {
            String name = eNames.nextElement();
            parameters.add(name + "=" + SensitiveDataUtils.maskLogParameter(
                    name, request.getParameter(name), args));
        }
        if (!parameters.isEmpty()) {
            sbOutput.append(" - ").append(String.join(";", parameters));
        }
        // HTTP请求体参数，有则输出
        if (bodyIdx >= 0) {
            sbOutput.append(" - ").append(SensitiveDataUtils.toLogJson(args[bodyIdx]));
        }
        // Java（全限定）方法名称
        sbOutput.append(" - ").append(methodSignature.getDeclaringTypeName())
                .append(".").append(methodSignature.getName())
                .append(" - time: ").append(System.currentTimeMillis() - startTime.get()).append("ms");

        // 非文本响应无法安全转换为日志文本，例如InputStreamResource无法解析为URL
        if (result != null) {
            boolean loggable = this.isLoggable(result);
            if (loggable) {
                sbOutput.append(" - return: ").append(SensitiveDataUtils.toLogJson(result));
            } else {
                sbOutput.append(" - return: [非文本响应，已跳过日志输出]");
            }
        }

        // 完整日志格式：[HTTP方法] - [HTTP路径] - [HTTP表单参数] - [HTTP请求体参数] - [Java方法名称] - [接口耗时] - [响应数据]
        String content = sbOutput.toString();
        log.info(content);
    }

    private boolean isLoggable(Object result) {
        if (result == null) return true;

        if (result instanceof ResponseEntity) {
            return this.isLoggable(((ResponseEntity<?>) result).getBody());
        }

        // 快速排除常见类型
        if (result instanceof byte[] ||
                result instanceof java.io.InputStream ||
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
