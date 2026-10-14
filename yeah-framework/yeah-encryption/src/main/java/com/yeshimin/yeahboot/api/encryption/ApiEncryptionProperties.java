package com.yeshimin.yeahboot.api.encryption;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 报文加密协议配置。
 *
 * <p>私钥必须由密钥管理服务或环境变量注入，不能存入系统参数或受版本控制的配置文件。</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "yeah-boot.encryption")
public class ApiEncryptionProperties {

    /** 协议模式，决定是否启用及是否强制要求加密。 */
    private ApiEncryptionMode mode = ApiEncryptionMode.OFF;

    /**
     * 加密套件标识，由服务端部署配置指定，客户端只能使用服务端公布的当前套件。
     */
    private String suite = "sm2-sm4-gcm-v1";

    /** 当前协议版本。 */
    private int version = 1;

    /** 当前服务端密钥标识，用于客户端密钥选择和后续轮换。 */
    private String keyId = "default";

    /** 服务端私钥，由部署环境注入，绝不能写入系统参数或版本库。 */
    @ToString.Exclude
    private String privateKey;

    /** 始终保持明文的接口规则，格式为“路径 [HTTP 方法]”。 */
    private List<String> bypassRoutes = new ArrayList<>();

    /** 请求时间戳允许的最大偏差，单位：秒。 */
    private long timestampWindowSeconds = 300;

    /** 防重放记录在缓存中的有效期，单位：秒。 */
    private long replayTtlSeconds = 300;

    /** 单个加密请求密文允许的最大字节数。 */
    private int maxPayloadBytes = 64 * 1024;

    /**
     * 在协议启用时校验密钥引用、版本、重放窗口和报文上限，避免以不完整配置启动。
     */
    @PostConstruct
    private void validate() {
        if (mode == ApiEncryptionMode.OFF) {
            return;
        }
        if (!StringUtils.hasText(keyId)) {
            throw new IllegalStateException("api.encryption.key-id must not be blank");
        }
        if (!StringUtils.hasText(suite)) {
            throw new IllegalStateException("api.encryption.suite must not be blank");
        }
        if (version != 1) {
            throw new IllegalStateException("only API encryption protocol version 1 is supported");
        }
        if (timestampWindowSeconds < 30 || replayTtlSeconds < timestampWindowSeconds) {
            throw new IllegalStateException("API encryption replay TTL must cover the timestamp window");
        }
        if (maxPayloadBytes < 1024 || maxPayloadBytes > 1024 * 1024) {
            throw new IllegalStateException("API encryption max payload bytes must be between 1024 and 1048576");
        }
    }

    /**
     * 判断当前部署是否启用报文加密协议。
     */
    public boolean isEnabled() {
        return mode != ApiEncryptionMode.OFF;
    }
}
