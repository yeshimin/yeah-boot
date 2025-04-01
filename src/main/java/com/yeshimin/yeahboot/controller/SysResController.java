package com.yeshimin.yeahboot.controller;

import com.yeshimin.yeahboot.controller.base.CrudController;
import com.yeshimin.yeahboot.domain.base.R;
import com.yeshimin.yeahboot.domain.dto.SysResCreateDto;
import com.yeshimin.yeahboot.domain.dto.SysResUpdateDto;
import com.yeshimin.yeahboot.domain.entity.SysResEntity;
import com.yeshimin.yeahboot.domain.vo.SysResTreeNodeVo;
import com.yeshimin.yeahboot.mapper.SysResMapper;
import com.yeshimin.yeahboot.repository.SysResRepo;
import com.yeshimin.yeahboot.service.SysResService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统资源相关
 */
@Valid
@RestController
@RequestMapping("/sysRes")
public class SysResController extends CrudController<SysResMapper, SysResEntity, SysResRepo> {

    @Autowired
    private SysResService sysResService;

    public SysResController(SysResRepo sysResRepo) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(sysResRepo);
    }

    // ================================================================================

    /**
     * 创建
     */
    @GetMapping("/create")
    public R<SysResEntity> create(@RequestBody SysResCreateDto dto) {
        return R.ok(sysResService.create(dto));
    }

    /**
     * 查询树
     */
    @GetMapping("/tree")
    public R<List<SysResTreeNodeVo>> tree() {
        return R.ok(sysResService.tree());
    }

    /**
     * 更新
     */
    @PostMapping("/update")
    public R<SysResEntity> update(@RequestBody SysResUpdateDto dto) {
        return R.ok(sysResService.update(dto));
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    public R<Void> delete(@RequestBody Long[] ids) {
        sysResService.delete(ids);
        return R.ok();
    }
}
