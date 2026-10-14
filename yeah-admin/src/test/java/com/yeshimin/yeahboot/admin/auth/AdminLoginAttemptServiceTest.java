package com.yeshimin.yeahboot.admin.auth;

import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.service.CacheService;
import com.yeshimin.yeahboot.data.service.DynamicConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLoginAttemptServiceTest {

    @Mock
    private CacheService cacheService;

    @Mock
    private DynamicConfigService dynamicConfigService;

    private AdminLoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new AdminLoginAttemptService(cacheService, dynamicConfigService);
    }

    @Test
    void acceptsMatchingCidrWhitelistRule() {
        when(dynamicConfigService.getBoolean(SysConfigEnum.ADMIN_AUTH_LOGIN_IP_WHITELIST_ENABLED)).thenReturn(true);
        when(dynamicConfigService.getString(SysConfigEnum.ADMIN_AUTH_LOGIN_IP_WHITELIST_VALUES))
                .thenReturn("192.168.10.0/24,2001:db8::/32");

        assertDoesNotThrow(() -> loginAttemptService.checkLoginAllowed("192.168.10.25"));
        assertDoesNotThrow(() -> loginAttemptService.checkLoginAllowed("2001:db8:1::10"));
        assertThrows(BaseException.class, () -> loginAttemptService.checkLoginAllowed("192.168.11.25"));
    }

    @Test
    void locksAccountAfterConfiguredFailureCount() {
        this.stubFailureRecordConfig();
        when(dynamicConfigService.getBoolean(SysConfigEnum.ADMIN_AUTH_LOGIN_FAILURE_INCLUDE_IP_ENABLED)).thenReturn(true);
        when(cacheService.executeLua(anyString(), any(), any())).thenReturn(3L);

        long remaining = loginAttemptService.recordFailure("alice", "WEB", "192.168.10.25");

        assertEquals(0, remaining);
        verify(cacheService).set("admin:login:lock:admin:WEB:alice:0:192.168.10.25", "1", 300L);
        verify(cacheService).delete("admin:login:failure:admin:WEB:alice:0:192.168.10.25");
    }

    @Test
    void clearsAllIpScopedRecordsByIncreasingVersion() {
        this.stubFailureClearConfig();
        when(dynamicConfigService.getBoolean(SysConfigEnum.ADMIN_AUTH_LOGIN_FAILURE_INCLUDE_IP_ENABLED)).thenReturn(true);

        loginAttemptService.clearAll("alice", "WEB");

        verify(cacheService).increase("admin:login:version:admin:WEB:alice");
        verify(cacheService).expire("admin:login:version:admin:WEB:alice", 660L);
    }

    private void stubFailureRecordConfig() {
        when(dynamicConfigService.getLong(SysConfigEnum.ADMIN_AUTH_LOGIN_FAILURE_WINDOW_SECONDS)).thenReturn(600L);
        when(dynamicConfigService.getLong(SysConfigEnum.ADMIN_AUTH_LOGIN_FAILURE_COUNT)).thenReturn(3L);
        when(dynamicConfigService.getLong(SysConfigEnum.ADMIN_AUTH_LOGIN_LOCK_SECONDS)).thenReturn(300L);
    }

    private void stubFailureClearConfig() {
        when(dynamicConfigService.getLong(SysConfigEnum.ADMIN_AUTH_LOGIN_FAILURE_WINDOW_SECONDS)).thenReturn(600L);
        when(dynamicConfigService.getLong(SysConfigEnum.ADMIN_AUTH_LOGIN_LOCK_SECONDS)).thenReturn(300L);
    }
}
