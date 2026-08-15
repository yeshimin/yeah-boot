package com.yeshimin.yeahboot.basic.service.storage;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson2.JSON;
import com.yeshimin.yeahboot.basic.domain.dto.FileDeleteDto;
import com.yeshimin.yeahboot.basic.domain.vo.FileUploadVo;
import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.enums.StorageTypeEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.service.base.BaseService;
import com.yeshimin.yeahboot.data.domain.entity.SysStorageEntity;
import com.yeshimin.yeahboot.data.repository.SysStorageRepo;
import com.yeshimin.yeahboot.storage.StorageGetResult;
import com.yeshimin.yeahboot.storage.StorageManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService extends BaseService {

    private final SysStorageRepo sysStorageRepo;
    private final StorageManager storageManager;

    /**
     * 上传文件
     */
    @Transactional(rollbackFor = Exception.class)
    public FileUploadVo upload(MultipartFile file, StorageTypeEnum storageType, String path,
                               Boolean isPublic, Boolean isUsed) {
        // 存储文件
        SysStorageEntity result = storageManager.put(null, path, file, storageType, isPublic, isUsed);
        if (!result.getSuccess()) {
            log.info("result: {}", JSON.toJSONString(result));
            throw new BaseException(ErrorCodeEnum.FAIL, "文件存储失败");
        }

        FileUploadVo vo = new FileUploadVo();
        vo.setFileKey(result.getFileKey());
        return vo;
    }

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
     * 删除文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String fileKey) {
        // 删除存储
        storageManager.delete(fileKey);
    }

    /**
     * 批量删除文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(FileDeleteDto dto) {
        if (CollectionUtil.isNotEmpty(dto.getIds())) {
            Set<String> fileKeys = sysStorageRepo.findListByIds(dto.getIds())
                    .stream().map(SysStorageEntity::getFileKey).collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(fileKeys)) {
                fileKeys.forEach(this::delete);
            }
            return;
        }

        if (CollectionUtil.isNotEmpty(dto.getFileKeys())) {
            dto.getFileKeys().forEach(this::delete);
        }
    }
}
