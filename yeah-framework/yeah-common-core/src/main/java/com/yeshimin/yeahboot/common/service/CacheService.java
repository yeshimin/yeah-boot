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
     * 写入永不过期的字符串缓存。
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 写入指定秒数后过期的字符串缓存。
     */
    public void set(String key, String value, long timeout) {
        redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 仅在键不存在时写入值，并原子设置过期时间。
     */
    public Boolean setIfAbsent(String key, String value, Duration timeout) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout);
    }

    /**
     * 读取字符串缓存。
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 读取 JSON 缓存并反序列化为指定类型。
     */
    public <T> T get(String key, Class<T> clazz) {
        return JSON.parseObject(redisTemplate.opsForValue().get(key), clazz);
    }

    /**
     * 读取 Hash 全部字段。
     */
    public Map<Object, Object> getHash(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 批量写入 Hash 字段。
     */
    public void setHash(String key, Map<String, String> map) {
        redisTemplate.opsForHash().putAll(key, map);
    }

    /**
     * 写入单个 Hash 字段。
     */
    public void setHash(String key, String field, String value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    /**
     * 删除一个或多个缓存键。
     */
    public Boolean delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return false;
        }
        Long deletedCount = redisTemplate.delete(Arrays.asList(keys));
        return deletedCount != null && deletedCount > 0;
    }

    /**
     * 删除 Hash 中的指定字段。
     */
    public Boolean deleteHashFields(String key, Object... fields) {
        if (fields == null || fields.length == 0) {
            return false;
        }
        return redisTemplate.opsForHash().delete(key, fields) > 0;
    }

    /**
     * 设置缓存键的秒级过期时间。
     */
    public Boolean expire(String key, long timeoutSeconds) {
        return redisTemplate.expire(key, timeoutSeconds, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存键的 Duration 过期时间。
     */
    public Boolean expire(String key, Duration timeout) {
        return redisTemplate.expire(key, timeout);
    }

    /**
     * 设置缓存键在指定秒级时间戳到期。
     */
    public Boolean expireAt(String key, int expireAtS) {
        return redisTemplate.expireAt(key, new Date(expireAtS * 1000L));
    }

    /**
     * 获取缓存键剩余过期秒数。
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断缓存键是否存在。
     */
    public Boolean exists(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 执行返回 Long 的 Lua 脚本。
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
     * 获取 Set 全部成员。
     */
    public Set<String> members(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 判断值是否属于 Set。
     */
    public Boolean isMember(String key, String member) {
        return redisTemplate.opsForSet().isMember(key, member);
    }

    /**
     * 向 Set 添加成员。
     */
    public Long addMember(String key, String member) {
        return redisTemplate.opsForSet().add(key, member);
    }

    /**
     * 从 Set 移除成员。
     */
    public Long removeMember(String key, String member) {
        return redisTemplate.opsForSet().remove(key, member);
    }

    /**
     * 将数值缓存递增 1。
     */
    public Long increase(String key) {
        return this.increase(key, 1);
    }

    /**
     * 将数值缓存递增指定值。
     */
    public Long increase(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 将数值缓存递减 1。
     */
    public Long decrease(String key) {
        return this.decrease(key, 1L);
    }

    /**
     * 将数值缓存递减指定值。
     */
    public Long decrease(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }
}
