package com.yeshimin.yeahboot.auth.common.config.security;

import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ErrorCodeEnum errorCode = ErrorCodeEnum.FORBIDDEN;
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + errorCode.getCode() +
                ",\"message\":\"" + errorCode.getDesc() + "\"}");
    }
}
