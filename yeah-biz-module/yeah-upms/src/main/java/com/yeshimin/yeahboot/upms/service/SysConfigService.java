package com.yeshimin.yeahboot.upms.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.common.common.enums.SysConfigValueTypeEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.data.domain.entity.SysConfigEntity;
import com.yeshimin.yeahboot.data.repository.SysConfigRepo;
import com.yeshimin.yeahboot.upms.domain.dto.SysConfigCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysConfigUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysConfigService {

    private static final Set<SysConfigEnum> LONG_POSITIVE_CONFIGS = new HashSet<>(Arrays.asList(
            SysConfigEnum.ADMIN_LOGIN_FAILURE_WINDOW_SECONDS,
            SysConfigEnum.ADMIN_LOGIN_MAX_FAILURE_COUNT,
            SysConfigEnum.ADMIN_LOGIN_LOCK_SECONDS,
            SysConfigEnum.SYS_USER_EXCEL_MAX_IMPORT_FILE_SIZE_MB));

    private static final Set<SysConfigEnum> INTEGER_POSITIVE_CONFIGS = new HashSet<>(Arrays.asList(
            SysConfigEnum.SMS_CODE_EXP_SECONDS,
            SysConfigEnum.SYS_USER_EXCEL_MAX_IMPORT_ROWS,
            SysConfigEnum.SYS_USER_EXCEL_MAX_EXPORT_ROWS,
            SysConfigEnum.SYS_USER_EXCEL_MAX_ERROR_MESSAGES));

    private final SysConfigRepo sysConfigRepo;

    /**
     * 创建系统参数
     */
    @Transactional(rollbackFor = Exception.class)
    public SysConfigEntity create(SysConfigCreateDto dto) {
        if (sysConfigRepo.countByConfigKey(dto.getConfigKey()) > 0) {
            throw new BaseException("参数键已存在");
        }
        this.validateValue(dto.getConfigKey(), dto.getValueType(), dto.getConfigValue());

        SysConfigEntity entity = BeanUtil.copyProperties(dto, SysConfigEntity.class);
        entity.insert();
        return entity;
    }

    /**
     * 更新系统参数；参数键作为程序读取入口，创建后不允许修改
     */
    @Transactional(rollbackFor = Exception.class)
    public SysConfigEntity update(SysConfigUpdateDto dto) {
        SysConfigEntity entity = sysConfigRepo.getOneById(dto.getId());
        this.validateValue(entity.getConfigKey(), dto.getValueType(), dto.getConfigValue());

        entity.setGroupCode(dto.getGroupCode());
        entity.setConfigName(dto.getConfigName());
        entity.setConfigValue(dto.getConfigValue());
        entity.setValueType(dto.getValueType());
        entity.setStatus(dto.getStatus());
        entity.setSort(dto.getSort());
        entity.setRemark(dto.getRemark());
        entity.updateById();
        return entity;
    }

    /**
     * 删除系统参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        sysConfigRepo.removeBatchByIds(ids);
    }

    /**
     * 校验参数值类型，并对已接入程序的参数执行必要范围校验
     */
    private void validateValue(String configKey, Integer valueType, String configValue) {
        SysConfigValueTypeEnum type = SysConfigValueTypeEnum.of(valueType);
        if (type == null) {
            throw new BaseException("参数值类型不正确");
        }

        switch (type) {
            case STRING:
                break;
            case INTEGER:
                this.parseInteger(configValue);
                break;
            case LONG:
                this.parseLong(configValue);
                break;
            case BOOLEAN:
                if (!"true".equalsIgnoreCase(configValue) && !"false".equalsIgnoreCase(configValue)) {
                    throw new BaseException("布尔参数值只能填写true或false");
                }
                break;
            default:
                throw new BaseException("参数值类型不正确");
        }

        this.validateKnownConfig(configKey, type, configValue);
    }

    private void validateKnownConfig(String configKey, SysConfigValueTypeEnum type, String configValue) {
        SysConfigEnum config = SysConfigEnum.ofKey(configKey);
        if (config == null) {
            return;
        }

        this.requireType(type, config.getValueType());
        if (config == SysConfigEnum.SMS_CODE_LENGTH) {
            int value = this.parseInteger(configValue);
            if (value < 4 || value > 8) {
                throw new BaseException("短信验证码长度必须在4到8之间");
            }
            return;
        }
        if (LONG_POSITIVE_CONFIGS.contains(config)) {
            if (this.parseLong(configValue) < 1) {
                throw new BaseException("参数值必须大于0");
            }
            return;
        }
        if (INTEGER_POSITIVE_CONFIGS.contains(config)) {
            if (this.parseInteger(configValue) < 1) {
                throw new BaseException("参数值必须大于0");
            }
        }
    }

    private void requireType(SysConfigValueTypeEnum actual, SysConfigValueTypeEnum expected) {
        if (actual != expected) {
            throw new BaseException("该参数的值类型必须为" + expected.getDesc());
        }
    }

    private int parseInteger(String value) {
        if (StrUtil.isBlank(value)) {
            throw new BaseException("整数参数值不能为空");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BaseException("参数值不是有效整数");
        }
    }

    private long parseLong(String value) {
        if (StrUtil.isBlank(value)) {
            throw new BaseException("长整数参数值不能为空");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BaseException("参数值不是有效长整数");
        }
    }

}
