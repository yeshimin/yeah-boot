package com.yeshimin.yeahboot.common.common.exception;

import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.domain.base.R;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Optional;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 未预期异常对客户端使用的通用提示，详细原因仅保留在服务端日志。 */
    private static final String SYSTEM_ERROR_MESSAGE = "系统异常，请联系管理员";

    /** 无法安全归类的请求解析异常对客户端使用的通用提示。 */
    private static final String INVALID_REQUEST_MESSAGE = "请求格式错误";

    /** 认证失败对客户端使用的统一提示。 */
    private static final String AUTH_REQUIRED_MESSAGE = "认证失败";

    /** 权限不足对客户端使用的统一提示。 */
    private static final String FORBIDDEN_MESSAGE = "权限不足";

    /**
     * 处理兜底异常
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        // 保留完整异常栈供按 traceId 排查，不能将框架、SQL 或内部实现细节回显给客户端。
        log.error("系统异常", e);
        return R.fail(SYSTEM_ERROR_MESSAGE);
    }

    /**
     * 处理基类异常
     */
    @ExceptionHandler(BaseException.class)
    public R<Void> handleBaseException(BaseException e) {
        log.error(String.format("handleBaseException(), e.code: %s, e.msg: %s",
                e.getErrorCode().getCode(), e.getMessage()), e);
        return R.fail(e.getErrorCode(), e.getMessage());
    }

    /**
     * 处理方法级鉴权中抛出的认证异常。
     *
     * <p>认证过滤器中的异常由 {@code AuthenticationEntryPoint} 处理；
     * {@code @PreAuthorize} 等方法级鉴权发生在 MVC 调用阶段，需要在这里保持相同的响应语义。</p>
     */
    @ExceptionHandler(AuthenticationException.class)
    public R<Void> handleAuthenticationException(AuthenticationException e) {
        log.warn("认证失败: {}", e.getMessage());
        return R.fail(ErrorCodeEnum.AUTH_REQUIRED, AUTH_REQUIRED_MESSAGE);
    }

    /**
     * 处理方法级鉴权中抛出的权限不足异常。
     *
     * <p>路径级拒绝由 {@code AccessDeniedHandler} 处理；
     * {@code @PreAuthorize} 等方法级拒绝会被 MVC 异常解析器捕获。</p>
     */
    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return R.fail(ErrorCodeEnum.FORBIDDEN, FORBIDDEN_MESSAGE);
    }

    /**
     * 处理参数异常
     */
    @ExceptionHandler({
            ConversionFailedException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestPartException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class})
    public R<Void> handleParamException(Exception e) {
        String msg = INVALID_REQUEST_MESSAGE;
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            msg = Optional.ofNullable(ex.getBindingResult().getFieldError())
                    .map(FieldError::getDefaultMessage)
                    .orElse("参数校验失败");
        } else if (e instanceof BindException) {
            BindException ex = (BindException) e;
            msg = Optional.ofNullable(ex.getBindingResult().getFieldError())
                    .map(FieldError::getDefaultMessage)
                    .orElse("参数校验失败");
        } else if (e instanceof MissingServletRequestParameterException) {
            MissingServletRequestParameterException ex = (MissingServletRequestParameterException) e;
            msg = "缺少参数: " + ex.getParameterName();
        } else if (e instanceof MissingRequestHeaderException) {
            MissingRequestHeaderException ex = (MissingRequestHeaderException) e;
            msg = "缺少请求头: " + ex.getHeaderName();
        } else if (e instanceof MissingServletRequestPartException) {
            MissingServletRequestPartException ex = (MissingServletRequestPartException) e;
            msg = "缺少文件参数: " + ex.getRequestPartName();
        } else if (e instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException ex = (MethodArgumentTypeMismatchException) e;
            msg = "参数类型错误: " + ex.getName();
        } else if (e instanceof ConstraintViolationException) {
            ConstraintViolationException ex = (ConstraintViolationException) e;
            msg = ex.getConstraintViolations().stream()
                    .findFirst()
                    .map(violation -> violation.getMessage())
                    .orElse("参数校验失败");
        }
        // 客户端只接收受控参数提示，原始解析异常仍写入服务端日志。
        log.error("请求参数异常: {}", msg, e);
        return R.fail(msg);
    }

    /**
     * 处理不存在的接口，避免被兜底异常处理为系统故障。
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public R<Void> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("接口不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return R.fail("接口不存在");
    }

    /**
     * 处理 HTTP 方法和媒体类型不匹配等客户端请求异常。
     */
    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class})
    public R<Void> handleHttpRequestException(Exception e) {
        String msg = "请求不支持";
        if (e instanceof HttpRequestMethodNotSupportedException) {
            msg = "请求方法不支持";
        } else if (e instanceof HttpMediaTypeNotSupportedException) {
            msg = "不支持的请求媒体类型";
        } else if (e instanceof HttpMediaTypeNotAcceptableException) {
            msg = "不支持的响应媒体类型";
        }
        log.warn("HTTP 请求异常: {}", msg, e);
        return R.fail(msg);
    }

    /**
     * 处理文件超过服务端限制，避免将 Multipart 解析错误误报为系统异常。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制", e);
        return R.fail("上传文件超过大小限制");
    }
}
