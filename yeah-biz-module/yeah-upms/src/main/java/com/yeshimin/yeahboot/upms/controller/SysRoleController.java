package com.yeshimin.yeahboot.upms.controller;

import com.yeshimin.yeahboot.common.common.enums.SysLogCategoryEnum;
import com.yeshimin.yeahboot.common.common.log.SysLog;
import com.yeshimin.yeahboot.common.controller.base.CrudController;
import com.yeshimin.yeahboot.common.domain.base.IdsDto;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.upms.domain.dto.SysRoleCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysRoleResSetDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysRoleUpdateDto;
import com.yeshimin.yeahboot.data.domain.entity.SysRoleEntity;
import com.yeshimin.yeahboot.upms.domain.vo.SysRoleResTreeNodeVo;
import com.yeshimin.yeahboot.upms.domain.vo.SysRoleVo;
import com.yeshimin.yeahboot.data.mapper.SysRoleMapper;
import com.yeshimin.yeahboot.data.repository.SysRoleRepo;
import com.yeshimin.yeahboot.upms.service.SysRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统角色相关
 */
@RestController
@RequestMapping("/admin/sysRole")
public class SysRoleController extends CrudController<SysRoleMapper, SysRoleEntity, SysRoleRepo> {

    private final SysRoleService sysRoleService;

    public SysRoleController(SysRoleRepo sysRoleRepo, SysRoleService sysRoleService) {
        super(sysRoleRepo);
        this.sysRoleService = sysRoleService;
        setModule("api:admin:sysRole")
                .disableCreate()
                .disableDetail()
                .disableUpdate()
                .disableDelete();
    }

    // ================================================================================

    /**
     * 创建
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':create')")
    @SysLog(value = "创建角色", category = SysLogCategoryEnum.DATA)
    @PostMapping("/create")
    public R<SysRoleEntity> create(@Valid @RequestBody SysRoleCreateDto dto) {
        return R.ok(sysRoleService.create(dto));
    }

    /**
     * 详情
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':detail')")
    @GetMapping("/detail")
    public R<SysRoleVo> detail(@RequestParam Long id) {
        return R.ok(sysRoleService.detail(id));
    }

    /**
     * 更新
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':update')")
    @SysLog(value = "更新角色", category = SysLogCategoryEnum.DATA)
    @PostMapping("/update")
    public R<SysRoleEntity> update(@Valid @RequestBody SysRoleUpdateDto dto) {
        return R.ok(sysRoleService.update(dto));
    }

    /**
     * 删除
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':delete')")
    @SysLog(value = "删除角色", category = SysLogCategoryEnum.DATA)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsDto dto) {
        sysRoleService.delete(dto.getIds());
        return R.ok();
    }

    // ================================================================================

    /**
     * 查询指定角色对应的资源数据
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':queryResourceTree')")
    @GetMapping("/queryResourceTree")
    public R<List<SysRoleResTreeNodeVo>> queryResourceTree(@RequestParam Long roleId) {
        return R.ok(sysRoleService.queryResourceTree(roleId));
    }

    /**
     * 角色挂载资源（全量操作）
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':setResources')")
    @SysLog(value = "设置角色资源", category = SysLogCategoryEnum.DATA)
    @PostMapping("/setResources")
    public R<Void> setResources(@Valid @RequestBody SysRoleResSetDto dto) {
        sysRoleService.setResources(dto);
        return R.ok();
    }
}
