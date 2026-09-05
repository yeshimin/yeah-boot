package com.yeshimin.yeahboot.pub;

import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.service.base.BaseService;
import com.yeshimin.yeahboot.data.domain.entity.SysStorageEntity;
import com.yeshimin.yeahboot.data.repository.SysStorageRepo;
import com.yeshimin.yeahboot.storage.StorageGetResult;
import com.yeshimin.yeahboot.storage.StorageManager;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicStorageService extends BaseService {

    private final SysStorageRepo sysStorageRepo;
    private final StorageManager storageManager;

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
        StorageGetResult result = storageManager.get(fileKey, sysStorage);
        return super.wrapForPreview(sysStorage.getOriginalName(), result.getInputStream(), result.getRedirectUrl());
    }
}
