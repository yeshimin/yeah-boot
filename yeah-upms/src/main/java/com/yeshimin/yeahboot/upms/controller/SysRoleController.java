package com.yeshimin.yeahboot.upms.controller;

import com.yeshimin.yeahboot.upms.controller.base.CrudController;
import com.yeshimin.yeahboot.upms.domain.base.IdsDto;
import com.yeshimin.yeahboot.upms.domain.base.R;
import com.yeshimin.yeahboot.upms.domain.dto.SysRoleCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysRoleResSetDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysRoleUpdateDto;
import com.yeshimin.yeahboot.upms.domain.entity.SysRoleEntity;
import com.yeshimin.yeahboot.upms.domain.vo.SysRoleResTreeNodeVo;
import com.yeshimin.yeahboot.upms.mapper.SysRoleMapper;
import com.yeshimin.yeahboot.upms.service.SysRoleService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统角色相关
 */
@Valid
@RestController
@RequestMapping("/sysRole")
public class SysRoleController extends CrudController<SysRoleMapper, SysRoleEntity, SysRoleService> {

    private final SysRoleService sysRoleService;

    public SysRoleController(SysRoleService sysRoleService) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(sysRoleService);
        this.sysRoleService = sysRoleService;
    }

    // ================================================================================

    /**
     * 创建
     */
    @PostMapping("/create")
    public R<SysRoleEntity> create(@RequestBody SysRoleCreateDto dto) {
        return R.ok(sysRoleService.create(dto));
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public R<SysRoleEntity> update(@RequestBody SysRoleUpdateDto dto) {
        return R.ok(sysRoleService.update(dto));
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody IdsDto dto) {
        sysRoleService.delete(dto.getIds());
        return R.ok();
    }

    // ================================================================================

    /**
     * 查询指定角色对应的资源数据
     */
    @GetMapping("/queryResourceTree")
    public R<List<SysRoleResTreeNodeVo>> queryResourceTree(@RequestParam Long roleId) {
        return R.ok(sysRoleService.queryResourceTree(roleId));
    }

    /**
     * 角色挂载资源（全量操作）
     */
    @PostMapping("/setResources")
    public R<Boolean> setResources(@RequestBody SysRoleResSetDto dto) {
        return R.ok(sysRoleService.setResources(dto));
    }
}
