package com.yeshimin.yeahboot.auth.service;

import cn.hutool.core.util.StrUtil;
import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import com.yeshimin.yeahboot.auth.domain.vo.CaptchaVo;
import com.yeshimin.yeahboot.common.common.consts.CommonConsts;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 图形验证码相关服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 获取PNG验证码
     */
    @SneakyThrows
    private Captcha getPngCaptcha() {
        Captcha captcha = new SpecCaptcha(130, 48);
        captcha.setCharType(this.randomCharType());
        captcha.setFont(this.randomFont());
        return captcha;
    }

    /**
     * 获取GIF验证码
     */
    @SneakyThrows
    private Captcha getGifCaptcha() {
        Captcha captcha = new GifCaptcha(130, 48);
        captcha.setCharType(this.randomCharType());
        captcha.setFont(this.randomFont());
        return captcha;
    }

    /**
     * 获取算术验证码
     */
    @SneakyThrows
    private Captcha getArithmeticCaptcha() {
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 48);
        captcha.setFont(this.randomFont());
        return captcha;
    }

    /**
     * 随机验证码
     */
    public Captcha getRandomCaptcha() {
        int random = this.random(1, 3);
        log.debug("随机验证码类型: {}", random);

        switch (random) {
            case 1:
                return this.getPngCaptcha();
            case 2:
                return this.getGifCaptcha();
            case 3:
                return this.getArithmeticCaptcha();
        }
        return this.getPngCaptcha();
    }

    /**
     * 生成验证码
     */
    public CaptchaVo generateCaptcha() {
        Captcha captcha = this.getRandomCaptcha();
        String verCode = captcha.text().toLowerCase();
        String key = UUID.randomUUID().toString();
        String cacheKey = String.format(CommonConsts.CAPTCHA_KEY, key);
        redisTemplate.opsForValue().set(cacheKey, verCode, 30, TimeUnit.SECONDS);

        CaptchaVo vo = new CaptchaVo();
        vo.setKey(key);
        vo.setImage(captcha.toBase64());
        return vo;
    }

    /**
     * 检查验证码
     */
    public void checkCaptcha(String key, String verCode) {
        if (StrUtil.isBlank(key) || StrUtil.isBlank(verCode)) {
            log.error("验证码key或验证码不能为空");
            throw new RuntimeException("验证码key或验证码不能为空");
        }

        String cacheKey = String.format(CommonConsts.CAPTCHA_KEY, key);
        String cacheVerCode = redisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isBlank(cacheVerCode)) {
            log.error("验证码已失效");
            throw new RuntimeException("验证码已失效");
        }
        if (!Objects.equals(cacheVerCode, verCode)) {
            log.error("验证码错误");
            throw new RuntimeException("验证码错误");
        }

        // 删除验证码
        redisTemplate.delete(cacheKey);
    }

    // ================================================================================

    private int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private int randomCharType() {
        return random(Captcha.TYPE_DEFAULT, Captcha.TYPE_NUM_AND_UPPER);
    }

    private int randomFont() {
        return random(Captcha.FONT_1, Captcha.FONT_10);
    }
}
