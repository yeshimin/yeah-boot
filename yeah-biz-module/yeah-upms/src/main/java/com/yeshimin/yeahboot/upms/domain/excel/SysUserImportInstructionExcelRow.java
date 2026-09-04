package com.yeshimin.yeahboot.upms.domain.excel;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户导入填写说明Excel行
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ExcelIgnoreUnannotated
public class SysUserImportInstructionExcelRow {

    @ExcelProperty(value = "字段", index = 0)
    @ColumnWidth(18)
    private String field;

    @ExcelProperty(value = "填写说明", index = 1)
    @ColumnWidth(70)
    private String description;
}
