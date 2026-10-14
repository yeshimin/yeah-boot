package com.yeshimin.yeahboot.common.common.log;

import com.yeshimin.yeahboot.common.common.utils.WebContextUtils;
import lombok.SneakyThrows;
import org.slf4j.MDC;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * MDC日志过滤器
 * 另有相关逻辑见AuthService.auth()方法
 */
public class MdcLogFilter extends GenericFilterBean {

    /** MDC 中保存聚合日志文本的键。 */
    public static final String MDC_INFO = "mdc-info";

    /** MDC 中保存请求追踪标识的键。 */
    public static final String MDC_REQ_ID = "reqId";

    /** MDC 中保存当前认证用户标识的键。 */
    public static final String MDC_USER_ID = "userId";

    /**
     * 在请求进入认证与业务链前写入 MDC，并在链路结束后清理线程上下文。
     */
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

    /**
     * 写入当前请求的追踪标识和已解析用户标识。
     */
    private void setMdcInfo() {
        String uuid = UUID.randomUUID().toString();
        Long userId = WebContextUtils.getUserId();

        MDC.put(MDC_REQ_ID, uuid);
        MDC.put(MDC_USER_ID, String.valueOf(userId));

        String mdcInfo = "[reqId: " + uuid + ", userId: " + WebContextUtils.getUserId() + "] ";
        MDC.put(MDC_INFO, mdcInfo);
    }
}
