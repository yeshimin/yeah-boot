package com.yeshimin.yeahboot.upms.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Min;

/**
 * 用户Excel导入导出配置
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "yeah-boot.sys-user-excel")
public class SysUserExcelProperties {

    /**
     * 导入文件最大大小，单位：MB
     */
    @Min(value = 1, message = "导入文件大小限制必须大于0")
    private Long maxImportFileSizeMb = 5L;

    /**
     * 单次最大导入行数
     */
    @Min(value = 1, message = "最大导入行数必须大于0")
    private Integer maxImportRows = 1000;

    /**
     * 单次最大导出行数
     */
    @Min(value = 1, message = "最大导出行数必须大于0")
    private Integer maxExportRows = 10000;

    /**
     * 最多展示的导入错误数量
     */
    @Min(value = 1, message = "最大错误展示数量必须大于0")
    private Integer maxErrorMessages = 20;
}
