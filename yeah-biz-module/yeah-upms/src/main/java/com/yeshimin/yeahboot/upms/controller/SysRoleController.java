package com.yeshimin.yeahboot.upms.controller;

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
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private SysRoleService sysRoleService;

    public SysRoleController(SysRoleRepo sysRoleRepo) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(sysRoleRepo);
        setModule("admin:sysRole");
    }

    // ================================================================================

    /**
     * 创建
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':create')")
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
    @PostMapping("/update")
    public R<SysRoleEntity> update(@Valid @RequestBody SysRoleUpdateDto dto) {
        return R.ok(sysRoleService.update(dto));
    }

    /**
     * 删除
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':delete')")
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
    @PostMapping("/setResources")
    public R<Boolean> setResources(@Valid @RequestBody SysRoleResSetDto dto) {
        return R.ok(sysRoleService.setResources(dto));
    }
}
