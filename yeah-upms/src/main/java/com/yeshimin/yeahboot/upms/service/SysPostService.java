package com.yeshimin.yeahboot.upms.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.upms.common.errors.BaseException;
import com.yeshimin.yeahboot.upms.domain.dto.SysPostCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysPostUpdateDto;
import com.yeshimin.yeahboot.upms.domain.entity.SysPostEntity;
import com.yeshimin.yeahboot.upms.repository.SysPostRepo;
import com.yeshimin.yeahboot.upms.repository.SysUserPostRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
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
        // 检查：是否已存在
        if (sysPostRepo.countByName(dto.getName()) > 0) {
            throw new BaseException("已存在");
        }

        // 创建记录
        return sysPostRepo.createOne(dto.getName(), dto.getRemark());
    }

    /**
     * 更新
     */
    @Transactional(rollbackFor = Exception.class)
    public SysPostEntity update(SysPostUpdateDto dto) {
        // 检查：是否存在
        SysPostEntity entity = sysPostRepo.getOneById(dto.getId());
        // 检查：是否已存在
        if (StrUtil.isNotBlank(dto.getName()) && !Objects.equals(dto.getName(), entity.getName())) {
            if (sysPostRepo.countByName(dto.getName()) > 0) {
                throw new BaseException("已存在同名数据");
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
