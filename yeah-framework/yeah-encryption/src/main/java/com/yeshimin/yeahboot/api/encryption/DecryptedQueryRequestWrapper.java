package com.yeshimin.yeahboot.api.encryption;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将单个加密查询参数替换为解密后的参数映射。
 */
public class DecryptedQueryRequestWrapper extends HttpServletRequestWrapper {

    /** 由解密 JSON 转换得到的 Servlet 多值参数映射。 */
    private final Map<String, String[]> parameters;

    /**
     * 将已认证的查询参数 JSON 转换为 Servlet 参数映射，供既有 @RequestParam 绑定继续使用。
     */
    public DecryptedQueryRequestWrapper(HttpServletRequest request, byte[] plaintext, ObjectMapper objectMapper) {
        super(request);
        // 仅在构造时解析一次明文，后续所有参数读取均使用该不可变语义的映射。
        this.parameters = this.toParameters(plaintext, objectMapper);
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameters.get(name);
        return values == null || values.length == 0 ? null : values[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = parameters.get(name);
        return values == null ? null : Arrays.copyOf(values, values.length);
    }

    @Override
    public String getQueryString() {
        return null;
    }

    /**
     * 解析解密后的 JSON 对象，并转换为 Servlet 兼容的多值参数映射。
     */
    private Map<String, String[]> toParameters(byte[] plaintext, ObjectMapper objectMapper) {
        try {
            Map<String, Object> source = objectMapper.readValue(plaintext, new TypeReference<Map<String, Object>>() {
            });
            Map<String, String[]> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                result.put(entry.getKey(), this.toValues(entry.getValue()));
            }
            return result;
        } catch (Exception e) {
            throw new ApiEncryptionException("加密报文无效");
        }
    }

    /**
     * 仅接受标量或一维数组参数，拒绝嵌套对象以避免查询参数语义歧义。
     */
    private String[] toValues(Object value) {
        if (value == null) {
            return new String[]{null};
        }
        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            List<String> values = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item instanceof Map<?, ?> || item instanceof List<?>) {
                    throw new ApiEncryptionException("加密查询参数格式无效");
                }
                values.add(String.valueOf(item));
            }
            return values.toArray(new String[0]);
        }
        if (value instanceof Map<?, ?>) {
            throw new ApiEncryptionException("加密查询参数格式无效");
        }
        return new String[]{String.valueOf(value)};
    }
}
