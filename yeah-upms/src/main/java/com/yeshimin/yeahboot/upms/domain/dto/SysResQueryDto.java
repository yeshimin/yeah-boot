package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.upms.common.config.mybatis.Query;
import com.yeshimin.yeahboot.upms.common.config.mybatis.QueryField;
import com.yeshimin.yeahboot.upms.domain.base.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Query(custom = true)
public class SysResQueryDto extends BaseQueryDto {

    /**
     * 父ID
     */
    @QueryField(QueryField.Type.EQ)
    private Long parentId;

    /**
     * 名称
     */
    @QueryField(QueryField.Type.LIKE)
    private String name;

    /**
     * 备注
     */
    @QueryField(QueryField.Type.LIKE)
    private String remark;
}
