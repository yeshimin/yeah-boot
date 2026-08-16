package com.yeshimin.yeahboot.common.domain.base;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IdNameStatusVo extends IdNameVo {

    /**
     * 状态
     */
    private String status;

    public IdNameStatusVo(Long id, String name, String status) {
        super(id, name);
        this.status = status;
    }
}
