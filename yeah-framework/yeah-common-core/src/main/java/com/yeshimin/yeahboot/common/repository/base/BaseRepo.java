package com.yeshimin.yeahboot.common.repository.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.domain.base.BaseEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class BaseRepo<M extends BaseMapper<E>, E extends BaseEntity<E>> extends ServiceImpl<M, E> {

    /**
     * findOneById
     */
    public E findOneById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id不能为空");
        }
        return this.getById(id);
    }

    /**
     * getOneById
     */
    public E getOneById(Long id) {
        E entity = this.findOneById(id);
        if (entity == null) {
            throw new BaseException("数据未找到");
        }
        return entity;
    }

    /**
     * getOneById
     */
    public E getOneById(Long id, String errMsg) {
        E entity = this.findOneById(id);
        if (entity == null) {
            throw new BaseException(errMsg);
        }
        return entity;
    }

    /**
     * countByIds
     */
    public long countByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids不能为空");
        }
        return this.lambdaQuery().in(BaseEntity::getId, ids).count();
    }

    /**
     * countById
     */
    public long countById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id不能为空");
        }
        return this.lambdaQuery().eq(BaseEntity::getId, id).count();
    }

    /**
     * findListByIds
     */
    public List<E> findListByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return this.lambdaQuery().in(BaseEntity::getId, ids).list();
    }
}
