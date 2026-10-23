package com.yeshimin.yeahboot.auth.service;

import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.base.Captcha;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 图形验证码服务测试。
 */
class CaptchaServiceTest {

    /**
     * JDK 15 及以上不再内置 Nashorn，随机验证码不能返回依赖脚本引擎的算术类型。
     */
    @Test
    void randomCaptchaNeverUsesArithmeticCaptcha() {
        CaptchaService captchaService = new CaptchaService(null);

        for (int index = 0; index < 100; index++) {
            Captcha captcha = captchaService.getRandomCaptcha();
            assertFalse(captcha instanceof ArithmeticCaptcha);
            assertNotNull(captcha.text());
        }
    }

    @Test
    void missingCaptchaUsesBusinessException() {
        CaptchaService captchaService = new CaptchaService(null);

        BaseException exception = assertThrows(BaseException.class,
                () -> captchaService.checkCaptcha("", "code"));

        assertEquals("验证码不能为空", exception.getMessage());
    }

    @Test
    void expiredCaptchaUsesBusinessException() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha_key:captcha-key")).thenReturn(null);
        CaptchaService captchaService = new CaptchaService(redisTemplate);

        BaseException exception = assertThrows(BaseException.class,
                () -> captchaService.checkCaptcha("captcha-key", "code"));

        assertEquals("验证码已失效", exception.getMessage());
    }

    @Test
    void invalidCaptchaUsesBusinessException() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("captcha_key:captcha-key")).thenReturn("expected-code");
        CaptchaService captchaService = new CaptchaService(redisTemplate);

        BaseException exception = assertThrows(BaseException.class,
                () -> captchaService.checkCaptcha("captcha-key", "wrong-code"));

        assertEquals("验证码错误", exception.getMessage());
    }
}
