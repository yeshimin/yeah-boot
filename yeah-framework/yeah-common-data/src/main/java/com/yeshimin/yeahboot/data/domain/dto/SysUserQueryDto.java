package com.yeshimin.yeahboot.data.domain.dto;

import com.yeshimin.yeahboot.common.common.sensitive.SensitiveData;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveScene;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveType;
import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserQueryDto extends BaseDomain {

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    @SensitiveData(type = SensitiveType.MOBILE, scenes = SensitiveScene.LOG)
    private String mobile;

    /**
     * 状态：1-启用 2-禁用
     */
    private String status;

    /**
     * 创建日期Begin
     */
    private LocalDate createDateBegin;

    /**
     * 创建日期End
     */
    private LocalDate createDateEnd;

    /**
     * 组织ID集合
     */
    private Set<Long> orgIds;
}
