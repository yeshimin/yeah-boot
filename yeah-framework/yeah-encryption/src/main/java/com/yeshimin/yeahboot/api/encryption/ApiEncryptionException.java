package com.yeshimin.yeahboot.api.encryption;

import com.yeshimin.yeahboot.common.common.exception.BaseException;

/**
 * 统一的加密协议异常，不向客户端暴露具体失败细节。
 */
public class ApiEncryptionException extends BaseException {

    public ApiEncryptionException(String message) {
        super(message);
    }
}
