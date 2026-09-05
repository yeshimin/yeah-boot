package com.yeshimin.yeahboot.common.common.log;

import com.yeshimin.yeahboot.common.common.utils.WebContextUtils;
import lombok.SneakyThrows;
import org.slf4j.MDC;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * MDC日志过滤器
 * 另有相关逻辑见AuthService.auth()方法
 */
public class MdcLogFilter extends GenericFilterBean {

    public static final String MDC_INFO = "mdc-info";
    public static final String MDC_REQ_ID = "reqId";
    public static final String MDC_USER_ID = "userId";

    @Override
    @SneakyThrows
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        try {
            this.setMdcInfo();
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private void setMdcInfo() {
        String uuid = UUID.randomUUID().toString();
        Long userId = WebContextUtils.getUserId();

        MDC.put(MDC_REQ_ID, uuid);
        MDC.put(MDC_USER_ID, String.valueOf(userId));

        String mdcInfo = "[reqId: " + uuid + ", userId: " + WebContextUtils.getUserId() + "] ";
        MDC.put(MDC_INFO, mdcInfo);
    }
}
