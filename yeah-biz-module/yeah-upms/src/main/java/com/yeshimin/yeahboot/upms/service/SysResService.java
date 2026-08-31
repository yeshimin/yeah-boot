package com.yeshimin.yeahboot.upms.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.common.common.config.mybatis.QueryHelper;
import com.yeshimin.yeahboot.common.common.consts.CommonConsts;
import com.yeshimin.yeahboot.common.common.enums.DataStatusEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.data.domain.entity.SysResEntity;
import com.yeshimin.yeahboot.data.domain.entity.SysResGroupEntity;
import com.yeshimin.yeahboot.data.repository.SysResGroupRepo;
import com.yeshimin.yeahboot.data.repository.SysResMountRepo;
import com.yeshimin.yeahboot.data.repository.SysResRepo;
import com.yeshimin.yeahboot.data.repository.SysRoleResRepo;
import com.yeshimin.yeahboot.upms.common.enums.ResTypeEnum;
import com.yeshimin.yeahboot.upms.domain.dto.SysResCreateDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysResTreeQueryDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysResUpdateDto;
import com.yeshimin.yeahboot.upms.domain.vo.SysResApiTreeNodeVo;
import com.yeshimin.yeahboot.upms.domain.vo.SysResTreeNodeVo;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class SysResService {

    private final SysResRepo sysResRepo;
    private final SysRoleResRepo sysRoleResRepo;
    private final SysResGroupRepo sysResGroupRepo;
    private final SysResMountRepo sysResMountRepo;

    /**
     * 创建
     */
    @Transactional(rollbackFor = Exception.class)
    public SysResEntity create(SysResCreateDto dto) {
        // 检查：父节点是否存在
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            SysResEntity parent = sysResRepo.getById(dto.getParentId());
            if (parent == null) {
                throw new BaseException("父节点未找到");
            }
        }
        // 检查：同一个父节点下是否存在相同名称
        if (sysResRepo.countByParentIdAndName(dto.getParentId(), dto.getName()) > 0) {
            throw new BaseException("同一个父节点下已存在相同名称");
        }
        // 检查：接口资源分组ID是否存在
        if (dto.getGroupId() != null && dto.getGroupId() > 0) {
            SysResGroupEntity group = sysResGroupRepo.getById(dto.getGroupId());
            if (group == null) {
                throw new BaseException("接口资源分组未找到");
            }
        }
        // 检查：权限标识是否已存在
        if (StrUtil.isNotBlank(dto.getPermission()) && sysResRepo.countByPermission(dto.getPermission()) > 0) {
            throw new BaseException("权限标识已存在");
        }

        // 创建记录
        SysResEntity entity = BeanUtil.copyProperties(dto, SysResEntity.class);
        entity.insert();
        return entity;
    }

    /**
     * 查询资源树（兼容旧接口）
     */
    public List<SysResTreeNodeVo> tree(SysResTreeQueryDto dto) {
        // query all
        List<SysResEntity> listAll = sysResRepo.list(QueryHelper.getQueryWrapper(dto));
        listAll.sort(this::compareRes);

        // entity to node vo
        List<SysResTreeNodeVo> listAllVo = listAll.stream()
                .map(this::toTreeNode)
                .collect(Collectors.toList());

        // 如果是搜索场景，直接返回列表形式的结果
        if (dto.isQuery()) {
            return listAllVo;
        }

        // list to map
        return this.buildResTree(listAllVo);
    }

    /**
     * 查询视图资源树：菜单、页面、按钮、分组
     */
    public List<SysResTreeNodeVo> viewTree(SysResTreeQueryDto dto) {
        // 查询视图类资源
        List<SysResEntity> listAll = sysResRepo.list(QueryHelper.getQueryWrapper(dto))
                .stream()
                .filter(e -> !this.isApi(e.getType()))
                .sorted(this::compareRes)
                .collect(Collectors.toList());

        // entity to node vo
        List<SysResTreeNodeVo> listAllVo = listAll.stream()
                .map(this::toTreeNode)
                .collect(Collectors.toList());

        // if query scene, return list all
        if (dto.isQuery()) {
            return listAllVo;
        }

        // build tree
        return this.buildResTree(listAllVo);
    }

    /**
     * 查询接口资源树：接口分组 + 接口资源
     */
    public List<SysResApiTreeNodeVo> apiTree(SysResTreeQueryDto dto) {
        // only for api type
        if (dto.getType() != null && !Objects.equals(dto.getType(), ResTypeEnum.API.getIntValue())) {
            return new ArrayList<>();
        }

        // query all api res groups
        List<SysResGroupEntity> groups = sysResGroupRepo.list();
        groups.sort(this::compareGroup);

        // groups list to map
        Map<Long, SysResApiTreeNodeVo> groupMap = groups.stream()
                .map(this::toApiGroupNode)
                .collect(Collectors.toMap(SysResApiTreeNodeVo::getId, v -> v));

        // build groups tree
        List<SysResApiTreeNodeVo> roots = new ArrayList<>();
        groupMap.values().forEach(node -> {
            SysResApiTreeNodeVo parent = groupMap.get(node.getParentId());
            if (parent != null) {
                parent.getChildren().add(node);
            } else {
                roots.add(node);
            }
        });

        // build tree with api res node
        List<SysResEntity> apis = this.queryApiList(dto);
        SysResApiTreeNodeVo ungroupedNode = null;
        for (SysResEntity api : apis) {
            SysResApiTreeNodeVo apiNode = this.toApiResNode(api);
            SysResApiTreeNodeVo groupNode = groupMap.get(api.getGroupId());
            if (groupNode == null) {
                if (ungroupedNode == null) {
                    ungroupedNode = this.buildUngroupedApiNode();
                    roots.add(ungroupedNode);
                }
                groupNode = ungroupedNode;
            }
            groupNode.getChildren().add(apiNode);
        }

        this.sortApiTree(roots);
        return roots;
    }

    /**
     * 更新
     */
    @Transactional(rollbackFor = Exception.class)
    public SysResEntity update(SysResUpdateDto dto) {
        // 检查：是否存在
        SysResEntity entity = sysResRepo.getOneById(dto.getId());
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
        // 检查：接口资源分组ID是否存在
        if (dto.getGroupId() != null && dto.getGroupId() > 0) {
            SysResGroupEntity group = sysResGroupRepo.getById(dto.getGroupId());
            if (group == null) {
                throw new BaseException("接口资源分组未找到");
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
        // 检查：权限标识是否已存在
        if (dto.getPermission() != null && !Objects.equals(dto.getPermission(), entity.getPermission())) {
            if (StrUtil.isNotBlank(dto.getPermission()) && sysResRepo.countByPermission(dto.getPermission()) > 0) {
                throw new BaseException("权限标识已存在");
            }
        }

        SysResEntity forUpdate = BeanUtil.copyProperties(dto, SysResEntity.class);
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
            SysResEntity entity = sysResRepo.getOneById(id);
            // 检查：是否存在未解除的关联
            if (sysRoleResRepo.countByResId(id) > 0) {
                throw new BaseException("存在未解除的角色授权关联");
            }
            // 检查：是否存在子节点
            if (sysResRepo.countByParentId(id) > 0) {
                throw new BaseException("存在子资源，请先删除或调整子资源");
            }
            if (sysResMountRepo.countByViewResId(id) > 0 || sysResMountRepo.countByApiResId(id) > 0) {
                throw new BaseException("存在未解除的接口挂载关联");
            }
            entity.deleteById();
        }
    }

    private List<SysResEntity> queryApiList(SysResTreeQueryDto dto) {
        return sysResRepo.lambdaQuery()
                .eq(SysResEntity::getType, ResTypeEnum.API.getIntValue())
                .eq(dto.getGroupId() != null, SysResEntity::getGroupId, dto.getGroupId())
                .like(StrUtil.isNotBlank(dto.getName()), SysResEntity::getName, dto.getName())
                .eq(StrUtil.isNotBlank(dto.getStatus()), SysResEntity::getStatus, dto.getStatus())
                .list()
                .stream()
                .sorted(this::compareRes)
                .collect(Collectors.toList());
    }

    private SysResTreeNodeVo toTreeNode(SysResEntity entity) {
        SysResTreeNodeVo vo = BeanUtil.copyProperties(entity, SysResTreeNodeVo.class);
        vo.setNodeKey("res:" + entity.getId());
        vo.setResId(entity.getId());
        vo.setTypeName(getTypeName(entity.getType()));
        vo.setMounted(Boolean.FALSE);
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    public SysResTreeNodeVo toMountedApiTreeNode(SysResEntity api, Long viewResId, Integer sort) {
        SysResTreeNodeVo vo = toTreeNode(api);
        vo.setNodeKey("mount:" + viewResId + ":api:" + api.getId());
        vo.setParentId(viewResId);
        vo.setMounted(Boolean.TRUE);
        if (sort != null) {
            vo.setSort(sort);
        }
        return vo;
    }

    private List<SysResTreeNodeVo> buildResTree(List<SysResTreeNodeVo> listAllVo) {
        Map<Long, SysResTreeNodeVo> mapAll =
                listAllVo.stream().collect(Collectors.toMap(SysResTreeNodeVo::getId, v -> v));

        listAllVo.forEach(vo -> {
            SysResTreeNodeVo parent = mapAll.get(vo.getParentId());
            if (parent != null) {
                parent.getChildren().add(vo);
            }
        });

        List<SysResTreeNodeVo> roots = listAllVo.stream()
                .filter(vo -> Objects.equals(vo.getParentId(), CommonConsts.ROOT_ID))
                .collect(Collectors.toList());
        sortResTree(roots);
        return roots;
    }

    private SysResApiTreeNodeVo toApiGroupNode(SysResGroupEntity group) {
        SysResApiTreeNodeVo vo = new SysResApiTreeNodeVo();
        vo.setNodeKey("group:" + group.getId());
        vo.setId(group.getId());
        vo.setGroupId(group.getId());
        vo.setParentId(group.getParentId());
        vo.setName(group.getName());
        vo.setType(ResTypeEnum.GROUP.getIntValue());
        vo.setTypeName(ResTypeEnum.GROUP.getDesc());
        vo.setStatus(DataStatusEnum.ENABLED.getValue());
        vo.setSort(group.getSort());
        vo.setRemark(group.getRemark());
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    private SysResApiTreeNodeVo toApiResNode(SysResEntity api) {
        SysResApiTreeNodeVo vo = new SysResApiTreeNodeVo();
        vo.setNodeKey("api:" + api.getId());
        vo.setId(api.getId());
        vo.setResId(api.getId());
        vo.setGroupId(api.getGroupId());
        vo.setParentId(vo.getGroupId());
        vo.setName(api.getName());
        vo.setType(api.getType());
        vo.setTypeName(getTypeName(api.getType()));
        vo.setPermission(api.getPermission());
        vo.setStatus(api.getStatus());
        vo.setSort(api.getSort());
        vo.setRemark(api.getRemark());
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    private SysResApiTreeNodeVo buildUngroupedApiNode() {
        SysResApiTreeNodeVo vo = new SysResApiTreeNodeVo();
        vo.setNodeKey("group:0");
        vo.setId(CommonConsts.ROOT_ID);
        vo.setGroupId(CommonConsts.ROOT_ID);
        vo.setParentId(CommonConsts.ROOT_ID);
        vo.setName("未分组");
        vo.setType(ResTypeEnum.GROUP.getIntValue());
        vo.setTypeName(ResTypeEnum.GROUP.getDesc());
        vo.setStatus(DataStatusEnum.ENABLED.getValue());
        vo.setSort(Integer.MAX_VALUE);
        vo.setRemark("未指定接口分组的接口资源");
        vo.setChildren(new ArrayList<>());
        return vo;
    }

    private void sortResTree(List<SysResTreeNodeVo> nodes) {
        nodes.sort(this::compareTreeNode);
        nodes.forEach(node -> sortResTree(node.getChildren()));
    }

    private void sortApiTree(List<SysResApiTreeNodeVo> nodes) {
        nodes.sort(this::compareApiTreeNode);
        nodes.forEach(node -> sortApiTree(node.getChildren()));
    }

    private int compareRes(SysResEntity a, SysResEntity b) {
        return Comparator
                .comparing((SysResEntity e) -> e.getSort() == null ? Integer.MAX_VALUE : e.getSort())
                .thenComparing(e -> e.getId() == null ? Long.MAX_VALUE : e.getId())
                .compare(a, b);
    }

    private int compareGroup(SysResGroupEntity a, SysResGroupEntity b) {
        return Comparator
                .comparing((SysResGroupEntity e) -> e.getSort() == null ? Integer.MAX_VALUE : e.getSort())
                .thenComparing(e -> e.getId() == null ? Long.MAX_VALUE : e.getId())
                .compare(a, b);
    }

    private int compareTreeNode(SysResTreeNodeVo a, SysResTreeNodeVo b) {
        return Comparator
                .comparing((SysResTreeNodeVo e) -> e.getSort() == null ? Integer.MAX_VALUE : e.getSort())
                .thenComparing(e -> e.getId() == null ? Long.MAX_VALUE : e.getId())
                .thenComparing(SysResTreeNodeVo::getNodeKey)
                .compare(a, b);
    }

    private int compareApiTreeNode(SysResApiTreeNodeVo a, SysResApiTreeNodeVo b) {
        return Comparator
                .comparing((SysResApiTreeNodeVo e) -> e.getSort() == null ? Integer.MAX_VALUE : e.getSort())
                .thenComparing(e -> e.getId() == null ? Long.MAX_VALUE : e.getId())
                .thenComparing(SysResApiTreeNodeVo::getNodeKey)
                .compare(a, b);
    }

    private boolean isApi(Integer type) {
        return Objects.equals(type, ResTypeEnum.API.getIntValue());
    }

    private String getTypeName(Integer type) {
        ResTypeEnum typeEnum = ResTypeEnum.of(String.valueOf(type));
        return typeEnum == null ? "" : typeEnum.getDesc();
    }

    /**
     * 判断目标父节点是否在当前节点自己的子树里
     * 如果是在自己的子树里，逐级向上查找，最终肯定会碰到自己这个节点
     */
    private boolean isParentInOwnSubTree(Long parentId, Long selfId) {
        Long currentId = parentId;
        Set<Long> visitedIds = new HashSet<>();

        while (currentId != null && currentId > 0) {
            if (Objects.equals(currentId, selfId)) {
                return true;
            }
            if (!visitedIds.add(currentId)) {
                throw new BaseException("资源父级关系存在循环");
            }

            SysResEntity current = sysResRepo.findOneById(currentId);
            if (current == null) {
                return false;
            }
            currentId = current.getParentId();
        }

        return false;
    }
}
