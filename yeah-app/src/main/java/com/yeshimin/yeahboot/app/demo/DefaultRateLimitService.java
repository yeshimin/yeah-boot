package com.yeshimin.yeahboot.app.demo;

import com.yeshimin.yeahboot.common.common.utils.WebContextUtils;
import com.yeshimin.yeahboot.common.common.utils.ClientIpUtils;
import com.yeshimin.yeahboot.flowcontrol.ratelimit.RateLimitService;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * App 端默认的限流分组解析实现。
 *
 * <p>默认支持用户和客户端 IP 维度；业务需要自定义分组时应提供独立实现。</p>
 */
@Service
public class DefaultRateLimitService implements RateLimitService {

    /**
     * 默认不提供自定义分组名称。
     *
     * @param request 当前 HTTP 请求
     * @param method  当前控制器方法
     * @return 不返回；CUSTOM 分组必须由业务实现覆盖
     */
    @Override
    public String getGroupName(HttpServletRequest request, Method method) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * 以当前认证用户 ID 作为用户维度限流分组。
     *
     * @param request 当前 HTTP 请求
     * @param method  当前控制器方法
     * @return 当前用户 ID 的字符串形式
     */
    @Override
    public String getUserId(HttpServletRequest request, Method method) {
        return String.valueOf(WebContextUtils.getUserId());
    }

    /**
     * 以可信代理头解析出的客户端 IP 作为 IP 维度限流分组。
     *
     * @param request 当前 HTTP 请求
     * @param method  当前控制器方法
     * @return 当前客户端 IP
     */
    @Override
    public String getIp(HttpServletRequest request, Method method) {
        return ClientIpUtils.getClientIp(request);
    }
}
