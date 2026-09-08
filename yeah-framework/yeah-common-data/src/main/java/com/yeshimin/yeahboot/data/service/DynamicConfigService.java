package com.yeshimin.yeahboot.data.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.yeshimin.yeahboot.common.common.consts.CacheKeyConsts;
import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.common.service.CacheService;
import com.yeshimin.yeahboot.data.domain.entity.SysConfigEntity;
import com.yeshimin.yeahboot.data.repository.SysConfigRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动态系统参数服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicConfigService {

    private final CacheService cacheService;
    private final SysConfigRepo sysConfigRepo;

    /**
     * 应用启动完成后，以数据库中的启用参数刷新缓存
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        this.refreshCacheSafely();
    }

    /**
     * 获取字符串参数
     */
    public String getString(SysConfigEnum config) {
        String value = this.getCachedValues().get(config.getKey());
        return value == null ? config.getDefaultValue() : value;
    }

    /**
     * 获取整数参数
     */
    public Integer getInteger(SysConfigEnum config) {
        String value = this.getCachedValues().get(config.getKey());
        if (StrUtil.isBlank(value)) {
            return Integer.valueOf(config.getDefaultValue());
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("系统参数值不是有效整数，使用默认值，key: {}", config.getKey());
            return Integer.valueOf(config.getDefaultValue());
        }
    }

    /**
     * 获取长整数参数
     */
    public Long getLong(SysConfigEnum config) {
        String value = this.getCachedValues().get(config.getKey());
        if (StrUtil.isBlank(value)) {
            return Long.valueOf(config.getDefaultValue());
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            log.warn("系统参数值不是有效长整数，使用默认值，key: {}", config.getKey());
            return Long.valueOf(config.getDefaultValue());
        }
    }

    /**
     * 获取布尔参数
     */
    public Boolean getBoolean(SysConfigEnum config) {
        String value = this.getCachedValues().get(config.getKey());
        if (StrUtil.isBlank(value)) {
            return Boolean.valueOf(config.getDefaultValue());
        }
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        log.warn("系统参数值不是有效布尔值，使用默认值，key: {}", config.getKey());
        return Boolean.valueOf(config.getDefaultValue());
    }

    /**
     * 从数据库重新加载全部启用参数到缓存
     */
    public synchronized void refreshCache() {
        Map<String, String> values = new LinkedHashMap<>();
        for (SysConfigEntity entity : sysConfigRepo.findAllEnabled()) {
            values.put(entity.getConfigKey(), entity.getConfigValue());
        }

        cacheService.set(CacheKeyConsts.SYSTEM_CONFIG, JSON.toJSONString(values));
        log.info("系统参数缓存刷新完成，启用参数数量: {}，参数键: {}", values.size(), values.keySet());
        this.logKnownConfig(values);
    }

    /**
     * 尝试刷新缓存；用于启动和数据变更后的自动刷新，失败时不影响已经完成的数据库操作
     */
    public void refreshCacheSafely() {
        try {
            this.refreshCache();
        } catch (Exception e) {
            log.error("系统参数缓存刷新失败，请手动刷新", e);
        }
    }

    /**
     * 从Redis读取完整参数快照；缓存不存在或损坏时自动从数据库重建
     */
    private Map<String, String> getCachedValues() {
        String json = cacheService.get(CacheKeyConsts.SYSTEM_CONFIG);
        if (StrUtil.isBlank(json)) {
            return this.reloadAndGetCachedValues();
        }
        try {
            Map<String, String> values = this.parseCache(json);
            return values == null ? this.reloadAndGetCachedValues() : values;
        } catch (Exception e) {
            log.warn("系统参数缓存解析失败，将从数据库重新加载", e);
            return this.reloadAndGetCachedValues();
        }
    }

    /**
     * 串行重建缓存，避免缓存丢失时多个请求同时查询数据库
     */
    private synchronized Map<String, String> reloadAndGetCachedValues() {
        String json = cacheService.get(CacheKeyConsts.SYSTEM_CONFIG);
        if (StrUtil.isNotBlank(json)) {
            try {
                Map<String, String> values = this.parseCache(json);
                if (values != null) {
                    return values;
                }
            } catch (Exception ignored) {
                log.debug("系统参数缓存仍然不可用，继续从数据库重建");
            }
        }

        try {
            this.refreshCache();
        } catch (Exception e) {
            log.error("系统参数缓存重建失败，暂时使用代码默认值", e);
            return new LinkedHashMap<>();
        }
        String refreshedJson = cacheService.get(CacheKeyConsts.SYSTEM_CONFIG);
        if (StrUtil.isBlank(refreshedJson)) {
            return new LinkedHashMap<>();
        }
        Map<String, String> values = this.parseCache(refreshedJson);
        return values == null ? new LinkedHashMap<>() : values;
    }

    private Map<String, String> parseCache(String json) {
        return JSON.parseObject(json, new TypeReference<Map<String, String>>() {
        });
    }

    /**
     * 打印已接入的动态配置，短信供应商标识类配置固定脱敏
     */
    private void logKnownConfig(Map<String, String> values) {
        log.info("动态配置...captchaEnabled: {}, loginFailureWindowSeconds: {}, loginMaxFailureCount: {}, "
                        + "loginLockSeconds: {}, smsCodeLength: {}, smsCodeExpSeconds: {}, smsTemplateCode: {}, "
                        + "smsSignName: {}, excelMaxImportFileSizeMb: {}, excelMaxImportRows: {}, "
                        + "excelMaxExportRows: {}, excelMaxErrorMessages: {}",
                this.getLogValue(values, SysConfigEnum.CAPTCHA_ENABLED),
                this.getLogValue(values, SysConfigEnum.ADMIN_LOGIN_FAILURE_WINDOW_SECONDS),
                this.getLogValue(values, SysConfigEnum.ADMIN_LOGIN_MAX_FAILURE_COUNT),
                this.getLogValue(values, SysConfigEnum.ADMIN_LOGIN_LOCK_SECONDS),
                this.getLogValue(values, SysConfigEnum.SMS_CODE_LENGTH),
                this.getLogValue(values, SysConfigEnum.SMS_CODE_EXP_SECONDS),
                "******", "******",
                this.getLogValue(values, SysConfigEnum.SYS_USER_EXCEL_MAX_IMPORT_FILE_SIZE_MB),
                this.getLogValue(values, SysConfigEnum.SYS_USER_EXCEL_MAX_IMPORT_ROWS),
                this.getLogValue(values, SysConfigEnum.SYS_USER_EXCEL_MAX_EXPORT_ROWS),
                this.getLogValue(values, SysConfigEnum.SYS_USER_EXCEL_MAX_ERROR_MESSAGES));
    }

    private String getLogValue(Map<String, String> values, SysConfigEnum config) {
        return values.getOrDefault(config.getKey(), config.getDefaultValue());
    }
}
