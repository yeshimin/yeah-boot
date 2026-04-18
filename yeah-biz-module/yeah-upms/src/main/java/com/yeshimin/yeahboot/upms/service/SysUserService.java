package com.yeshimin.yeahboot.upms.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.domain.base.IdNameVo;
import com.yeshimin.yeahboot.common.service.PasswordService;
import com.yeshimin.yeahboot.data.domain.dto.SysUserQueryDto;
import com.yeshimin.yeahboot.data.domain.entity.*;
import com.yeshimin.yeahboot.data.repository.*;
import com.yeshimin.yeahboot.storage.StorageManager;
import com.yeshimin.yeahboot.upms.domain.dto.*;
import com.yeshimin.yeahboot.upms.domain.vo.MineVo;
import com.yeshimin.yeahboot.upms.domain.vo.SysUserResTreeNodeVo;
import com.yeshimin.yeahboot.upms.domain.vo.SysUserVo;
import com.yeshimin.yeahboot.upms.domain.vo.UserRolesAndResourcesVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserRepo sysUserRepo;
    private final SysOrgRepo sysOrgRepo;
    private final SysUserRoleRepo sysUserRoleRepo;
    private final SysRoleRepo sysRoleRepo;
    private final SysRoleResRepo sysRoleResRepo;
    private final SysResRepo sysResRepo;
    private final SysUserOrgRepo sysUserOrgRepo;
    private final SysPostRepo sysPostRepo;
    private final SysUserPostRepo sysUserPostRepo;

    private final PasswordService passwordService;

    private final StorageManager storageManager;

    /**
     * 创建
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUserEntity create(SysUserCreateDto dto) {
        // 检查：用户名是否已存在
        if (sysUserRepo.countByUsername(dto.getUsername()) > 0) {
            throw new BaseException("用户名已存在");
        }
        // 检查：组织是否存在
        if (CollUtil.isNotEmpty(dto.getOrgIds())) {
            if (sysOrgRepo.countByIds(dto.getOrgIds()) != dto.getOrgIds().size()) {
                throw new BaseException("组织ID不正确");
            }
        }

        // 密码加密
        String encodedPassword = passwordService.encodePassword(dto.getPassword());

        // 创建用户记录
        SysUserEntity entity = BeanUtil.copyProperties(dto, SysUserEntity.class);
        entity.setPassword(encodedPassword);
        entity.insert();

        // 创建用户与组织关联记录
        sysUserOrgRepo.createUserOrgRelations(entity.getId(), dto.getOrgIds());

        // 创建用户与岗位关联记录
        sysUserPostRepo.createUserPostRelations(entity.getId(), dto.getPostIds());

        // 创建用于角色关联记录
        sysUserRoleRepo.createUserRoleRelations(entity.getId(), dto.getRoleIds());

        // 处理文件存储
        if (StrUtil.isNotBlank(dto.getAvatar())) {
            storageManager.markUse(dto.getAvatar());
        }

        return entity;
    }

    /**
     * 查询
     */
    public IPage<SysUserVo> query(Page<SysUserEntity> page, SysUserQueryDto dto) {
        // 查询用户
        IPage<SysUserEntity> pageUser = sysUserRepo.query(page, dto);
        List<Long> userIds = pageUser.getRecords().stream().map(SysUserEntity::getId).collect(Collectors.toList());

        // 查询用户岗位
        List<SysUserPostEntity> listUserPost = sysUserPostRepo.findListByUserIds(userIds);
        Map<Long, List<SysUserPostEntity>> mapUserPosts =
                listUserPost.stream().collect(Collectors.groupingBy(SysUserPostEntity::getUserId));
        List<Long> postIds = listUserPost.stream().map(SysUserPostEntity::getPostId).collect(Collectors.toList());
        Map<Long, SysPostEntity> mapPost = sysPostRepo.findListByIds(postIds)
                .stream().collect(Collectors.toMap(SysPostEntity::getId, v -> v));

        // 查询用户组织
        List<SysUserOrgEntity> listUserOrg = sysUserOrgRepo.findListByUserIds(userIds);
        Map<Long, List<SysUserOrgEntity>> mapUserOrgs =
                listUserOrg.stream().collect(Collectors.groupingBy(SysUserOrgEntity::getUserId));
        List<Long> orgIds = listUserOrg.stream().map(SysUserOrgEntity::getOrgId).collect(Collectors.toList());
        Map<Long, SysOrgEntity> mapOrg = sysOrgRepo.findListByIds(orgIds)
                .stream().collect(Collectors.toMap(SysOrgEntity::getId, v -> v));

        // 查询用户角色
        List<SysUserRoleEntity> listUserRole = sysUserRoleRepo.findListByUserIds(userIds);
        Map<Long, List<SysUserRoleEntity>> mapUserRoles =
                listUserRole.stream().collect(Collectors.groupingBy(SysUserRoleEntity::getUserId));
        List<Long> roleIds = listUserRole.stream().map(SysUserRoleEntity::getRoleId).collect(Collectors.toList());
        Map<Long, SysRoleEntity> mapRole = sysRoleRepo.findListByIds(roleIds)
                .stream().collect(Collectors.toMap(SysRoleEntity::getId, v -> v));

        return pageUser.convert(e -> {
            SysUserVo vo = BeanUtil.copyProperties(e, SysUserVo.class);

            // 岗位
            List<SysUserPostEntity> userPosts = mapUserPosts.getOrDefault(e.getId(), Collections.emptyList());
            vo.setPosts(userPosts.stream().map(r -> {
                SysPostEntity sysPost = mapPost.get(r.getPostId());
                if (sysPost == null) {
                    return null;
                }
                return new IdNameVo(sysPost.getId(), sysPost.getName());
            }).collect(Collectors.toList()));

            // 组织
            List<SysUserOrgEntity> userOrgs = mapUserOrgs.getOrDefault(e.getId(), Collections.emptyList());
            vo.setOrgs(userOrgs.stream().map(r -> {
                SysOrgEntity sysOrg = mapOrg.get(r.getOrgId());
                if (sysOrg == null) {
                    return null;
                }
                return new IdNameVo(sysOrg.getId(), sysOrg.getName());
            }).collect(Collectors.toList()));

            // 角色
            List<SysUserRoleEntity> userRoles = mapUserRoles.getOrDefault(e.getId(), Collections.emptyList());
            vo.setRoles(userRoles.stream().map(r -> {
                SysRoleEntity sysRole = mapRole.get(r.getRoleId());
                if (sysRole == null) {
                    return null;
                }
                return new IdNameVo(sysRole.getId(), sysRole.getName());
            }).filter(Objects::nonNull).collect(Collectors.toList()));
            return vo;
        });
    }

    /**
     * 详情
     */
    public SysUserVo detail(Long id) {
        // 检查：是否存在
        SysUserEntity entity = sysUserRepo.getOneById(id);

        // 查询用户岗位
        List<SysUserPostEntity> listUserPost = sysUserPostRepo.findListByUserId(id);
        Map<Long, SysPostEntity> mapPost = sysPostRepo.findListByIds(listUserPost.stream()
                        .map(SysUserPostEntity::getPostId).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(SysPostEntity::getId, v -> v));

        // 查询用户组织
        List<SysUserOrgEntity> listUserOrg = sysUserOrgRepo.findListByUserId(id);
        Map<Long, SysOrgEntity> mapOrg = sysOrgRepo.findListByIds(listUserOrg.stream()
                        .map(SysUserOrgEntity::getOrgId).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(SysOrgEntity::getId, v -> v));

        // 查询用户角色
        List<SysUserRoleEntity> listUserRole = sysUserRoleRepo.findListByUserId(id);
        Map<Long, SysRoleEntity> mapRole = sysRoleRepo.findListByIds(listUserRole.stream()
                        .map(SysUserRoleEntity::getRoleId).collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(SysRoleEntity::getId, v -> v));

        SysUserVo vo = BeanUtil.copyProperties(entity, SysUserVo.class);

        // 岗位
        vo.setPosts(listUserPost.stream().map(r -> {
            SysPostEntity sysPost = mapPost.get(r.getPostId());
            if (sysPost == null) {
                return null;
            }
            return new IdNameVo(sysPost.getId(), sysPost.getName());
        }).collect(Collectors.toList()));

        // 组织
        vo.setOrgs(listUserOrg.stream().map(r -> {
            SysOrgEntity sysOrg = mapOrg.get(r.getOrgId());
            if (sysOrg == null) {
                return null;
            }
            return new IdNameVo(sysOrg.getId(), sysOrg.getName());
        }).collect(Collectors.toList()));

        // 角色
        vo.setRoles(listUserRole.stream().map(r -> {
            SysRoleEntity sysRole = mapRole.get(r.getRoleId());
            if (sysRole == null) {
                return null;
            }
            return new IdNameVo(sysRole.getId(), sysRole.getName());
        }).filter(Objects::nonNull).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 更新
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUserEntity update(SysUserUpdateDto dto) {
        // 检查：是否存在
        SysUserEntity entity = sysUserRepo.getOneById(dto.getId());
        // 检查：用户名是否冲突
        if (StrUtil.isNotBlank(dto.getUsername()) && !Objects.equals(entity.getUsername(), dto.getUsername())) {
            if (sysUserRepo.countByUsername(dto.getUsername()) > 0) {
                throw new BaseException("用户名已存在");
            }
        }
        // 检查：组织是否存在
        if (CollUtil.isNotEmpty(dto.getOrgIds())) {
            if (sysOrgRepo.countByIds(dto.getOrgIds()) != dto.getOrgIds().size()) {
                throw new BaseException("组织ID不正确");
            }
        }

        // 清空并重新创建用户与组织关联记录
        if (dto.getOrgIds() != null) {
            sysUserOrgRepo.deleteByUserId(entity.getId());
            sysUserOrgRepo.createUserOrgRelations(entity.getId(), dto.getOrgIds());
        }
        // 清空并重新创建用户与岗位关联记录
        if (dto.getPostIds() != null) {
            sysUserPostRepo.deleteByUserId(entity.getId());
            sysUserPostRepo.createUserPostRelations(entity.getId(), dto.getPostIds());
        }
        // 清空并重新创建用户与角色关联记录
        if (dto.getRoleIds() != null) {
            sysUserRoleRepo.deleteByUserId(entity.getId());
            sysUserRoleRepo.createUserRoleRelations(entity.getId(), dto.getRoleIds());
        }

        // 获取旧值
        String oldAvatar = entity.getAvatar();

        // 更新用户信息
        BeanUtil.copyProperties(dto, entity);

        // 密码
        if (StrUtil.isNotBlank(dto.getPassword())) {
            entity.setPassword(passwordService.encodePassword(dto.getPassword()));
        } else {
            // 置空，跳过更新
            entity.setPassword(null);
        }

        // 按需更新头像
        if (StrUtil.isNotBlank(dto.getAvatar()) && !Objects.equals(dto.getAvatar(), oldAvatar)) {
            storageManager.markUse(dto.getAvatar());
            // 将旧的标记为未使用以待自动清理
            storageManager.unmarkUse(oldAvatar);
        } else {
            // 置空，跳过更新
            entity.setAvatar(null);
        }

        entity.updateById();
        return entity;
    }

    /**
     * 删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, List<Long> ids) {
        for (Long id : ids) {
            // 检查：不能删除自己
            if (Objects.equals(userId, id)) {
                throw new BaseException("不能删除自己");
            }
            // 检查：是否存在
            SysUserEntity entity = sysUserRepo.findOneById(id);
            if (entity == null) {
                throw new RuntimeException(String.format("用户[%s]未找到", id));
            }
            entity.deleteById();

            // 删除user-role关联
            boolean result = sysUserRoleRepo.deleteByUserId(id);
            log.debug("deleteByUserId.id[{}]result: {}", id, result);
        }
    }

    // ================================================================================

    /**
     * 查询用户角色
     */
    public List<SysRoleEntity> queryUserRoles(Long userId) {
        // 检查：用户是否存在
        SysUserEntity entity = sysUserRepo.findOneById(userId);
        if (entity == null) {
            throw new RuntimeException("用户未找到");
        }

        // 查询用户角色ID集合
        Set<Long> roleIds = sysUserRoleRepo.findListByUserId(userId)
                .stream().map(SysUserRoleEntity::getRoleId).collect(Collectors.toSet());

        return roleIds.isEmpty() ? Collections.emptyList() : sysRoleRepo.listByIds(roleIds);
    }

    /**
     * 用户挂载角色（全量操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean setUserRoles(UserRoleSetDto dto) {
        Long userId = dto.getUserId();
        Set<Long> roleIds = dto.getRoleIds();

        // 检查：用户是否存在
        SysUserEntity sysUser = sysUserRepo.findOneById(userId);
        if (sysUser == null) {
            throw new RuntimeException("用户未找到");
        }
        if (CollUtil.isNotEmpty(roleIds)) {
            List<SysRoleEntity> listRole = sysRoleRepo.listByIds(roleIds);
            if (listRole.size() != roleIds.size()) {
                throw new RuntimeException("角色ID不合法");
            }
        }

        // clear
        sysUserRoleRepo.deleteByUserId(userId);

        // add
        return sysUserRoleRepo.createUserRoleRelations(userId, roleIds);
    }

    /**
     * 查询用户资源
     */
    public List<SysUserResTreeNodeVo> queryUserResources(Long userId) {
        // 1.查询用户对应的勾选的资源
        List<Long> roleIds = sysUserRoleRepo.findListByUserId(userId)
                .stream().map(SysUserRoleEntity::getRoleId).collect(Collectors.toList());
        Set<Long> checkedSet = sysRoleResRepo.findListByRoleIds(roleIds)
                .stream().map(SysRoleResEntity::getResId).collect(Collectors.toSet());

        // 2.查询资源树
        List<SysUserResTreeNodeVo> listAllVo = sysResRepo.list()
                .stream().map(e -> {
                    SysUserResTreeNodeVo vo = BeanUtil.copyProperties(e, SysUserResTreeNodeVo.class);
                    // 初始化子节点集合对象
                    vo.setChildren(new ArrayList<>());
                    return vo;
                }).collect(Collectors.toList());

        // list to map
        Map<Long, SysUserResTreeNodeVo> mapAll =
                listAllVo.stream().collect(Collectors.toMap(SysUserResTreeNodeVo::getId, v -> v));

        listAllVo.forEach(vo -> {
            // set checked
            vo.setChecked(checkedSet.contains(vo.getId()));

            SysUserResTreeNodeVo parent = mapAll.get(vo.getParentId());
            if (parent != null) {
                parent.getChildren().add(vo);
            }
        });

        return listAllVo.stream().filter(vo -> vo.getParentId() == 0).collect(Collectors.toList());
    }

    /**
     * 更新用户个人信息
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMine(Long userId, SysUserUpdateMineDto dto) {
        // 检查：用户是否存在
        SysUserEntity entity = sysUserRepo.findOneById(userId);
        if (entity == null) {
            throw new RuntimeException("用户未找到");
        }

        // 密码，需要校验原密码
        if (StrUtil.isNotBlank(dto.getOldPassword()) && StrUtil.isNotBlank(dto.getNewPassword())) {
            if (!passwordService.validatePassword(dto.getOldPassword(), entity.getPassword())) {
                throw new BaseException("原密码错误");
            }
            entity.setPassword(passwordService.encodePassword(dto.getNewPassword()));
        } else {
            // 置空，跳过更新
            entity.setPassword(null);
        }

        // 按需更新头像
        if (StrUtil.isNotBlank(dto.getAvatar()) && !Objects.equals(dto.getAvatar(), entity.getAvatar())) {
            storageManager.markUse(dto.getAvatar());
            // 将旧的标记为未使用以待自动清理
            storageManager.unmarkUse(entity.getAvatar());
        } else {
            // 置空，跳过更新
            entity.setAvatar(null);
        }

        // 昵称
        if (StrUtil.isNotBlank(dto.getNickname())) {
            entity.setNickname(dto.getNickname());
        }
        // 手机号，确认校验格式，并且确保唯一
        if (StrUtil.isNotBlank(dto.getMobile()) && !Objects.equals(dto.getMobile(), entity.getMobile())) {
            if (!Validator.isMobile(dto.getMobile())) {
                throw new BaseException("手机号格式不正确");
            }
            if (!Objects.equals(entity.getMobile(), dto.getMobile())
                    && sysUserRepo.countByMobile(dto.getMobile()) > 0) {
                throw new BaseException("手机号已存在");
            }
            entity.setMobile(dto.getMobile());
        }
        // 邮箱，确认校验格式
        if (StrUtil.isNotBlank(dto.getEmail()) && !Objects.equals(dto.getEmail(), entity.getEmail())) {
            if (!Validator.isEmail(dto.getEmail())) {
                throw new BaseException("邮箱格式不正确");
            }
            entity.setEmail(dto.getEmail());
        }
        // 性别
        if (StrUtil.isNotBlank(dto.getGender())) {
            entity.setGender(dto.getGender());
        }

        entity.updateById();
    }

    // ================================================================================

    /**
     * 查询用户岗位
     */
    public List<SysOrgEntity> queryUserOrgs(Long userId) {
        // 检查：用户是否存在
        SysUserEntity entity = sysUserRepo.findOneById(userId);
        if (entity == null) {
            throw new RuntimeException("用户未找到");
        }

        // 查询用户岗位ID集合
        Set<Long> orgIds = sysUserOrgRepo.findListByUserId(userId)
                .stream().map(SysUserOrgEntity::getOrgId).collect(Collectors.toSet());

        return orgIds.isEmpty() ? Collections.emptyList() : sysOrgRepo.listByIds(orgIds);
    }

    /**
     * 用户挂载组织（全量操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean setUserOrgs(UserOrgSetDto dto) {
        Long userId = dto.getUserId();
        Set<Long> orgIds = dto.getOrgIds();

        // 检查：用户是否存在
        SysUserEntity sysUser = sysUserRepo.findOneById(userId);
        if (sysUser == null) {
            throw new RuntimeException("用户未找到");
        }
        if (CollUtil.isNotEmpty(orgIds)) {
            List<SysOrgEntity> listOrg = sysOrgRepo.listByIds(orgIds);
            if (listOrg.size() != orgIds.size()) {
                throw new RuntimeException("组织ID不合法");
            }
        }

        // clear
        sysUserOrgRepo.deleteByUserId(userId);

        // add
        return sysUserOrgRepo.createUserOrgRelations(userId, orgIds);
    }

    // ================================================================================

    /**
     * 查询用户角色和资源
     * called by AuthService
     */
    public UserRolesAndResourcesVo queryUserRolesAndResources(Long userId) {
        // 检查：用户是否存在
        SysUserEntity user = sysUserRepo.findOneById(userId);
        if (user == null) {
            throw new BaseException("用户未找到");
        }

        // 查询角色
        List<Long> roleIds = sysUserRoleRepo.findListByUserId(userId)
                .stream().map(SysUserRoleEntity::getRoleId).collect(Collectors.toList());
        Set<String> roles = roleIds.isEmpty() ? Collections.emptySet() :
                sysRoleRepo.listByIds(roleIds).stream()
                        .map(SysRoleEntity::getCode).filter(StrUtil::isNotBlank).collect(Collectors.toSet());

        // 查询资源
        List<Long> resIds = sysRoleResRepo.findListByRoleIds(roleIds)
                .stream().map(SysRoleResEntity::getResId).distinct().collect(Collectors.toList());
        Set<String> resources = resIds.isEmpty() ? Collections.emptySet() :
                sysResRepo.listByIds(resIds).stream()
                        .map(SysResEntity::getPermission).filter(StrUtil::isNotBlank).collect(Collectors.toSet());

        UserRolesAndResourcesVo vo = new UserRolesAndResourcesVo();
        vo.setUser(user);
        vo.setRoles(roles);
        vo.setResources(resources);
        return vo;
    }


    /**
     * 查询用户个人信息
     */
    public MineVo mine(Long userId) {
        SysUserEntity user = sysUserRepo.getOneById(userId);

        List<Long> roleIds = sysUserRoleRepo.findListByUserId(userId)
                .stream().map(SysUserRoleEntity::getRoleId).collect(Collectors.toList());
        List<SysRoleEntity> roles = sysRoleRepo.findListByIds(roleIds);

        List<Long> orgIds = sysUserOrgRepo.findListByUserId(userId)
                .stream().map(SysUserOrgEntity::getOrgId).collect(Collectors.toList());
        List<SysOrgEntity> orgs = sysOrgRepo.findListByIds(orgIds);

        List<String> permissions = Collections.singletonList("*:*:*");

        MineVo vo = new MineVo();
        vo.setUser(user);
        vo.setRoles(roles);
        vo.setOrgs(orgs);
        vo.setPermissions(permissions);
        return vo;
    }

    // ================================================================================

    /**
     * 查询用户岗位
     */
    public List<SysPostEntity> queryUserPosts(Long userId) {
        // 检查：用户是否存在
        SysUserEntity entity = sysUserRepo.findOneById(userId);
        if (entity == null) {
            throw new RuntimeException("用户未找到");
        }

        // 查询用户岗位ID集合
        Set<Long> postIds = sysUserPostRepo.findListByUserId(userId)
                .stream().map(SysUserPostEntity::getPostId).collect(Collectors.toSet());

        return postIds.isEmpty() ? Collections.emptyList() : sysPostRepo.listByIds(postIds);
    }

    /**
     * 用户挂载岗位（全量操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean setUserPosts(UserPostSetDto dto) {
        Long userId = dto.getUserId();
        Set<Long> postIds = dto.getPostIds();

        // 检查：用户是否存在
        SysUserEntity sysUser = sysUserRepo.findOneById(userId);
        if (sysUser == null) {
            throw new RuntimeException("用户未找到");
        }
        if (CollUtil.isNotEmpty(postIds)) {
            List<SysPostEntity> listPost = sysPostRepo.listByIds(postIds);
            if (listPost.size() != postIds.size()) {
                throw new RuntimeException("岗位ID不合法");
            }
        }

        // clear
        sysUserPostRepo.deleteByUserId(userId);

        // add
        return sysUserPostRepo.createUserPostRelations(userId, postIds);
    }
}
