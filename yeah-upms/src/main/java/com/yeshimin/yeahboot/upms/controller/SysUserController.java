package com.yeshimin.yeahboot.upms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yeshimin.yeahboot.upms.common.utils.WebContextUtils;
import com.yeshimin.yeahboot.upms.controller.base.CrudController;
import com.yeshimin.yeahboot.upms.domain.base.IdsDto;
import com.yeshimin.yeahboot.upms.domain.base.R;
import com.yeshimin.yeahboot.upms.domain.dto.SysUserCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysUserUpdateDto;
import com.yeshimin.yeahboot.upms.domain.dto.UserOrgSetDto;
import com.yeshimin.yeahboot.upms.domain.dto.UserRoleSetDto;
import com.yeshimin.yeahboot.upms.domain.entity.SysOrgEntity;
import com.yeshimin.yeahboot.upms.domain.entity.SysRoleEntity;
import com.yeshimin.yeahboot.upms.domain.entity.SysUserEntity;
import com.yeshimin.yeahboot.upms.domain.vo.SysUserResTreeNodeVo;
import com.yeshimin.yeahboot.upms.domain.vo.SysUserVo;
import com.yeshimin.yeahboot.upms.mapper.SysUserMapper;
import com.yeshimin.yeahboot.upms.repository.SysUserRepo;
import com.yeshimin.yeahboot.upms.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统用户相关
 */
@Valid
@RestController
@RequestMapping("/sysUser")
public class SysUserController extends CrudController<SysUserMapper, SysUserEntity, SysUserRepo> {

    @Autowired
    private SysUserService sysUserService;

    public SysUserController(SysUserRepo sysUserRepo) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(sysUserRepo);
    }

    // ================================================================================

    /**
     * 创建
     */
    @PostMapping("/create")
    public R<SysUserEntity> create(@RequestBody SysUserCreateDto dto) {
        return R.ok(sysUserService.create(dto));
    }

    /**
     * 查询
     */
    @GetMapping("/query")
    public R<IPage<SysUserVo>> query(Page<SysUserEntity> page) {
        return R.ok(sysUserService.query(page));
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public R<SysUserEntity> update(@RequestBody SysUserUpdateDto dto) {
        return R.ok(sysUserService.update(dto));
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody IdsDto dto) {
        Long userId = WebContextUtils.getUserId();
        sysUserService.delete(userId, dto.getIds());
        return R.ok();
    }

    // ================================================================================

    /**
     * 查询用户角色
     */
    @GetMapping("/queryUserRoles")
    public R<List<SysRoleEntity>> queryUserRoles(@RequestParam Long userId) {
        return R.ok(sysUserService.queryUserRoles(userId));
    }

    /**
     * 用户挂载角色（全部量操作）
     */
    @PostMapping("/setUserRoles")
    public R<Boolean> setUserRoles(@RequestBody UserRoleSetDto dto) {
        return R.ok(sysUserService.setUserRoles(dto));
    }

    /**
     * 查询用户资源
     */
    @GetMapping("/queryUserResources")
    public R<List<SysUserResTreeNodeVo>> queryUserResources(@RequestParam Long userId) {
        return R.ok(sysUserService.queryUserResources(userId));
    }

    // ================================================================================

    /**
     * 查询用户组织
     */
    @GetMapping("/queryUserOrgs")
    public R<List<SysOrgEntity>> queryUserOrgs(@RequestParam Long userId) {
        return R.ok(sysUserService.queryUserOrgs(userId));
    }

    /**
     * 用户挂载组织（全部量操作）
     */
    @PostMapping("/setUserOrgs")
    public R<Boolean> setUserOrgs(@RequestBody UserOrgSetDto dto) {
        return R.ok(sysUserService.setUserOrgs(dto));
    }
}
