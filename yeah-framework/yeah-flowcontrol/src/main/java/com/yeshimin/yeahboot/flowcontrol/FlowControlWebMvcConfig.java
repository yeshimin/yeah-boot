package com.yeshimin.yeahboot.flowcontrol;

import com.yeshimin.yeahboot.flowcontrol.ratelimit.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册 MVC 限流拦截器。
 *
 * <p>限流需要先于报文加密策略执行，尽早拒绝高频请求，避免额外的解密和 JSON 解析开销。</p>
 */
@Configuration
public class FlowControlWebMvcConfig implements WebMvcConfigurer {

    /** 按接口注解执行限流的拦截器。 */
    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * 注入限流拦截器。
     *
     * @param rateLimitInterceptor 限流拦截器
     */
    public FlowControlWebMvcConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    /**
     * 以最低 MVC 拦截器顺序注册限流；报文加密策略使用顺序 {@code 10}。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns("/**").order(0);
    }
}
