package com.yeshimin.yeahboot.upms.controller;

import com.yeshimin.yeahboot.common.common.enums.SysLogCategoryEnum;
import com.yeshimin.yeahboot.common.common.log.SysLog;
import com.yeshimin.yeahboot.common.controller.base.CrudController;
import com.yeshimin.yeahboot.common.domain.base.R;
import com.yeshimin.yeahboot.data.domain.entity.SysResMountEntity;
import com.yeshimin.yeahboot.data.mapper.SysResMountMapper;
import com.yeshimin.yeahboot.data.repository.SysResMountRepo;
import com.yeshimin.yeahboot.upms.domain.dto.SysResMountSaveDto;
import com.yeshimin.yeahboot.upms.service.SysResMountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 系统资源接口挂载相关
 */
@RestController
@RequestMapping("/admin/sysResMount")
public class SysResMountController extends CrudController<SysResMountMapper, SysResMountEntity, SysResMountRepo> {

    @Autowired
    private SysResMountService sysResMountService;

    public SysResMountController(SysResMountRepo service) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(service);
        setModule("admin:sysResMount")
                .disableCreate()
                .disableQuery()
                .disableDetail()
                .disableUpdate()
                .disableDelete();
    }

    // ================================================================================

    /**
     * 查询视图资源已挂载的接口
     */
    @PreAuthorize("@pms.hasPermission('admin:sysResMount:query')")
    @GetMapping("/queryByViewResId")
    public R<List<SysResMountEntity>> queryByViewResId(@RequestParam Long viewResId) {
        return R.ok(sysResMountService.queryByViewResId(viewResId));
    }

    /**
     * 保存视图资源挂载接口（全量操作）
     */
    @PreAuthorize("@pms.hasPermission('admin:sysResMount:save')")
    @SysLog(value = "设置视图资源接口", category = SysLogCategoryEnum.DATA)
    @PostMapping("/saveByViewResId")
    public R<Boolean> saveByViewResId(@Valid @RequestBody SysResMountSaveDto dto) {
        return R.ok(sysResMountService.saveByViewResId(dto));
    }
}
