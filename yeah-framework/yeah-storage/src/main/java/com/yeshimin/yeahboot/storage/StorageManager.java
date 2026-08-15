package com.yeshimin.yeahboot.storage;

import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.enums.StorageTypeEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.common.utils.YsmUtils;
import com.yeshimin.yeahboot.data.domain.entity.SysStorageEntity;
import com.yeshimin.yeahboot.data.repository.SysStorageRepo;
import com.yeshimin.yeahboot.storage.common.properties.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 存储管理器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageManager {

    private final StorageProperties storageProperties;

    private final SysStorageRepo sysStorageRepo;

    private final List<StorageProvider> storageProviders;

    private Map<StorageTypeEnum, StorageProvider> mapStorageProvider;

    @PostConstruct
    public void init() {
        log.info("storageProviders.size: {}", storageProviders.size());

        if (!this.isEnabled()) {
            log.info("[yeah-boot.storage] is disabled.");
            return;
        }

        if (storageProviders.isEmpty()) {
            throw new IllegalArgumentException("No storage providers found");
        }

        // sort by priority
        storageProviders.sort(Comparator.comparing(StorageProvider::getPriority));

        // list to map
        mapStorageProvider = storageProviders.stream()
                .collect(Collectors.toMap(StorageProvider::getStorageType, provider -> provider));
    }

    public SysStorageEntity put(@Nullable String bucketName, @Nullable String path, Object file,
                                @Nullable StorageTypeEnum storageType) {
        return this.put(bucketName, path, file, storageType, false, false);
    }

    /**
     * 获取最终使用的存储类型
     */
    public StorageTypeEnum getStorageType(@Nullable StorageTypeEnum storageType) {
        this.checkEnabled();
        return this.getProvider(storageType).getStorageType();
    }

    /**
     * 全参方法
     */
    public SysStorageEntity put(@Nullable String bucketName, @Nullable String path, Object file,
                                @Nullable StorageTypeEnum storageType, boolean isPublic, boolean isUsed) {
        this.checkEnabled();
        SysStorageEntity entity = this.getProvider(storageType).put(bucketName, path, file, isPublic);
        entity.setIsPublic(isPublic);
        entity.setIsUsed(isUsed);
        // 暂定如果一天后还没被使用到则可以清理掉
        entity.setCleanableTime(LocalDateTime.now().plusDays(1));
        boolean r = entity.insert();
        log.info("StorageManager.put.result: {}", r);
        return entity;
    }

    /**
     * 暂时用不到
     */
    private SysStorageEntity put(@Nullable String bucketName, @Nullable String path, byte[] fileBytes,
                                 String fileOriginName, @Nullable StorageTypeEnum storageType, boolean isPublic) {
        this.checkEnabled();
        SysStorageEntity entity =
                this.getProvider(storageType).put(bucketName, path, fileBytes, fileOriginName, isPublic);
        entity.setIsPublic(isPublic);
        boolean r = entity.insert();
        log.info("StorageManager.put.result: {}", r);
        return entity;
    }

    public StorageGetResult get(String fileKey) {
        this.checkEnabled();
        SysStorageEntity sysStorage = sysStorageRepo.getOneByFileKey(fileKey);
        return this.get(fileKey, sysStorage);
    }

    public StorageGetResult get(String fileKey, SysStorageEntity sysStorage) {
        this.checkEnabled();
        StorageTypeEnum storageType = StorageTypeEnum.of(sysStorage.getStorageType());
        StorageGetResult result = this.getProvider(storageType).get(fileKey, sysStorage);
        if (result == null) {
            throw new BaseException(ErrorCodeEnum.FAIL, "获取文件失败");
        }
        return result;
    }

    /**
     * 删除
     */
    public void delete(String fileKey) {
        this.checkEnabled();
        SysStorageEntity sysStorage = sysStorageRepo.findOneByFileKey(fileKey);
        if (sysStorage == null) {
            return;
        }
        sysStorage.deleteById();

        StorageTypeEnum storageType = StorageTypeEnum.of(sysStorage.getStorageType());
        this.getProvider(storageType).delete(fileKey, sysStorage);
    }

    /**
     * 获取下载信息
     */
    public String getDownloadInfo(String fileKey, String fileName) {
        this.checkEnabled();
        SysStorageEntity sysStorage = sysStorageRepo.getOneByFileKey(fileKey);
        return this.getProvider().getDownloadInfo(fileKey, fileName, sysStorage);
    }

    /**
     * 获取完整路径
     */
    public String getFullPath(SysStorageEntity sysStorage) {
        // name + suffix
        String fileName = this.getKeyWithSuffix(sysStorage.getFileKey(), sysStorage.getSuffix());
        // path + name + suffix
        String finalName = YsmUtils.path(sysStorage.getPath(), fileName);
        String path = YsmUtils.path(sysStorage.getBasePath(), sysStorage.getBucket(), finalName);
        log.info("path: {}", path);
        return path;
    }

    /**
     * 标记使用
     */
    public void markUse(String... fileKey) {
        this.markUse(true, fileKey);
    }

    /**
     * 取消使用标记
     */
    public void unmarkUse(String... fileKey) {
        this.markUse(false, fileKey);
    }

    // ================================================================================

    private StorageProvider getProvider() {
        return this.getProvider(null);
    }

    private StorageProvider getProvider(StorageTypeEnum storageType) {
        if (storageType != null) {
            StorageProvider provider = mapStorageProvider.get(storageType);
            if (provider == null) {
                throw new BaseException(ErrorCodeEnum.FAIL, "不支持的存储类型");
            }
            return provider;
        }
        // storageProviders是根据配置的priority排序的
        return storageProviders.get(0);
    }

    private boolean isEnabled() {
        return storageProperties.getEnabled();
    }

    private void checkEnabled() {
        if (!this.isEnabled()) {
            throw new BaseException(ErrorCodeEnum.FAIL, "存储功能未启用");
        }
    }

    /**
     * getKeyWithSuffix
     */
    private String getKeyWithSuffix(String key, String suffix) {
        return StrUtil.isBlank(suffix) ? key : key + "." + suffix;
    }

    /**
     * 标记使用
     */
    private void markUse(boolean isUsed, String... fileKey) {
        sysStorageRepo.markUse(isUsed, fileKey);
    }
}
