package com.yeshimin.yeahboot.common.common.sensitive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SensitiveDataUtilsTest {

    @Test
    void masksSensitiveParameterNamesForLogs() {
        assertEquals("******", SensitiveDataUtils.maskLogParameter("password", "secret", new Object[0]));
        assertFalse(SensitiveDataUtils.maskLogParameter("token", "1234567890abcdefgh", new Object[0])
                .contains("1234567890abcdefgh"));
    }

    @Test
    void masksAnnotatedFieldsWhenSerializingLogs() {
        String json = SensitiveDataUtils.toLogJson(new SensitivePayload());

        assertFalse(json.contains("13800138000"));
        assertFalse(json.contains("secret"));
    }

    private static final class SensitivePayload {

        @SensitiveData(type = SensitiveType.MOBILE, scenes = SensitiveScene.LOG)
        private final String mobile = "13800138000";

        @SensitiveData(scenes = SensitiveScene.LOG)
        private final String password = "secret";
    }
}
