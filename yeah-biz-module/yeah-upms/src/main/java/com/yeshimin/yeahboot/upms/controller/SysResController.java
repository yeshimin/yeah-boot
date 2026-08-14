package com.yeshimin.yeahboot.upms.controller;

import com.yeshimin.yeahboot.common.controller.base.CrudController;
import com.yeshimin.yeahboot.common.domain.base.IdsDto;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.upms.domain.dto.SysResCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysResTreeQueryDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysResUpdateDto;
import com.yeshimin.yeahboot.data.domain.entity.SysResEntity;
import com.yeshimin.yeahboot.upms.domain.vo.SysResTreeNodeVo;
import com.yeshimin.yeahboot.data.mapper.SysResMapper;
import com.yeshimin.yeahboot.data.repository.SysResRepo;
import com.yeshimin.yeahboot.upms.service.SysResService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统资源相关
 */
@RestController
@RequestMapping("/admin/sysRes")
public class SysResController extends CrudController<SysResMapper, SysResEntity, SysResRepo> {

    @Autowired
    private SysResService sysResService;

    public SysResController(SysResRepo sysResRepo) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(sysResRepo);
        setModule("admin:sysRes");
    }

    // ================================================================================

    /**
     * 创建
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':create')")
    @PostMapping("/create")
    public R<SysResEntity> create(@Valid @RequestBody SysResCreateDto dto) {
        return R.ok(sysResService.create(dto));
    }

    /**
     * 查询树
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':tree')")
    @GetMapping("/tree")
    public R<List<SysResTreeNodeVo>> tree(SysResTreeQueryDto dto) {
        return R.ok(sysResService.tree(dto));
    }

    /**
     * 更新
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':update')")
    @PostMapping("/update")
    public R<SysResEntity> update(@Valid @RequestBody SysResUpdateDto dto) {
        return R.ok(sysResService.update(dto));
    }

    /**
     * 删除
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':delete')")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsDto ids) {
        sysResService.delete(ids.getIds());
        return R.ok();
    }
}
