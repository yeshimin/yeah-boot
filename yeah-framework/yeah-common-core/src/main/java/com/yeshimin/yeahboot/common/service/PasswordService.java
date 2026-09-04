package com.yeshimin.yeahboot.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * 密码相关服务
 */
@Service
@RequiredArgsConstructor
public class PasswordService {

    private static final int DEFAULT_RANDOM_PASSWORD_LENGTH = 16;
    private static final char[] RANDOM_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordEncoder passwordEncoder;

    /**
     * 校验密码
     */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 加密密码
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 生成默认长度的安全随机密码
     */
    public String generateRandomPassword() {
        return this.generateRandomPassword(DEFAULT_RANDOM_PASSWORD_LENGTH);
    }

    /**
     * 生成指定长度的安全随机密码
     */
    public String generateRandomPassword(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("密码长度必须大于0");
        }
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(RANDOM_PASSWORD_CHARS[SECURE_RANDOM.nextInt(RANDOM_PASSWORD_CHARS.length)]);
        }
        return password.toString();
    }
}
