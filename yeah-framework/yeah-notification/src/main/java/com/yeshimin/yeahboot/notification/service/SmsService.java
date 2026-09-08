package com.yeshimin.yeahboot.notification.service;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.QuerySendDetailsRequest;
import com.aliyun.dysmsapi20170525.models.QuerySendDetailsResponse;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.common.common.properties.NotificationAliyunSmsProperties;
import com.yeshimin.yeahboot.data.service.DynamicConfigService;
import com.yeshimin.yeahboot.notification.repository.NotifSmsApiLogRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * 短信服务
 * https://help.aliyun.com/zh/sms/getting-started/use-sms-api?spm=a2c4g.11186623.help-menu-44282.d_1_2.180e3d13hr5Rn6#9661beebe3r58
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsService {

    private final NotificationAliyunSmsProperties properties;
    private final DynamicConfigService dynamicConfigService;

    private final NotifSmsApiLogRepo notifSmsApiLogRepo;

    private Client client;

    @PostConstruct
    public void init() throws Exception {
        this.client = this.createClient();
    }

    public SendSmsResponse sendSms(String smsCode, String... numbers) {
        Map<String, String> templateParam = new HashMap<>();
        templateParam.put("code", smsCode);
        String templateCode = dynamicConfigService.getString(SysConfigEnum.SMS_TEMPLATE_CODE);
        return this.sendSms(templateCode, templateParam, numbers);
    }

    public SendSmsResponse sendSms(String templateCode, Map<String, String> templateParam, String... numbers) {
        // 构造请求对象，请填入请求参数值
        SendSmsRequest request = new SendSmsRequest()
                .setPhoneNumbers(String.join(",", numbers))
                .setSignName(dynamicConfigService.getString(SysConfigEnum.SMS_SIGN_NAME))
                .setTemplateCode(templateCode)
                .setTemplateParam(JSON.toJSONString(templateParam));

        // 获取响应对象
        try {
            SendSmsResponse response = client.sendSms(request);
            notifSmsApiLogRepo.createOne(JSON.toJSONString(request), JSON.toJSONString(response));
            return response;
        } catch (Exception e) {
            log.error("短信发送失败", e);
        }
        notifSmsApiLogRepo.createOne(JSON.toJSONString(request), null);
        return null;
    }

    /**
     * 查询发送结果
     */
    public QuerySendDetailsResponse querySendDetails(String bizId, String number, String sendDate,
                                                     Long currentPage, Long pageSize) {
        // 构造请求对象，请填入请求参数值
        QuerySendDetailsRequest request = new QuerySendDetailsRequest();
        if (StrUtil.isNotBlank(bizId)) {
            request.setBizId(bizId);
        }
        request.setPhoneNumber(number);
        request.setSendDate(sendDate);
        request.setCurrentPage(currentPage);
        request.setPageSize(pageSize);

        try {
            QuerySendDetailsResponse response = client.querySendDetails(request);
            notifSmsApiLogRepo.createOne(JSON.toJSONString(request), JSON.toJSONString(response));
            return response;
        } catch (Exception e) {
            log.error("短信发送结果查询失败", e);
        }
        notifSmsApiLogRepo.createOne(JSON.toJSONString(request), null);
        return null;
    }

    // ================================================================================

    private Client createClient() throws Exception {
        Config config = new Config()
                // 配置 AccessKey ID
                .setAccessKeyId(properties.getAccessKeyId())
                // 配置 AccessKey Secret
                .setAccessKeySecret(properties.getAccessKeySecret());

        // 配置 Endpoint
        config.endpoint = "dysmsapi.aliyuncs.com";

        return new Client(config);
    }
}
