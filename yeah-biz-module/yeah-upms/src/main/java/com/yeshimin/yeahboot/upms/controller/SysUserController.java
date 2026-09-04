package com.yeshimin.yeahboot.upms.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yeshimin.yeahboot.common.common.enums.SysLogCategoryEnum;
import com.yeshimin.yeahboot.common.common.log.SysLog;
import com.yeshimin.yeahboot.common.common.utils.WebContextUtils;
import com.yeshimin.yeahboot.common.common.utils.YsmUtils;
import com.yeshimin.yeahboot.common.controller.base.CrudController;
import com.yeshimin.yeahboot.common.domain.base.IdsDto;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.data.domain.dto.SysUserQueryDto;
import com.yeshimin.yeahboot.data.domain.entity.SysOrgEntity;
import com.yeshimin.yeahboot.data.domain.entity.SysPostEntity;
import com.yeshimin.yeahboot.data.domain.entity.SysRoleEntity;
import com.yeshimin.yeahboot.data.domain.entity.SysUserEntity;
import com.yeshimin.yeahboot.data.mapper.SysUserMapper;
import com.yeshimin.yeahboot.data.repository.SysUserRepo;
import com.yeshimin.yeahboot.upms.domain.dto.*;
import com.yeshimin.yeahboot.upms.domain.vo.MineVo;
import com.yeshimin.yeahboot.upms.domain.vo.SysUserImportResultVo;
import com.yeshimin.yeahboot.upms.domain.vo.SysUserResTreeNodeVo;
import com.yeshimin.yeahboot.upms.domain.vo.SysUserVo;
import com.yeshimin.yeahboot.upms.service.SysUserExcelService;
import com.yeshimin.yeahboot.upms.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * 系统用户相关
 */
@RestController
@RequestMapping("/admin/sysUser")
public class SysUserController extends CrudController<SysUserMapper, SysUserEntity, SysUserRepo> {

    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private SysUserExcelService sysUserExcelService;

