package com.yeshimin.yeahboot.admin.controller;

import com.yeshimin.yeahboot.common.common.enums.AuthSubjectEnum;
import com.yeshimin.yeahboot.common.controller.base.BaseController;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.ws.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * admin端-测试相关
 */
@RestController
@RequestMapping("/admin/test")
@RequiredArgsConstructor
public class AdminTestController extends BaseController {

    private final WebSocketService wsService;

    /**
     * 发送websocket消息测试
     */
    @PreAuthorize("@pms.hasPermission('api:admin:test:wsSend')")
    @PostMapping("/wsSend")
    public R<Void> wsSend(@RequestBody String message) {
        wsService.sendMessageToUser(message, AuthSubjectEnum.ADMIN.getValue(), String.valueOf(1L));
        return R.ok();
    }

    /**
     * websocket广播测试
     */
    @PreAuthorize("@pms.hasPermission('api:admin:test:wsBroadcast')")
    @PostMapping("/wsBroadcast")
    public R<Void> wsBroadcast(@RequestBody String message) {
        wsService.sendMessageBroadcast(message, AuthSubjectEnum.ADMIN.getValue());
        return R.ok();
    }
}
