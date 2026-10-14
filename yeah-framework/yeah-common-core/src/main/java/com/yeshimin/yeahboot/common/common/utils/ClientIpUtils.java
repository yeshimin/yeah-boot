package com.yeshimin.yeahboot.common.common.utils;

import cn.hutool.extra.servlet.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * 客户端 IP 工具。
 * <p>
 * 反向代理场景依赖网关或 Nginx 正确覆盖转发头；部署方必须确保应用端口不直接暴露公网。
 * </p>
 */
public final class ClientIpUtils {

    /** 工具类禁止实例化。 */
    private ClientIpUtils() {
    }

    /**
     * 从当前请求及可信代理转发头中解析客户端真实 IP。
     */
    public static String getClientIp(HttpServletRequest request) {
        return JakartaServletUtil.getClientIP(request);
    }

    /**
     * 从 Spring 请求上下文中取得当前客户端 IP；非 HTTP 线程返回 {@code null}。
     */
    public static String getCurrentClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return getClientIp(attributes.getRequest());
    }

    /**
     * 判断客户端 IP 是否匹配单个 IP 或 CIDR 网段规则。
     *
     * @param clientIp 客户端 IP
     * @param rule     单个 IP 或 CIDR 网段，例如 {@code 192.168.1.0/24}
     * @return 是否匹配规则
     */
    public static boolean matchesIpRule(String clientIp, String rule) {
        if (!isValidIpOrCidr(rule)) {
            return false;
        }

        String normalizedRule = rule.trim();
        int slashIndex = normalizedRule.indexOf('/');
        InetAddress clientAddress = parseLiteralIp(clientIp);
        InetAddress ruleAddress = parseLiteralIp(slashIndex < 0
                ? normalizedRule : normalizedRule.substring(0, slashIndex));
        if (clientAddress == null || ruleAddress == null
                || clientAddress.getAddress().length != ruleAddress.getAddress().length) {
            return false;
        }
        if (slashIndex < 0) {
            return Arrays.equals(clientAddress.getAddress(), ruleAddress.getAddress());
        }

        int prefixLength = Integer.parseInt(normalizedRule.substring(slashIndex + 1));
        return matchesCidr(clientAddress.getAddress(), ruleAddress.getAddress(), prefixLength);
    }

    /**
     * 校验单个 IP 或 CIDR 网段规则格式，不接受域名和带区域标识的 IPv6 地址。
     *
     * @param rule 待校验规则
     * @return 是否为有效的 IP 或 CIDR 规则
     */
    public static boolean isValidIpOrCidr(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return false;
        }

        String normalizedRule = rule.trim();
        int slashIndex = normalizedRule.indexOf('/');
        if (slashIndex < 0) {
            return parseLiteralIp(normalizedRule) != null;
        }
        if (slashIndex == 0 || slashIndex != normalizedRule.lastIndexOf('/')) {
            return false;
        }

        InetAddress address = parseLiteralIp(normalizedRule.substring(0, slashIndex));
        if (address == null) {
            return false;
        }
        try {
            int prefixLength = Integer.parseInt(normalizedRule.substring(slashIndex + 1));
            return prefixLength >= 0 && prefixLength <= address.getAddress().length * 8;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 仅解析字面量 IP，避免白名单配置触发 DNS 查询。
     */
    private static InetAddress parseLiteralIp(String value) {
        if (value == null) {
            return null;
        }
        boolean ipv4 = value.matches("[0-9.]+");
        boolean ipv6 = value.indexOf(':') >= 0 && value.matches("[0-9a-fA-F:.]+");
        if (!ipv4 && !ipv6) {
            return null;
        }
        try {
            InetAddress address = InetAddress.getByName(value);
            if ((ipv4 && address.getAddress().length != 4) || (ipv6 && address.getAddress().length != 16)) {
                return null;
            }
            return address;
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * 比较两个同地址族 IP 的 CIDR 网络前缀。
     */
    private static boolean matchesCidr(byte[] clientAddress, byte[] networkAddress, int prefixLength) {
        int fullBytes = prefixLength / 8;
        for (int index = 0; index < fullBytes; index++) {
            if (clientAddress[index] != networkAddress[index]) {
                return false;
            }
        }

        int remainingBits = prefixLength % 8;
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remainingBits);
        return (clientAddress[fullBytes] & mask) == (networkAddress[fullBytes] & mask);
    }
}
