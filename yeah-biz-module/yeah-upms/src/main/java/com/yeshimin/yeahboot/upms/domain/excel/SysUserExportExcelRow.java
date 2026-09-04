package com.yeshimin.yeahboot.upms.domain.excel;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户导出Excel行
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ExcelIgnoreUnannotated
public class SysUserExportExcelRow {

    @ExcelProperty(value = "ID", index = 0)
    @ColumnWidth(12)
    private String id;

    @ExcelProperty(value = "用户名", index = 1)
    @ColumnWidth(18)
    private String username;

    @ExcelProperty(value = "状态", index = 2)
    @ColumnWidth(12)
    private String status;

    @ExcelProperty(value = "昵称", index = 3)
    @ColumnWidth(18)
    private String nickname;

    @ExcelProperty(value = "手机号", index = 4)
    @ColumnWidth(18)
    private String mobile;

    @ExcelProperty(value = "邮箱", index = 5)
    @ColumnWidth(28)
    private String email;

    @ExcelProperty(value = "性别", index = 6)
    @ColumnWidth(12)
    private String gender;

    @ExcelProperty(value = "备注", index = 7)
    @ColumnWidth(36)
    private String remark;

    @ExcelProperty(value = "创建时间", index = 8)
    @ColumnWidth(22)
    private String createTime;
}
