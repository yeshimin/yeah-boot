package com.yeshimin.yeahboot.common.common.exception;

import com.yeshimin.yeahboot.common.domain.base.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
     * 处理参数异常
     */
    @ExceptionHandler({
            ConversionFailedException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class,
            NoHandlerFoundException.class,
            BindException.class})
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
        } else if (e instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException ex = (MethodArgumentTypeMismatchException) e;
            msg = "参数类型错误: " + ex.getName();
        }
        // 客户端只接收受控参数提示，原始解析异常仍写入服务端日志。
        log.error("请求参数异常: {}", msg, e);
        return R.fail(msg);
    }
}
