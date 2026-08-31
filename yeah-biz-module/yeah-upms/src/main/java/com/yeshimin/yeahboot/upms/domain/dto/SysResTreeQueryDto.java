package com.yeshimin.yeahboot.upms.domain.dto;

import com.yeshimin.yeahboot.common.common.config.mybatis.QueryField;
import com.yeshimin.yeahboot.common.domain.base.BaseQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysResTreeQueryDto extends BaseQueryDto {

    /**
     * 名称
     */
    @QueryField(QueryField.Type.LIKE)
    private String name;

    /**
     * 状态：1-启用 2-禁用
     */
    @QueryField(QueryField.Type.EQ)
    private String status;

    /**
     * 类型：1-菜单 2-页面 3-按钮 4-接口 5-分组
     */
    @QueryField(QueryField.Type.EQ)
    private Integer type;

    /**
     * 接口资源分组ID，仅接口资源使用
     */
    @QueryField(QueryField.Type.EQ)
    private Long groupId;

    public boolean isQuery() {
        return name != null && !name.isEmpty()
                || status != null && !status.isEmpty()
                || type != null
                || groupId != null;
    }
}