    public SysUserController(SysUserRepo sysUserRepo) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(sysUserRepo);
        setModule("admin:sysUser")
                .disableCreate()
                .disableQuery()
                .disableDetail()
                .disableUpdate()
                .disableDelete();
    }

    // ================================================================================

    /**
     * 创建
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':create')")
    @SysLog(value = "创建用户", category = SysLogCategoryEnum.DATA)
    @PostMapping("/create")
    public R<SysUserEntity> create(@Valid @RequestBody SysUserCreateDto dto) {
        return R.ok(sysUserService.create(dto));
    }

    /**
     * 查询
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':query')")
    @GetMapping("/query")
    public R<IPage<SysUserVo>> query(Page<SysUserEntity> page, SysUserQueryDto dto) {
        return R.ok(sysUserService.query(page, dto));
    }

    /**
     * 详情
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':detail')")
    @GetMapping("/detail")
    public R<SysUserVo> detail(@RequestParam Long id) {
        return R.ok(sysUserService.detail(id));
    }

    /**
     * 更新
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':update')")
    @SysLog(value = "更新用户", category = SysLogCategoryEnum.DATA)
    @PostMapping("/update")
    public R<SysUserEntity> update(@Valid @RequestBody SysUserUpdateDto dto) {
        Long userId = WebContextUtils.getUserId();
        return R.ok(sysUserService.update(userId, dto));
    }

    /**
     * 重置用户密码
     */
    @PreAuthorize("@pms.hasPermission('api:admin:sysUser:resetPassword')")
    @SysLog(value = "重置用户密码", category = SysLogCategoryEnum.AUTH)
    @PostMapping("/resetPassword")
    public R<Void> resetPassword(@Valid @RequestBody SysUserResetPasswordDto dto) {
        sysUserService.resetPassword(dto);
        return R.ok();
    }

    /**
     * 删除
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':delete')")
    @SysLog(value = "删除用户", category = SysLogCategoryEnum.DATA)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsDto dto) {
        Long userId = WebContextUtils.getUserId();
        sysUserService.delete(userId, dto.getIds());
        return R.ok();
    }

    /**
     * 下载用户导入模板
     */
    @PreAuthorize("@pms.hasPermission('api:admin:sysUser:importTemplate')")
    @GetMapping("/importTemplate")
    public ResponseEntity<byte[]> importTemplate() {
        return this.buildExcelResponse(sysUserExcelService.createImportTemplate(), "用户导入模板.xlsx");
    }

    /**
     * 导入用户
     */
    @PreAuthorize("@pms.hasPermission('api:admin:sysUser:import')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<SysUserImportResultVo> importUsers(@RequestParam("file") MultipartFile file) {
        return R.ok(sysUserExcelService.importUsers(file));
    }

    /**
     * 导出用户
     */
    @PreAuthorize("@pms.hasPermission('api:admin:sysUser:export')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUsers(SysUserQueryDto dto) {
        String fileName = "用户数据_" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + ".xlsx";
        return this.buildExcelResponse(sysUserExcelService.exportUsers(dto), fileName);
    }

    // ================================================================================

    /**
     * 查询用户角色
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':queryUserRoles')")
    @GetMapping("/queryUserRoles")
    public R<List<SysRoleEntity>> queryUserRoles(@RequestParam Long userId) {
        return R.ok(sysUserService.queryUserRoles(userId));
    }

    /**
     * 用户挂载角色（全部量操作）
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':setUserRoles')")
    @SysLog(value = "设置用户角色", category = SysLogCategoryEnum.DATA)
    @PostMapping("/setUserRoles")
    public R<Boolean> setUserRoles(@Valid @RequestBody UserRoleSetDto dto) {
        return R.ok(sysUserService.setUserRoles(dto));
    }

    /**
     * 查询用户资源
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':queryUserResources')")
    @GetMapping("/queryUserResources")
    public R<List<SysUserResTreeNodeVo>> queryUserResources(@RequestParam Long userId) {
        return R.ok(sysUserService.queryUserResources(userId));
    }

    // ================================================================================

    /**
     * 查询用户组织
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':queryUserOrgs')")
    @GetMapping("/queryUserOrgs")
    public R<List<SysOrgEntity>> queryUserOrgs(@RequestParam Long userId) {
        return R.ok(sysUserService.queryUserOrgs(userId));
    }

    /**
     * 用户挂载组织（全部量操作）
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':setUserOrgs')")
    @SysLog(value = "设置用户组织", category = SysLogCategoryEnum.DATA)
    @PostMapping("/setUserOrgs")
    public R<Boolean> setUserOrgs(@Valid @RequestBody UserOrgSetDto dto) {
        return R.ok(sysUserService.setUserOrgs(dto));
    }

    /**
     * 查询用户个人信息
     */
    @GetMapping("/mine")
    public R<MineVo> mine() {
        Long userId = WebContextUtils.getUserId();
        return R.ok(sysUserService.mine(userId));
    }

    /**
     * 查询用户个人资源
     */
    @GetMapping("/mineResources")
    public R<List<SysUserResTreeNodeVo>> mineResources() {
        Long userId = WebContextUtils.getUserId();
        return R.ok(sysUserService.queryUserResources(userId));
    }

    /**
     * 更新用户个人信息
     */
    @SysLog(value = "更新用户个人信息", category = SysLogCategoryEnum.DATA)
    @PostMapping("/updateMine")
    public R<Void> updateMine(@Valid @RequestBody SysUserUpdateMineDto dto) {
        Long userId = WebContextUtils.getUserId();
        sysUserService.updateMine(userId, dto);
        return R.ok();
    }

    // ================================================================================

    /**
     * 查询用户岗位
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':queryUserPosts')")
    @GetMapping("/queryUserPosts")
    public R<List<SysPostEntity>> queryUserPosts(@RequestParam Long userId) {
        return R.ok(sysUserService.queryUserPosts(userId));
    }

    /**
     * 用户挂载岗位（全部量操作）
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':setUserPosts')")
    @SysLog(value = "设置用户岗位", category = SysLogCategoryEnum.DATA)
    @PostMapping("/setUserPosts")
    public R<Boolean> setUserPosts(@Valid @RequestBody UserPostSetDto dto) {
        return R.ok(sysUserService.setUserPosts(dto));
    }

    private ResponseEntity<byte[]> buildExcelResponse(byte[] data, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build());
        headers.setContentLength(data.length);
        return ResponseEntity.ok().headers(headers).body(data);
    }
}
