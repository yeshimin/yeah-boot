package com.yeshimin.yeahboot.api.encryption;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 Spring Bean 中解析部署配置指定的加密套件。
 */
@Component
public class ApiEncryptionSuiteRegistry {

    /** 按唯一套件标识索引的 Spring 密码套件 Bean。 */
    private final Map<String, ApiEncryptionSuite> suites = new HashMap<>();

    /**
     * 收集并校验应用上下文中的密码套件实现。
     *
     * @param suiteList Spring 注入的密码套件列表
     */
    public ApiEncryptionSuiteRegistry(List<ApiEncryptionSuite> suiteList) {
        for (ApiEncryptionSuite suite : suiteList) {
            ApiEncryptionSuite previous = suites.putIfAbsent(suite.getId(), suite);
            if (previous != null) {
                throw new IllegalStateException("duplicate API encryption suite: " + suite.getId());
            }
        }
    }

    /**
     * 按部署配置取得密码套件。
     *
     * @param suiteId 密码套件唯一标识
     * @return 已注册的密码套件
     */
    public ApiEncryptionSuite get(String suiteId) {
        ApiEncryptionSuite suite = suites.get(suiteId);
        if (suite == null) {
            throw new IllegalStateException("api.encryption.suite is not available: " + suiteId);
        }
        return suite;
    }
}
