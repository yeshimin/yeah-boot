package com.yeshimin.yeahboot.basic.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StorageDeleteDto extends FileDeleteDto {

    /**
     * 是否强制删除正在使用的存储文件
     */
    private Boolean force;
}
