package com.yeshimin.yeahboot.upms.controller;

import com.yeshimin.yeahboot.common.common.enums.SysLogCategoryEnum;
import com.yeshimin.yeahboot.common.common.log.SysLog;
import com.yeshimin.yeahboot.common.controller.base.CrudController;
import com.yeshimin.yeahboot.common.domain.base.IdsDto;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.data.domain.entity.SysConfigEntity;
import com.yeshimin.yeahboot.data.mapper.SysConfigMapper;
import com.yeshimin.yeahboot.data.repository.SysConfigRepo;
import com.yeshimin.yeahboot.data.service.DynamicConfigService;
import com.yeshimin.yeahboot.upms.domain.dto.SysConfigCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysConfigUpdateDto;
import com.yeshimin.yeahboot.upms.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 系统参数管理
 */
@RestController
@RequestMapping("/admin/sysConfig")
public class SysConfigController extends CrudController<SysConfigMapper, SysConfigEntity, SysConfigRepo> {

    @Autowired
    private SysConfigService sysConfigService;
    @Autowired
    private DynamicConfigService dynamicConfigService;

    public SysConfigController(SysConfigRepo sysConfigRepo) {
        super(sysConfigRepo);
        super.setModule("api:admin:sysConfig")
                .disableCreate()
                .disableUpdate()
                .disableDelete();
    }

    /**
     * 创建系统参数
     */
    @PreAuthorize("@pms.hasPermission('api:admin:sysConfig:create')")
    @SysLog(value = "创建系统参数", category = SysLogCategoryEnum.DATA)
    @PostMapping("/create")
    public R<SysConfigEntity> create(@Valid @RequestBody SysConfigCreateDto dto) {
        SysConfigEntity entity = sysConfigService.create(dto);
        dynamicConfigService.refreshCacheSafely();
        return R.ok(entity);
    }

    /**
     * 更新系统参数
     */
    @PreAuthorize("@pms.hasPermission('api:admin:sysConfig:update')")
    @SysLog(value = "更新系统参数", category = SysLogCategoryEnum.DATA)
    @PostMapping("/update")
    public R<SysConfigEntity> update(@Valid @RequestBody SysConfigUpdateDto dto) {
        SysConfigEntity entity = sysConfigService.update(dto);
        dynamicConfigService.refreshCacheSafely();
        return R.ok(entity);
    }

    /**
     * 删除系统参数
     */
    @PreAuthorize("@pms.hasPermission('api:admin:sysConfig:delete')")
    @SysLog(value = "删除系统参数", category = SysLogCategoryEnum.DATA)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsDto dto) {
        sysConfigService.delete(dto.getIds());
        dynamicConfigService.refreshCacheSafely();
        return R.ok();
    }

    /**
     * 从数据库重新加载系统参数缓存
     */
    @PreAuthorize("@pms.hasPermission('api:admin:sysConfig:refreshCache')")
    @SysLog(value = "刷新系统参数缓存", category = SysLogCategoryEnum.DATA)
    @PostMapping("/refreshCache")
    public R<Void> refreshCache() {
        dynamicConfigService.refreshCache();
        return R.ok();
    }
}
