package com.yeshimin.yeahboot.upms.controller;

import com.yeshimin.yeahboot.common.controller.base.CrudController;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.upms.domain.dto.SysDictCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysDictDeleteDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysDictUpdateDto;
import com.yeshimin.yeahboot.data.domain.entity.SysDictEntity;
import com.yeshimin.yeahboot.upms.domain.vo.SysDictTreeNodeVo;
import com.yeshimin.yeahboot.data.mapper.SysDictMapper;
import com.yeshimin.yeahboot.data.repository.SysDictRepo;
import com.yeshimin.yeahboot.upms.service.SysDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统地点相关
 */
@RestController
@RequestMapping("/admin/sysDict")
public class SysDictController extends CrudController<SysDictMapper, SysDictEntity, SysDictRepo> {

    @Autowired
    private SysDictService sysDictService;

    public SysDictController(SysDictRepo service) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(service);
        setModule("admin:sysDict");
    }

    // ================================================================================

    /**
     * 创建
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':create')")
    @PostMapping("/create")
    public R<SysDictEntity> create(@Valid @RequestBody SysDictCreateDto dto) {
        return R.ok(sysDictService.create(dto));
    }

    /**
     * 查询树
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':tree')")
    @GetMapping("/tree")
    public R<List<SysDictTreeNodeVo>> tree(
            @RequestParam(value = "rootNodeCode", required = false) String rootNodeCode) {
        return R.ok(sysDictService.tree(rootNodeCode));
    }

    /**
     * 更新
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':update')")
    @PostMapping("/update")
    public R<SysDictEntity> update(@Valid @RequestBody SysDictUpdateDto dto) {
        return R.ok(sysDictService.update(dto));
    }

    /**
     * 删除
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':delete')")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody SysDictDeleteDto dto) {
        sysDictService.delete(dto.getIds(), dto.getForce());
        return R.ok();
    }
}
