package com.yeshimin.yeahboot.common.common.sensitive;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.filter.ValueFilter;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * 敏感数据脱敏工具
 */
public final class SensitiveDataUtils {

    /**
     * 完全隐藏时使用的固定掩码，避免通过掩码长度推测原始数据长度
     */
    private static final String FULL_MASK = "******";

    /**
     * Fastjson2日志字段过滤器
     * <p>
     * 仅处理标注了{@link SensitiveData}且包含{@link SensitiveScene#LOG}场景的字段。
     * </p>
     */
    private static final ValueFilter LOG_VALUE_FILTER = (object, name, value) -> {
        if (object == null || value == null) {
            return value;
        }

        // 根据当前序列化对象和属性名查找字段注解
        Field field = ReflectUtil.getField(object.getClass(), name);
        if (field == null) {
            return value;
        }
        SensitiveData sensitiveData = field.getAnnotation(SensitiveData.class);
        if (sensitiveData == null || !hasScene(sensitiveData, SensitiveScene.LOG)) {
            return value;
        }

        // 使用注解指定的类型生成日志脱敏值，不修改原始对象
        return mask(String.valueOf(value), sensitiveData.type());
    };

    /**
     * 工具类禁止实例化
     */
    private SensitiveDataUtils() {
    }

    /**
     * 将对象序列化为日志专用JSON，并处理标注了日志脱敏场景的字段
     *
     * @param value 待序列化对象
     * @return 日志JSON字符串
     */
    public static String toLogJson(Object value) {
        return JSON.toJSONString(value, LOG_VALUE_FILTER);
    }

    /**
     * 对Query/Form请求参数进行日志脱敏
     * <p>
     * 优先查找Controller已绑定参数对象中的字段注解；未找到注解时，
     * 再按常见敏感参数名称执行兜底脱敏。
     * </p>
     *
     * @param name  请求参数名
     * @param value 请求参数值
     * @param args  Controller方法参数
     * @return 日志中使用的参数值
     */
    public static String maskLogParameter(String name, String value, Object[] args) {
        if (StrUtil.isBlank(value)) {
            return value;
        }

        // 注解优先：根据已绑定的DTO或实体字段确定脱敏规则
        SensitiveData sensitiveData = findSensitiveData(name, args);
        if (sensitiveData != null) {
            return mask(value, sensitiveData.type());
        }

        // 字段名兜底：兼容简单@RequestParam参数或遗漏注解的情况
        return maskLogParameterByName(name, value);
    }

    /**
     * 从Controller方法参数对象中查找指定字段的日志脱敏注解
     *
     * @param name 请求参数名
     * @param args Controller方法参数
     * @return 匹配且包含LOG场景的注解，未找到时返回null
     */
    private static SensitiveData findSensitiveData(String name, Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        // Query/Form参数通常已经由Spring绑定为DTO或实体对象
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            Field field = ReflectUtil.getField(arg.getClass(), name);
            if (field == null) {
                continue;
            }
            SensitiveData sensitiveData = field.getAnnotation(SensitiveData.class);
            if (sensitiveData != null && hasScene(sensitiveData, SensitiveScene.LOG)) {
                return sensitiveData;
            }
        }
        return null;
    }

    /**
     * 根据常见敏感参数名称执行日志脱敏兜底
     *
     * @param name  请求参数名
     * @param value 请求参数值
     * @return 脱敏后的参数值；非敏感参数原样返回
     */
    private static String maskLogParameterByName(String name, String value) {
        // 密码和令牌不保留任何原始内容
        if ("password".equalsIgnoreCase(name)
                || "oldPassword".equalsIgnoreCase(name)
                || "newPassword".equalsIgnoreCase(name)
                || "token".equalsIgnoreCase(name)
                || "accessToken".equalsIgnoreCase(name)
                || "refreshToken".equalsIgnoreCase(name)) {
            return FULL_MASK;
        }

        // 结构化敏感数据按对应类型保留必要的可识别部分
        if ("mobile".equalsIgnoreCase(name) || "phone".equalsIgnoreCase(name)) {
            return mask(value, SensitiveType.MOBILE);
        }
        if ("email".equalsIgnoreCase(name)) {
            return mask(value, SensitiveType.EMAIL);
        }
        if ("realName".equalsIgnoreCase(name) || "fullName".equalsIgnoreCase(name)) {
            return mask(value, SensitiveType.NAME);
        }
        if ("idCard".equalsIgnoreCase(name)
                || "idCardNo".equalsIgnoreCase(name)
                || "certificateNo".equalsIgnoreCase(name)
                || "certNo".equalsIgnoreCase(name)) {
            return mask(value, SensitiveType.CERT);
        }
        if ("address".equalsIgnoreCase(name) || "detailAddress".equalsIgnoreCase(name)) {
            return mask(value, SensitiveType.ADDRESS);
        }
        return value;
    }

    /**
     * 判断脱敏注解是否包含指定生效场景
     *
     * @param sensitiveData 脱敏注解
     * @param scene         生效场景
     * @return 是否包含指定场景
     */
    public static boolean hasScene(SensitiveData sensitiveData, SensitiveScene scene) {
        return Arrays.asList(sensitiveData.scenes()).contains(scene);
    }

    /**
     * 按指定敏感类型对字符串进行脱敏
     *
     * @param value 原始值
     * @param type  敏感类型
     * @return 脱敏后的值
     */
    public static String mask(String value, SensitiveType type) {
        if (StrUtil.isBlank(value)) {
            return value;
        }

        // 手机号、邮箱等标准类型复用Hutool脱敏算法
        switch (type) {
            case NAME:
                return maskOrFull(value, DesensitizedUtil.chineseName(value));
            case MOBILE:
                return maskOrFull(value, DesensitizedUtil.mobilePhone(value));
            case EMAIL:
                return maskOrFull(value, DesensitizedUtil.email(value));
            case ADDRESS:
                return maskOrFull(value, DesensitizedUtil.address(value, Math.min(8, value.length())));
            case CERT:
                return maskOrFull(value, DesensitizedUtil.idCardNum(value, 6, 4));
            case FULL:
            default:
                return FULL_MASK;
        }
    }

    /**
     * 校验第三方脱敏结果，格式异常或未发生变化时回退为完全隐藏
     *
     * @param original 原始值
     * @param masked   第三方工具生成的脱敏值
     * @return 可安全输出的脱敏值
     */
    private static String maskOrFull(String original, String masked) {
        return StrUtil.isBlank(masked) || original.equals(masked) ? FULL_MASK : masked;
    }
}
