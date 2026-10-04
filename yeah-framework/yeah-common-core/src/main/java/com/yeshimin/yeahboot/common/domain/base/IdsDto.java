package com.yeshimin.yeahboot.common.domain.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class IdsDto extends BaseDomain {

    @NotNull(message = "ID集合不能为空")
    @NotEmpty(message = "ID集合不能为空")
    private List<Long> ids;
}
