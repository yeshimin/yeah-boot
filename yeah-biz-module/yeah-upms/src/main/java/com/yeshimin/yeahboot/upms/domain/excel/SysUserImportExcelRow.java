package com.yeshimin.yeahboot.upms.domain.excel;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveData;
import com.yeshimin.yeahboot.common.common.sensitive.SensitiveScene;
import lombok.Data;

/**
 * 用户导入Excel行
 */
@Data
@ExcelIgnoreUnannotated
public class SysUserImportExcelRow {

    @ExcelIgnore
    private Integer rowNumber;

    @ExcelProperty(value = "用户名*", index = 0)
    @ColumnWidth(32)
    private String username;

    @ExcelProperty(value = "初始密码", index = 1)
    @ColumnWidth(18)
    @SensitiveData(scenes = SensitiveScene.LOG)
    private String password;

    @ExcelProperty(value = "状态（1启用/2禁用）", index = 2)
    @ColumnWidth(12)
    private String status;

    @ExcelProperty(value = "昵称", index = 3)
    @ColumnWidth(32)
    private String nickname;

    @ExcelProperty(value = "手机号", index = 4)
    @ColumnWidth(18)
    private String mobile;

    @ExcelProperty(value = "邮箱", index = 5)
    @ColumnWidth(28)
    private String email;

    @ExcelProperty(value = "性别（0未知/1男/2女）", index = 6)
    @ColumnWidth(12)
    private String gender;

    @ExcelProperty(value = "备注", index = 7)
    @ColumnWidth(36)
    private String remark;
}
