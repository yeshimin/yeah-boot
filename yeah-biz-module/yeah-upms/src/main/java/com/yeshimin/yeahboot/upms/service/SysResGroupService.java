package com.yeshimin.yeahboot.upms.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.common.common.consts.CommonConsts;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.data.domain.entity.SysResEntity;
import com.yeshimin.yeahboot.data.domain.entity.SysResGroupEntity;
import com.yeshimin.yeahboot.data.repository.SysResGroupRepo;
import com.yeshimin.yeahboot.data.repository.SysResRepo;
import com.yeshimin.yeahboot.upms.domain.dto.SysResGroupCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysResGroupUpdateDto;
import com.yeshimin.yeahboot.upms.domain.vo.SysResGroupTreeNodeVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysResGroupService {

    private final SysResGroupRepo sysResGroupRepo;
    private final SysResRepo sysResRepo;

    /**
     * 创建
     */
    @Transactional(rollbackFor = Exception.class)
    public SysResGroupEntity create(SysResGroupCreateDto dto) {
        // 检查：父ID合法性
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            SysResGroupEntity parent = sysResGroupRepo.findOneById(dto.getParentId());
            if (parent == null) {
                throw new BaseException("父分组未找到");
            }
        }
        if (sysResGroupRepo.countByParentIdAndName(dto.getParentId(), dto.getName()) > 0) {
            throw new BaseException("同一个父分组下已存在相同名称");
        }

        SysResGroupEntity entity = BeanUtil.copyProperties(dto, SysResGroupEntity.class);
        boolean r = entity.insert();
        log.info("sysResGroup.create.result: {}", r);
        return entity;
    }

    /**
     * 查询树
     */
    public List<SysResGroupTreeNodeVo> tree() {
        List<SysResGroupTreeNodeVo> listAllVo = sysResGroupRepo.list()
                .stream()
                .sorted(this::compareGroup)
                .map(e -> {
                    SysResGroupTreeNodeVo vo = BeanUtil.copyProperties(e, SysResGroupTreeNodeVo.class);
                    vo.setChildren(new ArrayList<>());
                    return vo;
                }).collect(Collectors.toList());

        Map<Long, SysResGroupTreeNodeVo> mapAll =
                listAllVo.stream().collect(Collectors.toMap(SysResGroupTreeNodeVo::getId, v -> v));

        listAllVo.forEach(vo -> {
            SysResGroupTreeNodeVo parent = mapAll.get(vo.getParentId());
            if (parent != null) {
                parent.getChildren().add(vo);
            }
        });

        List<SysResGroupTreeNodeVo> roots = listAllVo.stream()
                .filter(vo -> vo.getParentId() == null || Objects.equals(vo.getParentId(), CommonConsts.ROOT_ID))
                .collect(Collectors.toList());
        this.sortTree(roots);
        return roots;
    }

    /**
     * 更新
     */
    @Transactional(rollbackFor = Exception.class)
    public SysResGroupEntity update(SysResGroupUpdateDto dto) {
        // 检查：是否存在
        SysResGroupEntity entity = sysResGroupRepo.getOneById(dto.getId());
        // 检查：父节点是否存在 ; 父节点不能是自己
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            if (Objects.equals(dto.getParentId(), dto.getId())) {
                throw new BaseException("父节点不能是自己");
            }
            SysResEntity parent = sysResRepo.findOneById(dto.getParentId());
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
            if (sysResRepo.countByParentIdAndName(parentId, name) > 0) {
                throw new BaseException("同一个父节点下已存在相同名称");
            }
        }

        SysResGroupEntity forUpdate = BeanUtil.copyProperties(dto, SysResGroupEntity.class);
        forUpdate.updateById();
        return forUpdate;
    }

    /**
     * 删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            SysResGroupEntity entity = sysResGroupRepo.getOneById(id);
            if (sysResGroupRepo.countByParentId(id) > 0) {
                throw new BaseException("存在子分组，请先删除或调整子分组");
            }
            if (sysResRepo.countByGroupId(id) > 0) {
                throw new BaseException("分组下存在接口资源，请先删除或调整接口资源");
            }
            entity.deleteById();
        }
    }

    // ================================================================================

    private void sortTree(List<SysResGroupTreeNodeVo> nodes) {
        nodes.sort(Comparator
                .comparing((SysResGroupTreeNodeVo e) -> e.getSort() == null ? Integer.MAX_VALUE : e.getSort())
                .thenComparing(e -> e.getId() == null ? Long.MAX_VALUE : e.getId()));
        nodes.forEach(node -> sortTree(node.getChildren()));
    }

    private int compareGroup(SysResGroupEntity a, SysResGroupEntity b) {
        return Comparator
                .comparing((SysResGroupEntity e) -> e.getSort() == null ? Integer.MAX_VALUE : e.getSort())
                .thenComparing(e -> e.getId() == null ? Long.MAX_VALUE : e.getId())
                .compare(a, b);
    }

    private boolean isParentInOwnSubTree(Long parentId, Long selfId) {
        Long currentId = parentId;
        Set<Long> visitedIds = new HashSet<>();

        while (currentId != null && currentId > 0) {
            if (Objects.equals(currentId, selfId)) {
                return true;
            }
            if (!visitedIds.add(currentId)) {
                throw new BaseException("接口分组父级关系存在循环");
            }

            SysResGroupEntity current = sysResGroupRepo.findOneById(currentId);
            if (current == null) {
                return false;
            }
            currentId = current.getParentId();
        }

        return false;
    }
}
