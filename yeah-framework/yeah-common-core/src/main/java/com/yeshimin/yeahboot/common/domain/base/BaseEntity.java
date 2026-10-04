package com.yeshimin.yeahboot.common.domain.base;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.spring.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yeshimin.yeahboot.common.controller.validation.Create;
import com.yeshimin.yeahboot.common.controller.validation.Query;
import com.yeshimin.yeahboot.common.controller.validation.Update;
import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class BaseEntity<T extends Model<T>> extends Model<T> {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @NotNull(message = "主键ID不能为空", groups = {Update.class})
    @Null(message = "主键ID必须为空", groups = {Create.class})
    private Long id;

    /**
     * 删除时间
     */
    @JsonIgnore
    @TableField(fill = FieldFill.INSERT)
    @Null(message = "删除时间必须为空", groups = {Create.class, Query.class, Update.class})
    private LocalDateTime deleteTime;

    /**
     * 是否删除：1-是 0-否
     */
    @JsonIgnore
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    @Null(message = "删除标识必须为空", groups = {Create.class, Query.class, Update.class})
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    /**
     * 创建者
     */
    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    private String createBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 更新者
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
