package com.yeshimin.yeahboot.basic.domain.dto;

import com.yeshimin.yeahboot.common.domain.base.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileDeleteDto extends BaseDomain {

    /**
     * 文件Key集合
     */
    private Set<String> fileKeys;

    /**
     * 主键ID集合
     */
    private Set<Long> ids;
}
