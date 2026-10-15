package com.yeshimin.yeahboot.auth.service;

import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.base.Captcha;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
