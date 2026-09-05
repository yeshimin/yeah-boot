package com.yeshimin.yeahboot.common.service;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务
 */
@Service
@RequiredArgsConstructor
public class CacheService {

    private final StringRedisTemplate redisTemplate;

    /**
     * set
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * set with timeout seconds
     */
    public void set(String key, String value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * get string value
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * get by class
     */
    public <T> T get(String key, Class<T> clazz) {
        return JSON.parseObject(redisTemplate.opsForValue().get(key), clazz);
    }

    /**
     * get hash
     */
    public Map<Object, Object> getHash(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * set hash
     */
    public void setHash(String key, Map<String, String> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * set hash
     */
    public void setHash(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * delete keys
     */
    public Boolean delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return false;
        }
        Long deletedCount = redisTemplate.delete(Arrays.asList(keys));
        return deletedCount != null && deletedCount > 0;
    }

    /**
     * delete key fields
     */
    public Boolean deleteHashFields(String key, Object... fields) {
        if (fields == null || fields.length == 0) {
            return false;
        }
        return redisTemplate.opsForHash().delete(key, fields) > 0;
    }

    /**
     * expire
     */
    public Boolean expire(String key, long timeoutSeconds) {
        return redisTemplate.expire(key, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * expire
     */
    public Boolean expire(String key, Duration timeout) {
        return redisTemplate.expire(key, timeout);
    }

    /**
     * expireAt
     */
    public Boolean expireAt(String key, int expireAtS) {
        return redisTemplate.expireAt(key, new Date(expireAtS * 1000L));
    }

    /**
     * getExpire seconds
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * exists
     */
    public Boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * execute lua script
     */
    public Long executeLua(String luaScript, List<String> keys, List<String> args) {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);

        // 如果 keys 或 args 为 null，传空数组
        List<String> keyList = keys != null ? keys : Collections.emptyList();
        Object[] argArray = args != null ? args.toArray() : new Object[]{};

        return redisTemplate.execute(redisScript, keyList, argArray);
    }

    /**
     * get members of set
     */
    public Set<String> members(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * is member
     */
    public Boolean isMember(String key, String member) {
        return redisTemplate.opsForSet().isMember(key, member);
    }

    /**
     * add member to set
     */
    public Long addMember(String key, String member) {
        return redisTemplate.opsForSet().add(key, member);
    }

    /**
     * remove member from set
     */
    public Long removeMember(String key, String member) {
        return redisTemplate.opsForSet().remove(key, member);
    }

    /**
     * increase
     */
    public Long increase(String key) {
        return this.increase(key, 1);
    }

    /**
     * increase by delta
     */
    public Long increase(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * decrease
     */
    public Long decrease(String key) {
        return this.decrease(key, 1L);
    }

    /**
     * decrease by delta
     */
    public Long decrease(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }
}
