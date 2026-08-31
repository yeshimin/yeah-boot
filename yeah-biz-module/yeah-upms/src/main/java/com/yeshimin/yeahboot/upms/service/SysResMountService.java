package com.yeshimin.yeahboot.upms.service;

import cn.hutool.core.collection.CollUtil;
import com.yeshimin.yeahboot.common.common.consts.CommonConsts;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.data.domain.entity.SysResEntity;
import com.yeshimin.yeahboot.data.domain.entity.SysResMountEntity;
import com.yeshimin.yeahboot.data.repository.SysResMountRepo;
import com.yeshimin.yeahboot.data.repository.SysResRepo;
import com.yeshimin.yeahboot.data.repository.SysRoleResRepo;
import com.yeshimin.yeahboot.upms.common.enums.ResTypeEnum;
import com.yeshimin.yeahboot.upms.domain.dto.SysResMountItemDto;
import com.yeshimin.yeahboot.upms.domain.dto.SysResMountSaveDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysResMountService {

    private final SysResRepo sysResRepo;
    private final SysResMountRepo sysResMountRepo;
    private final SysRoleResRepo sysRoleResRepo;

    /**
     * 查询视图资源已挂载的接口
     */
    public List<SysResMountEntity> queryByViewResId(Long viewResId) {
        this.validateViewResource(viewResId);
        return sysResMountRepo.findListByViewResId(viewResId);
    }

    /**
     * 保存视图资源挂载接口（全量操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveByViewResId(SysResMountSaveDto dto) {
        // 校验参数
        this.validateViewResource(dto.getViewResId());
        List<SysResMountItemDto> items = dto.getItems();
        this.validateApiResources(items);

        // 查询视图资源对应的现有接口资源
        List<SysResMountEntity> existingList = sysResMountRepo.findListByViewResId(dto.getViewResId());
        Map<Long, SysResMountEntity> existingMap = existingList.stream().collect(
                Collectors.toMap(SysResMountEntity::getApiResId, v -> v, (left, right) -> left, LinkedHashMap::new));
        // 参数-接口资源ID集合
        Set<Long> finalApiIds = items.stream()
                .map(SysResMountItemDto::getApiResId)
                .collect(Collectors.toSet());

        // 找出要删除的部分
        List<Long> removedMountIds = existingList.stream()
                .filter(entity -> !finalApiIds.contains(entity.getApiResId()))
                .map(SysResMountEntity::getId)
                .collect(Collectors.toList());

        Boolean result = true;
        List<SysResMountEntity> saveList = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            SysResMountItemDto item = items.get(i);
            SysResMountEntity existing = existingMap.get(item.getApiResId());
            Integer itemSort = item.getSort() == null ? (i + 1) * CommonConsts.DEFAULT_SORT_STEP : item.getSort();
            String itemRemark = item.getRemark();

            if (existing == null) {
                SysResMountEntity entity = new SysResMountEntity();
                entity.setViewResId(dto.getViewResId());
                entity.setApiResId(item.getApiResId());
                entity.setSort(itemSort);
                entity.setRemark(itemRemark);
                saveList.add(entity);
            } else {
                existing.setSort(itemSort);
                existing.setRemark(itemRemark);
                saveList.add(existing);
            }
        }

        // 批量新增和更新
        if (CollUtil.isNotEmpty(saveList)) {
            result = sysResMountRepo.saveOrUpdateBatch(saveList);
        }

        // 清除标记为删除的挂载项和角色资源关联
        if (CollUtil.isNotEmpty(removedMountIds)) {
            result = result && sysResMountRepo.deleteByIds(removedMountIds);
            result = result && sysRoleResRepo.deleteByMountIds(removedMountIds);
        }
        return result;
    }

    // ================================================================================

    /**
     * 校验视图资源
     */
    private void validateViewResource(Long viewResId) {
        SysResEntity viewRes = sysResRepo.getOneById(viewResId);
        if (ResTypeEnum.API.equalsValue(viewRes.getType())) {
            throw new BaseException("接口资源不能挂载接口资源");
        }
    }

    /**
     * 校验接口资源
     */
    private void validateApiResources(List<SysResMountItemDto> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }

        List<Long> apiIds = items.stream().map(SysResMountItemDto::getApiResId).collect(Collectors.toList());
        List<SysResEntity> apis = sysResRepo.listByIds(apiIds);
        if (apis.size() != apiIds.size()) {
            throw new BaseException("接口资源ID不合法");
        }
        boolean hasNonApi = apis.stream().anyMatch(api -> !ResTypeEnum.API.equalsValue(api.getType()));
        if (hasNonApi) {
            throw new BaseException("只能挂载接口资源");
        }
    }
}
