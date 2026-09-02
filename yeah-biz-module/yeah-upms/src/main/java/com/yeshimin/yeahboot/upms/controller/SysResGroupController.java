package com.yeshimin.yeahboot.upms.controller;

import com.yeshimin.yeahboot.common.common.enums.SysLogCategoryEnum;
import com.yeshimin.yeahboot.common.common.log.SysLog;
import com.yeshimin.yeahboot.common.controller.base.CrudController;
import com.yeshimin.yeahboot.common.domain.base.IdsDto;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.data.domain.entity.SysResGroupEntity;
import com.yeshimin.yeahboot.data.mapper.SysResGroupMapper;
import com.yeshimin.yeahboot.data.repository.SysResGroupRepo;
import com.yeshimin.yeahboot.upms.domain.dto.SysResGroupCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysResGroupUpdateDto;
import com.yeshimin.yeahboot.upms.domain.vo.SysResGroupTreeNodeVo;
import com.yeshimin.yeahboot.upms.service.SysResGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统资源接口分组相关
 */
@RestController
@RequestMapping("/admin/sysResGroup")
public class SysResGroupController extends CrudController<SysResGroupMapper, SysResGroupEntity, SysResGroupRepo> {

    @Autowired
    private SysResGroupService sysResGroupService;

    public SysResGroupController(SysResGroupRepo service) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(service);
        setModule("admin:sysResGroup")
                .disableCreate()
                .disableQuery()
                .disableDetail()
                .disableUpdate()
                .disableDelete();
    }

    // ================================================================================

    /**
     * 查询树
     */
    @PreAuthorize("@pms.hasPermission('admin:sysResGroup:tree')")
    @GetMapping("/tree")
    public R<List<SysResGroupTreeNodeVo>> tree() {
        return R.ok(sysResGroupService.tree());
    }

    /**
     * 创建
     */
    @PreAuthorize("@pms.hasPermission('admin:sysResGroup:create')")
    @SysLog(value = "创建接口资源分组", category = SysLogCategoryEnum.DATA)
    @PostMapping("/create")
    public R<SysResGroupEntity> create(@Valid @RequestBody SysResGroupCreateDto dto) {
        return R.ok(sysResGroupService.create(dto));
    }

    /**
     * 更新
     */
    @PreAuthorize("@pms.hasPermission('admin:sysResGroup:update')")
    @SysLog(value = "更新接口资源分组", category = SysLogCategoryEnum.DATA)
    @PostMapping("/update")
    public R<SysResGroupEntity> update(@Valid @RequestBody SysResGroupUpdateDto dto) {
        return R.ok(sysResGroupService.update(dto));
    }

    /**
     * 删除
     */
    @PreAuthorize("@pms.hasPermission('admin:sysResGroup:delete')")
    @SysLog(value = "删除接口资源分组", category = SysLogCategoryEnum.DATA)
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody IdsDto dto) {
        sysResGroupService.delete(dto.getIds());
        return R.ok();
    }
}
