package com.yeshimin.yeahboot.common.domain.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class IdDto extends BaseDomain {

    @NotNull(message = "ID不能为空")
    private Long id;
}
