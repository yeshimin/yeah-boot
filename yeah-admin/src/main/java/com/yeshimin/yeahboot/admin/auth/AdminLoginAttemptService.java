package com.yeshimin.yeahboot.admin.auth;

import com.yeshimin.yeahboot.common.common.enums.AuthSubjectEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.common.service.CacheService;
import com.yeshimin.yeahboot.data.service.DynamicConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 管理端登录失败次数和临时锁定服务
 */
@Service
@RequiredArgsConstructor
public class AdminLoginAttemptService {

    private static final String FAILURE_KEY_PREFIX = "admin:login:failure:";
    private static final String LOCK_KEY_PREFIX = "admin:login:lock:";

    /**
     * 原子增加失败次数，并在首次失败时设置统计窗口
     */
    private static final String INCREASE_FAILURE_LUA =
            "local count = redis.call('INCR', KEYS[1]); " +
                    "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; " +
                    "return count;";

    private final CacheService cacheService;
    private final DynamicConfigService dynamicConfigService;

    /**
     * 检查当前登录维度是否处于临时锁定状态
     */
    public void checkLocked(String username, String terminal) {
        String lockKey = this.getLockKey(username, terminal);
        Long seconds = cacheService.getExpire(lockKey);
        // Redis返回-2表示Key不存在或已经过期
        if (seconds == null || seconds == -2) {
            return;
        }

        if (seconds > 0) {
            throw new BaseException("登录失败次数过多，请" + seconds + "秒后重试");
        }
        throw new BaseException("登录失败次数过多，请稍后重试");
    }

    /**
     * 记录一次登录失败
     *
     * @return 剩余可尝试次数；返回0表示已经进入临时锁定
     */
    public long recordFailure(String username, String terminal) {
        long failureWindowSeconds = dynamicConfigService.getLong(SysConfigEnum.ADMIN_LOGIN_FAILURE_WINDOW_SECONDS);
        long maxFailureCount = dynamicConfigService.getLong(SysConfigEnum.ADMIN_LOGIN_MAX_FAILURE_COUNT);
        long lockSeconds = this.getLockSeconds();
         String failureKey = this.getFailureKey(username, terminal);
        Long count = cacheService.executeLua(INCREASE_FAILURE_LUA,
                Collections.singletonList(failureKey),
                Collections.singletonList(String.valueOf(failureWindowSeconds)));
        long failureCount = count == null ? 1 : count;
        if (failureCount < maxFailureCount) {
            return maxFailureCount - failureCount;
        }

        cacheService.set(this.getLockKey(username, terminal), "1", lockSeconds);
        cacheService.delete(failureKey);
        return 0;
    }

    /**
     * 获取配置的临时锁定时长
     */
    public long getLockSeconds() {
        return dynamicConfigService.getLong(SysConfigEnum.ADMIN_LOGIN_LOCK_SECONDS);
    }

    /**
     * 登录成功后清除失败次数和临时锁定状态
     */
    public void clear(String username, String terminal) {
        cacheService.delete(this.getFailureKey(username, terminal), this.getLockKey(username, terminal));
    }

    private String getFailureKey(String username, String terminal) {
        return this.getCacheKey(FAILURE_KEY_PREFIX, username, terminal);
    }

    private String getLockKey(String username, String terminal) {
        return this.getCacheKey(LOCK_KEY_PREFIX, username, terminal);
    }

    /**
     * 使用主体、终端和用户名组成可读的缓存Key
     */
    private String getCacheKey(String prefix, String username, String terminal) {
        return prefix + AuthSubjectEnum.ADMIN.getValue() + ":" + terminal + ":" + username;
    }
}
