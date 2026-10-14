package com.yeshimin.yeahboot.pub;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.service.base.BaseService;
import com.yeshimin.yeahboot.data.domain.entity.SysStorageEntity;
import com.yeshimin.yeahboot.data.repository.SysStorageRepo;
import com.yeshimin.yeahboot.data.service.DynamicConfigService;
import com.yeshimin.yeahboot.storage.StorageGetResult;
import com.yeshimin.yeahboot.storage.StorageManager;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicStorageService extends BaseService {

    /** 查询公开存储对象元数据的数据访问入口。 */
    private final SysStorageRepo sysStorageRepo;

    /** 读取底层存储对象的统一管理器。 */
    private final StorageManager storageManager;

    /** 提供公开预览策略动态参数的运行时配置服务。 */
    private final DynamicConfigService dynamicConfigService;

    /**
     * 下载
     */
    public ResponseEntity<InputStreamResource> download(String fileKey, boolean isPublic) {
        SysStorageEntity sysStorage = sysStorageRepo.getOneByFileKey(fileKey);
        // 访问公开文件场景时才进行校验
        if (isPublic && !sysStorage.getIsPublic()) {
            throw new BaseException(ErrorCodeEnum.FAIL, "该文件不可公开访问");
        }
        StorageGetResult result = storageManager.get(fileKey, sysStorage);
        return super.wrap(sysStorage.getOriginalName(), result.getInputStream(), result.getRedirectUrl());
    }

    /**
     * 预览
     */
    public ResponseEntity<InputStreamResource> preview(String fileKey, boolean isPublic) {
        SysStorageEntity sysStorage = sysStorageRepo.getOneByFileKey(fileKey);
        // 访问公开文件场景时才进行校验
        if (isPublic && !sysStorage.getIsPublic()) {
            throw new BaseException(ErrorCodeEnum.FAIL, "该文件不可公开访问");
        }
        this.validatePublicPreview(sysStorage);
        StorageGetResult result = storageManager.get(fileKey, sysStorage);
        return super.wrapForPreview(sysStorage.getOriginalName(), result.getInputStream(), result.getRedirectUrl());
    }

    /**
     * 预览入口同样按系统参数校验，避免存量公开文件绕过上传时的类型限制。
     */
    private void validatePublicPreview(SysStorageEntity storage) {
        if (!Boolean.TRUE.equals(dynamicConfigService.getBoolean(SysConfigEnum.ADMIN_STORAGE_UPLOAD_PUBLIC_PREVIEW_ENABLED))) {
            throw new BaseException(ErrorCodeEnum.FAIL, "当前未开放公开文件预览");
        }
        String extension = FileUtil.getSuffix(StrUtil.blankToDefault(storage.getOriginalName(), ""))
                .toLowerCase(Locale.ROOT);
        if (!this.getPublicPreviewAllowedExtensions().contains(extension)) {
            throw new BaseException(ErrorCodeEnum.FAIL, "当前文件类型不允许公开预览");
        }
    }

    /**
     * 读取允许公开预览的扩展名白名单。
     */
    private Set<String> getPublicPreviewAllowedExtensions() {
        return Arrays.stream(dynamicConfigService
                        .getString(SysConfigEnum.ADMIN_STORAGE_UPLOAD_PUBLIC_PREVIEW_ALLOWED_EXTENSIONS).split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }
}
