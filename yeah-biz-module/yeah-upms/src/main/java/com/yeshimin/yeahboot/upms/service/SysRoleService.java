package com.yeshimin.yeahboot.upms.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.common.common.consts.CommonConsts;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.domain.base.IdNameStatusVo;
import com.yeshimin.yeahboot.upms.domain.dto.SysRoleCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysRoleResSetDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysRoleUpdateDto;
import com.yeshimin.yeahboot.data.domain.entity.SysResEntity;
import com.yeshimin.yeahboot.data.domain.entity.SysResMountEntity;
import com.yeshimin.yeahboot.data.domain.entity.SysRoleEntity;
import com.yeshimin.yeahboot.data.domain.entity.SysRoleResEntity;
import com.yeshimin.yeahboot.data.repository.SysResMountRepo;
import com.yeshimin.yeahboot.upms.domain.vo.SysRoleResTreeNodeVo;
import com.yeshimin.yeahboot.upms.domain.vo.SysRoleVo;
import com.yeshimin.yeahboot.upms.domain.vo.SysResTreeNodeVo;
import com.yeshimin.yeahboot.data.repository.SysResRepo;
import com.yeshimin.yeahboot.data.repository.SysRoleRepo;
import com.yeshimin.yeahboot.data.repository.SysRoleResRepo;
import com.yeshimin.yeahboot.data.repository.SysUserRoleRepo;
import com.yeshimin.yeahboot.upms.common.enums.ResTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleRepo sysRoleRepo;
    private final SysUserRoleRepo sysUserRoleRepo;
    private final SysRoleResRepo sysRoleResRepo;
    private final SysResRepo sysResRepo;
    private final SysResMountRepo sysResMountRepo;

    /**
     * 创建
     */
    @Transactional(rollbackFor = Exception.class)
    public SysRoleEntity create(SysRoleCreateDto dto) {
        // 检查：编码是否已存在
        if (sysRoleRepo.countByCode(dto.getCode()) > 0) {
            throw new BaseException("编码已存在");
        }
        // 检查：名称是否已存在
        if (sysRoleRepo.countByName(dto.getName()) > 0) {
            throw new BaseException("名称已存在");
        }
        // 检查：资源是否存在
        if (CollUtil.isNotEmpty(dto.getResIds())) {
            if (sysResRepo.countByIds(dto.getResIds()) != dto.getResIds().size()) {
                throw new BaseException("资源ID不正确");
            }
        }

        // 创建记录
        SysRoleEntity entity = sysRoleRepo.createOne(dto.getCode(), dto.getName(), dto.getStatus(), dto.getRemark());

        // 创建角色与资源的关联记录
        sysRoleResRepo.createRoleResRelations(entity.getId(), dto.getResIds());

        return entity;
    }

    /**
     * 详情
     */
    public SysRoleVo detail(Long id) {
        // 检查：是否存在
        SysRoleEntity entity = sysRoleRepo.getOneById(id);

        // 查询角色资源
        List<SysRoleResEntity> listRoleRes = sysRoleResRepo.findListByRoleId(id);
        List<Long> resIds = listRoleRes.stream()
                .map(SysRoleResEntity::getResId)
                .collect(Collectors.toList());
        Map<Long, SysResEntity> mapRes = sysResRepo.findListByIds(resIds)
                .stream().collect(Collectors.toMap(SysResEntity::getId, v -> v));

        SysRoleVo vo = BeanUtil.copyProperties(entity, SysRoleVo.class);

        // 资源
        vo.setResources(resIds.stream().map(resId -> {
            SysResEntity sysRes = mapRes.get(resId);
            if (sysRes == null) {
                return null;
            }
            return new IdNameStatusVo(sysRes.getId(), sysRes.getName(), sysRes.getStatus());
        }).collect(Collectors.toList()));

        return vo;
    }

    /**
     * 更新
     */
    @Transactional(rollbackFor = Exception.class)
    public SysRoleEntity update(SysRoleUpdateDto dto) {
        // 检查：是否存在
        SysRoleEntity entity = sysRoleRepo.getOneById(dto.getId());
        // 检查：编码是否已存在
        if (StrUtil.isNotBlank(dto.getCode()) && !Objects.equals(dto.getCode(), entity.getCode())) {
            if (sysRoleRepo.countByCode(dto.getCode()) > 0) {
                throw new BaseException("编码已存在");
            }
        }
        // 检查：名称是否已存在
        if (StrUtil.isNotBlank(dto.getName()) && !Objects.equals(dto.getName(), entity.getName())) {
            if (sysRoleRepo.countByName(dto.getName()) > 0) {
                throw new BaseException("名称已存在");
            }
        }
        // 检查：资源是否存在
        if (CollUtil.isNotEmpty(dto.getResIds())) {
            if (sysResRepo.countByIds(dto.getResIds()) != dto.getResIds().size()) {
                throw new BaseException("资源ID不正确");
            }
        }

        // 清空并重新创建角色与资源的关联记录
        if (dto.getResIds() != null) {
            sysRoleResRepo.deleteByRoleId(dto.getId());
            sysRoleResRepo.createRoleResRelations(dto.getId(), dto.getResIds());
        }

        BeanUtil.copyProperties(dto, entity);
        entity.updateById();
        return entity;
    }

    /**
     * 删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            // 检查：是否存在
            SysRoleEntity entity = sysRoleRepo.getOneById(id);
            // 检查：是否存在未解除的关联
            if (sysUserRoleRepo.countByRoleId(id) > 0) {
                throw new BaseException("存在未解除的关联");
            }
            entity.deleteById();

            // 删除role-res关联
            boolean result = sysRoleResRepo.deleteByRoleId(id);
            log.debug("deleteByRoleId.id[{}]result: {}", id, result);
        }
    }

    // ================================================================================

    /**
     * 查询指定角色对应的资源数据
     */
    public List<SysRoleResTreeNodeVo> queryResourceTree(Long roleId) {
        // 检查：角色是否存在
        if (sysRoleRepo.countById(roleId) == 0) {
            throw new BaseException("角色未找到");
        }

        // 查询角色对应的所有资源
        List<SysRoleResEntity> listRoleRes = sysRoleResRepo.findListByRoleId(roleId);
        // 获取视图资源IDs
        Set<Long> viewResIds = listRoleRes.stream()
                .filter(e -> e.getMountId() == 0)
                .map(SysRoleResEntity::getResId).collect(Collectors.toSet());
        // 获取挂载IDs
        Set<Long> mountIds = listRoleRes.stream()
                .map(SysRoleResEntity::getMountId)
                .filter(mountId -> mountId > 0)
                .collect(Collectors.toSet());

        // 查询所有资源
        List<SysResEntity> listAll = sysResRepo.list();
        // filter api res, list to map
        Map<Long, SysResEntity> mapApiRes = listAll.stream()
                .filter(e -> Objects.equals(e.getType(), ResTypeEnum.API.getIntValue()))
                .collect(Collectors.toMap(SysResEntity::getId, v -> v));
        // filter view res, entity to vo
        List<SysRoleResTreeNodeVo> listViewResVo = listAll.stream()
                .filter(e -> !Objects.equals(e.getType(), ResTypeEnum.API.getIntValue()))
                .sorted(this::compareRes)
                .map(e -> this.toRoleResTreeNode(e, viewResIds))
                .collect(Collectors.toList());
        // view res list to map
        Map<Long, SysRoleResTreeNodeVo> mapViewRes =
                listViewResVo.stream().collect(Collectors.toMap(SysRoleResTreeNodeVo::getId, v -> v));

        // view res convert to tree
        listViewResVo.forEach(vo -> {
            SysRoleResTreeNodeVo parent = mapViewRes.get(vo.getParentId());
            if (parent != null) {
                parent.getChildren().add(vo);
            }
        });

        // 挂载接口资源到视图资源 ; 回填checked
        List<SysResMountEntity> mounts = sysResMountRepo.findListAll();
        mounts.forEach(mount -> {
            SysRoleResTreeNodeVo viewNode = mapViewRes.get(mount.getViewResId());
            SysResEntity apiRes = mapApiRes.get(mount.getApiResId());
            if (viewNode != null && apiRes != null) {
                viewNode.getChildren().add(this.toMountedApiRoleNode(apiRes, mount, mountIds));
            }
        });

        List<SysRoleResTreeNodeVo> roots = listViewResVo.stream()
                .filter(vo -> Objects.equals(vo.getParentId(), CommonConsts.ROOT_ID))
                .collect(Collectors.toList());
        this.sortRoleResTree(roots);
        return roots;
    }

    private SysRoleResTreeNodeVo toRoleResTreeNode(SysResEntity entity, Set<Long> viewResIds) {
        SysRoleResTreeNodeVo vo = BeanUtil.copyProperties(entity, SysRoleResTreeNodeVo.class);
        vo.setNodeKey("res:" + entity.getId());
        vo.setResId(entity.getId());
        vo.setMountId(0L);
        vo.setTypeName(getTypeName(entity.getType()));
        vo.setMounted(Boolean.FALSE);
        vo.setChildren(new ArrayList<>());
        vo.setChecked(viewResIds.contains(entity.getId()));
        return vo;
    }

    private SysRoleResTreeNodeVo toMountedApiRoleNode(
            SysResEntity apiRes, SysResMountEntity mount, Set<Long> mountIds) {
        SysRoleResTreeNodeVo vo = this.toRoleResTreeNode(apiRes, Collections.emptySet());
        vo.setNodeKey("mount:" + mount.getId());
        vo.setParentId(mount.getViewResId());
        vo.setMounted(Boolean.TRUE);
        vo.setMountId(mount.getId());
        if (mount.getSort() != null) {
            vo.setSort(mount.getSort());
        }
        vo.setChecked(mountIds.contains(mount.getId()));
        return vo;
    }

    private void sortRoleResTree(List<? extends SysResTreeNodeVo> nodes) {
        nodes.sort(Comparator
                .comparing((SysResTreeNodeVo e) -> e.getSort() == null ? Integer.MAX_VALUE : e.getSort())
                .thenComparing(e -> e.getId() == null ? Long.MAX_VALUE : e.getId())
                .thenComparing(SysResTreeNodeVo::getNodeKey));
        nodes.forEach(node -> sortRoleResTree(node.getChildren()));
    }

    private int compareRes(SysResEntity a, SysResEntity b) {
        return Comparator
                .comparing((SysResEntity e) -> e.getSort() == null ? Integer.MAX_VALUE : e.getSort())
                .thenComparing(e -> e.getId() == null ? Long.MAX_VALUE : e.getId())
                .compare(a, b);
    }

    private String getTypeName(Integer type) {
        ResTypeEnum typeEnum = ResTypeEnum.of(String.valueOf(type));
        return typeEnum == null ? "" : typeEnum.getDesc();
    }

    /**
     * 角色挂载资源（全量操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public void setResources(SysRoleResSetDto dto) {
        // 检查：角色是否存在
        SysRoleEntity sysRole = sysRoleRepo.findOneById(dto.getRoleId());
        if (sysRole == null) {
            throw new BaseException("角色未找到");
        }

        // 视图资源ID集合
        Set<Long> viewResIds = dto.getViewResIds();
        // 挂载ID集合
        Set<Long> mountIds = dto.getMountIds();
        if (CollUtil.isNotEmpty(viewResIds)) {
            List<SysResEntity> listRes = sysResRepo.listByIds(viewResIds);
            if (listRes.size() != viewResIds.size()) {
                throw new BaseException("视图资源ID不合法");
            }
            // 检查：viewResIds只允许传视图资源ID
            if (listRes.stream().anyMatch(e -> Objects.equals(e.getType(), ResTypeEnum.API.getIntValue()))) {
                throw new BaseException("viewResIds只允许传视图资源ID");
            }
        }
        List<SysResMountEntity> listMount = new ArrayList<>();
        if (CollUtil.isNotEmpty(mountIds)) {
            listMount = sysResMountRepo.listByIds(mountIds);
            if (listMount.size() != mountIds.size()) {
                throw new BaseException("挂载ID不合法");
            }
        }

        // clear
        boolean result = sysRoleResRepo.deleteByRoleId(dto.getRoleId());
        log.debug("setResources.clear.result: {}", result);

        // add view res
        sysRoleResRepo.createRoleResRelations(dto.getRoleId(), viewResIds);
        // add api res
        for (SysResMountEntity mount : listMount) {
            sysRoleResRepo.createRoleResRelations(
                    dto.getRoleId(), Collections.singleton(mount.getApiResId()), mount.getId());
        }
    }
}
