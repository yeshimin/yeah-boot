package com.yeshimin.yeahboot.app.demo;

import com.yeshimin.yeahboot.common.common.utils.WebContextUtils;
import com.yeshimin.yeahboot.flowcontrol.ratelimit.RateLimitService;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

@Service
public class DefaultRateLimitService implements RateLimitService {

    @Override
    public String getGroupName(HttpServletRequest request, Method method) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public String getUserId(HttpServletRequest request, Method method) {
        return String.valueOf(WebContextUtils.getUserId());
    }

    @Override
    public String getIp(HttpServletRequest request, Method method) {
        return request.getRemoteAddr();
    }
}
