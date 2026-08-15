package com.yeshimin.yeahboot.basic.service.storage;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson2.JSON;
import com.yeshimin.yeahboot.basic.domain.dto.FileDeleteDto;
import com.yeshimin.yeahboot.basic.domain.vo.FileUploadVo;
import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.enums.StorageTypeEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.service.base.BaseService;
import com.yeshimin.yeahboot.data.domain.entity.SysFileEntity;
import com.yeshimin.yeahboot.data.domain.entity.SysStorageEntity;
import com.yeshimin.yeahboot.data.repository.SysFileRepo;
import com.yeshimin.yeahboot.storage.StorageGetResult;
import com.yeshimin.yeahboot.storage.StorageManager;
import com.yeshimin.yeahboot.storage.common.properties.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService extends BaseService {

    private final SysFileRepo sysFileRepo;

    private final StorageManager storageManager;

    private final StorageProperties storageProperties;

    private String bucket;
    private String path;

    @PostConstruct
    public void init() {
        this.bucket = storageProperties.getBiz().getFile().getBucket();
        this.path = storageProperties.getBiz().getFile().getPath();
    }

    /**
     * 上传文件
     */
    @Transactional(rollbackFor = Exception.class)
    public FileUploadVo upload(MultipartFile file, StorageTypeEnum storageType) {
        // 决定bucket，除了local存储方式需要使用this.bucket，其他方式都指定为null
        String bucket = storageType == StorageTypeEnum.LOCAL ? this.bucket : null;
        // 存储文件
        SysStorageEntity result = storageManager.put(bucket, this.path, file, storageType, false, true);
        if (!result.getSuccess()) {
            log.info("result: {}", JSON.toJSONString(result));
            throw new BaseException(ErrorCodeEnum.FAIL, "文件存储失败");
        }

        // 添加文件记录
        SysFileEntity sysFile = new SysFileEntity();
        sysFile.setStorageType(storageType.getValue());
        sysFile.setBasePath(result.getBasePath());
        sysFile.setBucket(result.getBucket());
        sysFile.setPath(result.getPath());
        sysFile.setFileKey(result.getFileKey());
        sysFile.setSuffix(result.getSuffix());
        sysFile.setOriginalName(result.getOriginalName());
        boolean r = sysFile.insert();
        log.info("sysFile.save.result: {}", r);

        FileUploadVo vo = new FileUploadVo();
        vo.setFileKey(result.getFileKey());
        return vo;
    }

    /**
     * 下载
     */
    public ResponseEntity<InputStreamResource> download(String fileKey) {
        SysFileEntity sysFile = this.getLocalFileByFileKey(fileKey);
        StorageGetResult result = storageManager.get(fileKey);
        return super.wrap(sysFile.getOriginalName(), result.getInputStream(), result.getRedirectUrl());
    }

    /**
     * 删除文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(String fileKey) {
        SysFileEntity sysFile = this.getLocalFileByFileKey(fileKey);
        sysFile.deleteById();

        // 删除存储
        storageManager.delete(fileKey);
    }

    /**
     * 批量删除文件
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(FileDeleteDto dto) {
        if (CollectionUtil.isNotEmpty(dto.getIds())) {
            Set<String> fileKeys = sysFileRepo.findListByIds(dto.getIds())
                    .stream().map(SysFileEntity::getFileKey).collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(fileKeys)) {
                fileKeys.forEach(this::delete);
            }
            return;
        }

        if (CollectionUtil.isNotEmpty(dto.getFileKeys())) {
            dto.getFileKeys().forEach(this::delete);
        }
    }

    // ================================================================================

    private SysFileEntity getLocalFileByFileKey(String fileKey) {
        SysFileEntity sysFile = sysFileRepo.findOneByFileKey(fileKey);
        if (sysFile == null) {
            throw new BaseException(ErrorCodeEnum.FAIL, "File not found: " + fileKey);
        }
        return sysFile;
    }
}
