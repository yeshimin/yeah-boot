package com.yeshimin.yeahboot.common.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientIpUtilsTest {

    @Test
    void matchesSingleIpAndIpv4Cidr() {
        assertTrue(ClientIpUtils.matchesIpRule("192.168.1.20", "192.168.1.20"));
        assertTrue(ClientIpUtils.matchesIpRule("192.168.1.20", "192.168.1.0/24"));
        assertFalse(ClientIpUtils.matchesIpRule("192.168.2.20", "192.168.1.0/24"));
    }

    @Test
    void matchesIpv6Cidr() {
        assertTrue(ClientIpUtils.matchesIpRule("2001:db8:1::10", "2001:db8::/32"));
        assertFalse(ClientIpUtils.matchesIpRule("2001:db9::10", "2001:db8::/32"));
    }

    @Test
    void rejectsInvalidIpRules() {
        assertFalse(ClientIpUtils.isValidIpOrCidr("192.168.1.0/33"));
        assertFalse(ClientIpUtils.isValidIpOrCidr("example.com"));
        assertFalse(ClientIpUtils.isValidIpOrCidr("192.168.1.1/24/1"));
    }
}
