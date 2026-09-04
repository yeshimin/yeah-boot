package com.yeshimin.yeahboot.upms.domain.vo;

import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户导入结果-VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserImportResultVo extends BaseDomain {

    /**
     * 导入用户数量
     */
    private Integer importedCount;

    /**
     * 自动生成初始密码的用户数量
     */
    private Integer generatedPasswordCount;
}
