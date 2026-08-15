package com.yeshimin.yeahboot.upms.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.common.common.config.mybatis.QueryHelper;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.upms.domain.dto.SysOrgCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysOrgTreeQueryDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysOrgUpdateDto;
import com.yeshimin.yeahboot.data.domain.entity.SysOrgEntity;
import com.yeshimin.yeahboot.upms.domain.vo.SysOrgTreeNodeVo;
import com.yeshimin.yeahboot.data.repository.SysOrgRepo;
import com.yeshimin.yeahboot.data.repository.SysUserOrgRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysOrgService {

    private final SysOrgRepo sysOrgRepo;
    private final SysUserOrgRepo sysUserOrgRepo;

    /**
     * 创建
     */
    @Transactional(rollbackFor = Exception.class)
    public SysOrgEntity create(SysOrgCreateDto dto) {
        // 检查：父节点是否存在
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            SysOrgEntity parent = sysOrgRepo.getById(dto.getParentId());
            if (parent == null) {
                throw new BaseException("父节点未找到");
            }
        }
        // 检查：同一个父节点下是否存在相同名称
        if (sysOrgRepo.countByParentIdAndName(dto.getParentId(), dto.getName()) > 0) {
            throw new BaseException("同一个父节点下已存在相同名称");
        }

        // 创建记录
        SysOrgEntity entity = BeanUtil.copyProperties(dto, SysOrgEntity.class);
        entity.insert();
        return entity;
    }

    /**
     * 查询树
     */
    public List<SysOrgTreeNodeVo> tree(SysOrgTreeQueryDto dto) {
        // query all
        List<SysOrgEntity> listAll = sysOrgRepo.list(QueryHelper.getQueryWrapper(dto));

        // entity to node vo
        List<SysOrgTreeNodeVo> listAllVo = listAll.stream().map(e -> {
            SysOrgTreeNodeVo vo = BeanUtil.copyProperties(e, SysOrgTreeNodeVo.class);
            // 初始化子节点集合对象
            vo.setChildren(new ArrayList<>());
            return vo;
        }).collect(Collectors.toList());

        // 如果是搜索场景，直接返回列表形式的结果
        if (dto.isQuery()) {
            return listAllVo;
        }

        // list to map
        Map<Long, SysOrgTreeNodeVo> mapAll =
                listAllVo.stream().collect(Collectors.toMap(SysOrgTreeNodeVo::getId, v -> v));

        // convert to tree
        listAllVo.forEach(vo -> {
            SysOrgTreeNodeVo parent = mapAll.get(vo.getParentId());
            if (parent != null) {
                parent.getChildren().add(vo);
            }
        });

        return listAllVo.stream().filter(vo -> vo.getParentId() == 0).collect(Collectors.toList());
    }

    /**
     * 更新
     */
    @Transactional(rollbackFor = Exception.class)
    public SysOrgEntity update(SysOrgUpdateDto dto) {
        // 检查：是否存在
        SysOrgEntity entity = sysOrgRepo.getOneById(dto.getId());
        // 检查：父节点是否存在 ; 父节点不能是自己
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            if (Objects.equals(dto.getParentId(), dto.getId())) {
                throw new BaseException("父节点不能是自己");
            }
            SysOrgEntity parent = sysOrgRepo.findOneById(dto.getParentId());
            if (parent == null) {
                throw new BaseException("父节点未找到");
            }
            // 检查：不能挂载到子节点
            if (isParentInOwnSubTree(dto.getParentId(), dto.getId())) {
                throw new BaseException("不能挂载到子节点");
            }
        }
        // 检查：同一个父节点下是否存在相同名称
        Long parentId = dto.getParentId() != null ? dto.getParentId() : entity.getParentId();
        String name = StrUtil.isNotBlank(dto.getName()) ? dto.getName() : entity.getName();
        boolean parentChanged = dto.getParentId() != null && !Objects.equals(dto.getParentId(), entity.getParentId());
        boolean nameChanged = StrUtil.isNotBlank(dto.getName()) && !Objects.equals(dto.getName(), entity.getName());
        if (parentChanged || nameChanged) {
            if (sysOrgRepo.countByParentIdAndName(parentId, name) > 0) {
                throw new BaseException("同一个父节点下已存在相同名称");
            }
        }

        SysOrgEntity forUpdate = BeanUtil.copyProperties(dto, SysOrgEntity.class);
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
            SysOrgEntity entity = sysOrgRepo.getOneById(id);
            // 检查：是否存在未解除的关联
            if (sysUserOrgRepo.countByOrgId(id) > 0) {
                throw new BaseException("存在未解除的关联");
            }
            // 检查：是否存在子节点
            if (sysOrgRepo.countByParentId(id) > 0) {
                throw new BaseException("存在子节点");
            }
            entity.deleteById();
        }
    }

    /**
     * 判断目标父节点是否在当前节点自己的子树里
     */
    private boolean isParentInOwnSubTree(Long parentId, Long selfId) {
        Long currentId = parentId;
        Set<Long> visitedIds = new HashSet<>();

        while (currentId != null && currentId > 0) {
            if (Objects.equals(currentId, selfId)) {
                return true;
            }
            if (!visitedIds.add(currentId)) {
                throw new BaseException("组织父级关系存在循环");
            }

            SysOrgEntity current = sysOrgRepo.findOneById(currentId);
            if (current == null) {
                return false;
            }
            currentId = current.getParentId();
        }

        return false;
    }
}
