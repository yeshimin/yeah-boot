package com.yeshimin.yeahboot.common.controller.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yeshimin.yeahboot.common.common.config.mybatis.QueryHelper;
import com.yeshimin.yeahboot.common.common.enums.SysLogCategoryEnum;
import com.yeshimin.yeahboot.common.common.log.SysLog;
import com.yeshimin.yeahboot.common.controller.validation.Create;
import com.yeshimin.yeahboot.common.controller.validation.Query;
import com.yeshimin.yeahboot.common.controller.validation.Update;
import com.yeshimin.yeahboot.common.domain.base.BaseEntity;
import com.yeshimin.yeahboot.common.domain.base.R;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collection;

/**
 * 提供基础的CRUD接口
 *
 * @param <M> BaseMapper
 * @param <E> Entity
 * @param <S> ServiceImpl
 */
@Slf4j
@RequiredArgsConstructor
public class CrudController<M extends BaseMapper<E>, E extends BaseEntity<E>, S extends ServiceImpl<M, E>>
        extends BaseController {

    private final S service;

    private String module;

    @Getter
    private boolean createDisabled = false;
    @Getter
    private boolean queryDisabled = false;
    @Getter
    private boolean detailDisabled = false;
    @Getter
    private boolean updateDisabled = false;
    @Getter
    private boolean deleteDisabled = false;

    /**
     * CRUD-创建
     */
    @Operation(summary = "创建数据")
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':crud:create')")
    @SysLog(value = "创建数据", category = SysLogCategoryEnum.DATA)
    @PostMapping("/crud/create")
    @Transactional(rollbackFor = Exception.class)
    public R<E> crudCreate(@Validated(Create.class) @RequestBody E e) {
        if (createDisabled) {
            return R.fail("该接口已被禁用");
        }
        boolean r = service.save(e);
        log.debug("crudCreate.result: {}", r);
        return R.ok(e);
    }

    /**
     * CRUD-查询
     */
    @Operation(summary = "分页查询数据")
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':crud:query')")
    @GetMapping("/crud/query")
    public R<Page<E>> crudQuery(Page<E> page, @Validated(Query.class) E query) {
        if (queryDisabled) {
            return R.fail("该接口已被禁用");
        }
        @SuppressWarnings("unchecked")
        Class<E> clazz = (Class<E>) query.getClass();
        return R.ok(service.page(page, QueryHelper.getQueryWrapper(query, clazz)));
    }

    /**
     * CRUD-详情
     */
    @Operation(summary = "查询数据详情")
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':crud:detail')")
    @GetMapping("/crud/detail")
    public R<E> crudDetail(Long id) {
        if (detailDisabled) {
            return R.fail("该接口已被禁用");
        }
        if (id == null) {
            return R.fail("ID不能为空");
        }
        E e = service.getById(id);
        if (e == null) {
            return R.fail("数据未找到");
        }
        return R.ok(e);
    }

    /**
     * CRUD-更新
     */
    @Operation(summary = "更新数据")
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':crud:update')")
    @SysLog(value = "更新数据", category = SysLogCategoryEnum.DATA)
    @PostMapping("/crud/update")
    @Transactional(rollbackFor = Exception.class)
    public R<E> crudUpdate(@Validated(Update.class) @RequestBody E e) {
        if (updateDisabled) {
            return R.fail("该接口已被禁用");
        }
        boolean r = service.updateById(e);
        log.debug("crudUpdate.result: {}", r);
        if (!r) {
            return R.fail("更新失败");
        }
        return R.ok(service.getById(e.getId()));
    }

    /**
     * CRUD-删除
     */
    @Operation(summary = "删除数据")
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':crud:delete')")
    @SysLog(value = "删除数据", category = SysLogCategoryEnum.DATA)
    @PostMapping("/crud/delete")
    @Transactional(rollbackFor = Exception.class)
    public R<Void> crudDelete(@RequestBody Collection<Long> ids) {
        if (deleteDisabled) {
            return R.fail("该接口已被禁用");
        }
        boolean r = service.removeBatchByIds(ids);
        log.debug("crudDelete.result: {}", r);
        return R.ok();
    }

    // ================================================================================

    /**
     * disable create
     */
    public CrudController<M, E, S> disableCreate() {
        this.createDisabled = true;
        return this;
    }

    /**
     * disable query
     */
    public CrudController<M, E, S> disableQuery() {
        this.queryDisabled = true;
        return this;
    }

    /**
     * disable detail
     */
    public CrudController<M, E, S> disableDetail() {
        this.detailDisabled = true;
        return this;
    }

    /**
     * disable update
     */
    public CrudController<M, E, S> disableUpdate() {
        this.updateDisabled = true;
        return this;
    }

    /**
     * disable delete
     */
    public CrudController<M, E, S> disableDelete() {
        this.deleteDisabled = true;
        return this;
    }

    // ================================================================================

    public String getModule() {
        if (this.module == null || this.module.isEmpty()) {
            throw new IllegalArgumentException("module不能为空，需要在controller构造函数中调用super.setModule方法进行设置");
        }
        return this.module;
    }

    public CrudController<M, E, S> setModule(String module) {
        this.module = module;
        return this;
    }
}
