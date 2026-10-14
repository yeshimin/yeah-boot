package com.yeshimin.yeahboot.api.encryption;

import com.yeshimin.yeahboot.common.domain.base.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供公开加密元数据；仅返回公钥，不返回任何私钥或密钥材料。
 */
@RestController
@RequestMapping(ApiEncryptionController.PUBLIC_KEY_PATH)
@RequiredArgsConstructor
@ApiEncryption(ApiEncryptionPolicy.BYPASS)
public class ApiEncryptionController {

    /** 客户端获取公钥和协议元数据的固定公开路径。 */
    public static final String PUBLIC_KEY_PATH = "/encryption/public-key";

    /** 组装当前服务端公钥及协议元数据的加密服务。 */
    private final ApiEncryptionService apiEncryptionService;

    /**
     * 获取当前部署的公开密钥和协议开关，供前端初始化请求加密能力。
     */
    @GetMapping
    public R<ApiEncryptionPublicKeyVo> publicKey() {
        return R.ok(apiEncryptionService.getPublicKey());
    }
}
