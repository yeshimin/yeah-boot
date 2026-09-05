package com.yeshimin.yeahboot.upms.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.upms.domain.dto.SysPostCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysPostUpdateDto;
import com.yeshimin.yeahboot.data.domain.entity.SysPostEntity;
import com.yeshimin.yeahboot.data.repository.SysPostRepo;
import com.yeshimin.yeahboot.data.repository.SysUserPostRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SysPostService {

    private final SysPostRepo sysPostRepo;
    private final SysUserPostRepo sysUserPostRepo;

    /**
     * 创建
     */
    @Transactional(rollbackFor = Exception.class)
    public SysPostEntity create(SysPostCreateDto dto) {
        // 检查：编码是否已存在
        if (sysPostRepo.countByCode(dto.getCode()) > 0) {
            throw new BaseException("编码已存在");
        }
        // 检查：名称是否已存在
        if (sysPostRepo.countByName(dto.getName()) > 0) {
            throw new BaseException("名称已存在");
        }

        // 创建记录
        SysPostEntity entity = BeanUtil.copyProperties(dto, SysPostEntity.class);
        entity.insert();
        return entity;
    }

    /**
     * 更新
     */
    @Transactional(rollbackFor = Exception.class)
    public SysPostEntity update(SysPostUpdateDto dto) {
        // 检查：是否存在
        SysPostEntity entity = sysPostRepo.getOneById(dto.getId());
        // 检查：编码是否已存在
        if (StrUtil.isNotBlank(dto.getCode()) && !Objects.equals(dto.getCode(), entity.getCode())) {
            if (sysPostRepo.countByCode(dto.getCode()) > 0) {
                throw new BaseException("编码已存在");
            }
        }
        // 检查：名称已存在
        if (StrUtil.isNotBlank(dto.getName()) && !Objects.equals(dto.getName(), entity.getName())) {
            if (sysPostRepo.countByName(dto.getName()) > 0) {
                throw new BaseException("名称已存在");
            }
        }

        SysPostEntity forUpdate = BeanUtil.copyProperties(dto, SysPostEntity.class);
        forUpdate.updateById();
        return forUpdate;
    }

    /**
     * 删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            // 检查：是否存在
            SysPostEntity entity = sysPostRepo.getOneById(id);
            // 检查：是否存在未解除的关联
            if (sysUserPostRepo.countByPostId(id) > 0) {
                throw new BaseException("存在未解除的关联");
            }
            entity.deleteById();
        }
    }
}
