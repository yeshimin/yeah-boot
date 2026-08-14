package com.yeshimin.yeahboot.upms.controller;

import com.yeshimin.yeahboot.common.controller.base.CrudController;
import com.yeshimin.yeahboot.common.domain.base.IdsDto;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.upms.domain.dto.SysOrgCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysOrgTreeQueryDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysOrgUpdateDto;
import com.yeshimin.yeahboot.data.domain.entity.SysOrgEntity;
import com.yeshimin.yeahboot.upms.domain.vo.SysOrgTreeNodeVo;
import com.yeshimin.yeahboot.data.mapper.SysOrgMapper;
import com.yeshimin.yeahboot.data.repository.SysOrgRepo;
import com.yeshimin.yeahboot.upms.service.SysOrgService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统组织相关
 */
@RestController
@RequestMapping("/admin/sysOrg")
public class SysOrgController extends CrudController<SysOrgMapper, SysOrgEntity, SysOrgRepo> {

    @Autowired
    private SysOrgService sysOrgService;

    public SysOrgController(SysOrgRepo service) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(service);
        setModule("admin:sysOrg");
    }

    // ================================================================================

    /**
     * 创建
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':create')")
    @PostMapping("/create")
    public R<SysOrgEntity> create(@Valid @RequestBody SysOrgCreateDto dto) {
        return R.ok(sysOrgService.create(dto));
    }

    /**
     * 查询树
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':tree')")
    @GetMapping("/tree")
    public R<List<SysOrgTreeNodeVo>> tree(SysOrgTreeQueryDto dto) {
        return R.ok(sysOrgService.tree(dto));
    }

    /**
     * 更新
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':update')")
    @PostMapping("/update")
    public R<SysOrgEntity> update(@Valid @RequestBody SysOrgUpdateDto dto) {
        return R.ok(sysOrgService.update(dto));
    }

    /**
     * 删除
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':delete')")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsDto dto) {
        sysOrgService.delete(dto.getIds());
        return R.ok();
    }
}
