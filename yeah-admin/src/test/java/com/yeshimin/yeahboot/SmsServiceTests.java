package com.yeshimin.yeahboot;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson2.JSON;
import com.aliyun.dysmsapi20170525.models.QuerySendDetailsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.yeshimin.yeahboot.admin.YeahAdminApplication;
import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.data.service.DynamicConfigService;
import com.yeshimin.yeahboot.notification.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

@Slf4j
@SpringBootTest(classes = YeahAdminApplication.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
public class SmsServiceTests {

    private final SmsService smsService;
    private final DynamicConfigService dynamicConfigService;

    @Test
    public void sendSmsTest() {
        // 生成短信验证码
        Integer smsCodeLength = dynamicConfigService.getInteger(SysConfigEnum.SMS_CODE_LENGTH);
        String smsCode = RandomUtil.randomNumbers(smsCodeLength);
        SendSmsResponse response = smsService.sendSms(smsCode, "152****3680");
        log.info("Response: {}", JSON.toJSONString(response));
        assert response != null;
    }

    @Test
    public void querySendTest() {
        String bizId = "86962********67440^0";
        String number = "152****3680";
        String sendDate = "20251014";
        Long currentPage = 1L;
        Long pageSize = 10L;
        QuerySendDetailsResponse response = smsService.querySendDetails(bizId, number, sendDate, currentPage, pageSize);
        log.info("Response: {}", JSON.toJSONString(response));
        assert response != null;
    }
}
