package com.yeshimin.yeahboot.admin.auth;

import com.yeshimin.yeahboot.common.common.enums.AuthSubjectEnum;
import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.common.utils.ClientIpUtils;
import com.yeshimin.yeahboot.common.service.CacheService;
import com.yeshimin.yeahboot.data.service.DynamicConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端登录失败次数和临时锁定服务
 */
@Service
@RequiredArgsConstructor
public class AdminLoginAttemptService {

    /** 登录失败次数 Redis 键前缀。 */
    private static final String FAILURE_KEY_PREFIX = "admin:login:failure:";

    /** 登录临时锁定 Redis 键前缀。 */
    private static final String LOCK_KEY_PREFIX = "admin:login:lock:";

    /** IP 维度切换后的失败记录版本 Redis 键前缀。 */
    private static final String VERSION_KEY_PREFIX = "admin:login:version:";

    /**
     * 原子增加失败次数，并在首次失败时设置统计窗口
     */
    private static final String INCREASE_FAILURE_LUA =
            "local count = redis.call('INCR', KEYS[1]); " +
                    "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; " +
                    "return count;";

    /** 登录失败次数与锁定状态的 Redis 访问服务。 */
    private final CacheService cacheService;

    /** 提供登录安全策略动态参数的运行时配置服务。 */
    private final DynamicConfigService dynamicConfigService;

    /**
     * 检查白名单是否启用与匹配
     */
    public void checkLoginAllowed(String clientIp) {
        if (!Boolean.TRUE.equals(dynamicConfigService.getBoolean(SysConfigEnum.ADMIN_AUTH_LOGIN_IP_WHITELIST_ENABLED))) {
            return;
        }

        Set<String> whitelist = this.getIpWhitelist();
        if (whitelist.stream().noneMatch(rule -> ClientIpUtils.matchesIpRule(clientIp, rule))) {
            throw new BaseException("当前IP不允许登录管理后台");
        }
    }

    /**
     * 检查当前登录维度是否处于临时锁定状态
     */
    public void checkLocked(String username, String terminal, String clientIp) {
        String lockKey = this.getLockKey(username, terminal, clientIp);
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
    public long recordFailure(String username, String terminal, String clientIp) {
        long failureWindowSeconds = dynamicConfigService.getLong(SysConfigEnum.ADMIN_AUTH_LOGIN_FAILURE_WINDOW_SECONDS);
        long maxFailureCount = dynamicConfigService.getLong(SysConfigEnum.ADMIN_AUTH_LOGIN_FAILURE_COUNT);
        long lockSeconds = this.getLockSeconds();
        String failureKey = this.getFailureKey(username, terminal, clientIp);
        Long count = cacheService.executeLua(INCREASE_FAILURE_LUA,
                Collections.singletonList(failureKey),
                Collections.singletonList(String.valueOf(failureWindowSeconds)));
        long failureCount = count == null ? 1 : count;
        if (failureCount < maxFailureCount) {
            return maxFailureCount - failureCount;
        }

        cacheService.set(this.getLockKey(username, terminal, clientIp), "1", lockSeconds);
        cacheService.delete(failureKey);
        return 0;
    }

    /**
     * 获取配置的临时锁定时长
     */
    public long getLockSeconds() {
        return dynamicConfigService.getLong(SysConfigEnum.ADMIN_AUTH_LOGIN_LOCK_SECONDS);
    }

    /**
     * 登录成功后清除失败次数和临时锁定状态
     */
    public void clearCurrent(String username, String terminal, String clientIp) {
        cacheService.delete(this.getFailureKey(username, terminal, clientIp),
                this.getLockKey(username, terminal, clientIp));
    }

    /**
     * 使指定账号的全部IP维度失败记录失效，避免Redis按前缀扫描。
     */
    public void clearAll(String username, String terminal) {
        if (!this.isFailureIncludeIpEnabled()) {
            cacheService.delete(this.getFailureKey(username, terminal, null),
                    this.getLockKey(username, terminal, null));
            return;
        }

        String versionKey = this.getVersionKey(username, terminal);
        cacheService.increase(versionKey);
        long expireSeconds = Math.max(
                dynamicConfigService.getLong(SysConfigEnum.ADMIN_AUTH_LOGIN_FAILURE_WINDOW_SECONDS),
                this.getLockSeconds()) + 60;
        cacheService.expire(versionKey, expireSeconds);
    }

    /**
     * 构造当前登录维度的失败次数缓存键。
     */
    private String getFailureKey(String username, String terminal, String clientIp) {
        return this.getCacheKey(FAILURE_KEY_PREFIX, username, terminal, clientIp);
    }

    /**
     * 构造当前登录维度的临时锁定缓存键。
     */
    private String getLockKey(String username, String terminal, String clientIp) {
        return this.getCacheKey(LOCK_KEY_PREFIX, username, terminal, clientIp);
    }

    /**
     * 按当前 IP 统计策略构造缓存键；启用 IP 维度时加入版本号，便于批量失效全部来源记录。
     */
    private String getCacheKey(String prefix, String username, String terminal, String clientIp) {
        String key = prefix + AuthSubjectEnum.ADMIN.getValue() + ":" + terminal + ":" + username;
        if (!this.isFailureIncludeIpEnabled()) {
            return key;
        }
        return key + ":" + this.getVersion(username, terminal) + ":" + clientIp;
    }

    /**
     * 构造指定用户名和终端的 IP 维度版本键。
     */
    private String getVersionKey(String username, String terminal) {
        return VERSION_KEY_PREFIX + AuthSubjectEnum.ADMIN.getValue() + ":" + terminal + ":" + username;
    }

    /**
     * 读取当前 IP 维度缓存版本；首次使用默认版本 {@code 0}。
     */
    private String getVersion(String username, String terminal) {
        String version = cacheService.get(this.getVersionKey(username, terminal));
        return version == null ? "0" : version;
    }

    /**
     * 判断失败次数是否按客户端 IP 隔离。
     */
    private boolean isFailureIncludeIpEnabled() {
        return Boolean.TRUE.equals(dynamicConfigService.getBoolean(
                SysConfigEnum.ADMIN_AUTH_LOGIN_FAILURE_INCLUDE_IP_ENABLED));
    }

    /**
     * 将逗号分隔的后台登录 IP 或 CIDR 白名单转换为去重集合。
     */
    private Set<String> getIpWhitelist() {
        return Arrays.stream(dynamicConfigService.getString(SysConfigEnum.ADMIN_AUTH_LOGIN_IP_WHITELIST_VALUES)
                        .split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }
}
