package com.yeshimin.yeahboot.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.common.common.enums.StorageTypeEnum;
import com.yeshimin.yeahboot.common.common.utils.YsmUtils;
import com.yeshimin.yeahboot.data.domain.entity.SysStorageEntity;
import com.yeshimin.yeahboot.storage.common.properties.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "yeah-boot.storage.impl.local", name = "enabled", havingValue = "true")
public class LocalStorageProvider implements StorageProvider {

    // bucketName 命名只能包含字母、数字、下划线、中划线，且必须以字母开头，长度为3-63个字符
    private static final String BUCKET_NAME_REGEX = "^[a-zA-Z0-9][a-zA-Z0-9_-]{2,62}$";
    // path 命名只能包含字母、数字、下划线、中划线，且必须以字母开头，长度为1-63个字符
    private static final String PATH_REGEX = "^[a-zA-Z0-9][a-zA-Z0-9_-]{0,62}$";

    private final StorageProperties storageProperties;

    private StorageProperties.LocalImpl local;

    @PostConstruct
    public void init() {
        this.local = storageProperties.getImpl().getLocal();
    }

    @SneakyThrows
    @Override
    public SysStorageEntity put(@Nullable String bucket, @Nullable String path, Object file, boolean isPublic) {
        byte[] fileBytes = ((MultipartFile) file).getBytes();
        String fileOriginName = ((MultipartFile) file).getOriginalFilename();
        return this.put(bucket, path, fileBytes, fileOriginName, isPublic);
    }

    @Override
    public SysStorageEntity put(String bucket, String path, byte[] fileBytes, String fileOriginName, boolean isPublic) {
        if (StrUtil.isNotBlank(bucket) && !ReUtil.isMatch(BUCKET_NAME_REGEX, bucket)) {
            throw new IllegalArgumentException("BucketName format error");
        }
        if (StrUtil.isNotBlank(path) && !ReUtil.isMatch(PATH_REGEX, path)) {
            throw new IllegalArgumentException("Path format error");
        }

        // 生成fileKey、获取文件后缀名
        String fileKey = IdUtil.simpleUUID();
        @Nullable String suffix = FileUtil.getSuffix(fileOriginName);
        // 最终的fileKey（不带path）
        String finalKey = getKeyWithSuffix(fileKey, suffix);
//        finalKey = StrUtil.isBlank(path) ? finalKey: FileUtil.file(path, finalKey).getAbsolutePath();
        log.info("finalKey: {}", finalKey);

        String finalBucket = isPublic ? local.getPublicBucket() : StrUtil.isBlank(bucket) ? local.getBucket() : bucket;

        // 按需创建目录
        File directory = this.makeDirectory(local.getBasePath(), finalBucket, path);

        // 生成最终的文件（名）并保存
        File finalFile = FileUtil.file(directory, finalKey);
        log.info("local.finalFile: {}", finalFile.getAbsolutePath());

        boolean success;
        try {
            FileUtil.writeBytes(fileBytes, finalFile);
            success = true;
        } catch (Exception e) {
            log.error("Failed to upload file: {}", e.getMessage(), e);
            success = false;
        }
        log.info("local.success: {}", success);

        SysStorageEntity result = new SysStorageEntity();
        result.setStorageType(StorageTypeEnum.LOCAL.getValue());
        result.setSuccess(success);
        result.setBasePath(local.getBasePath());
        result.setBucket(finalBucket);
        result.setPath(path);
        result.setFileKey(fileKey);
        result.setSuffix(suffix);
        result.setOriginalName(fileOriginName);

        return result;
    }

    @Override
    public StorageGetResult get(String fileKey, SysStorageEntity sysStorage) {
        try {
            return new StorageGetResult(Files.newInputStream(Paths.get(this.getFullPath(sysStorage))), false);
        } catch (IOException e) {
            log.error("本地存储获取文件失败: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void delete(String fileKey, SysStorageEntity sysStorage) {
        FileUtil.del(this.getFullPath(sysStorage));
    }

    @Override
    public String getDownloadInfo(String fileKey, String fileName, SysStorageEntity sysStorage) {
        throw new UnsupportedOperationException("本地存储方式不支持获取下载链接");
    }

    @Override
    public int getPriority() {
        return local.getPriority();
    }

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.LOCAL;
    }

    // ================================================================================

    /**
     * make directory
     */
    public File makeDirectory(String basePath, String bucketName, String path) {
        File directory = new File(YsmUtils.path(basePath, bucketName, path));
        if (!FileUtil.exist(directory)) {
            directory = FileUtil.mkdir(directory);
        }
        return directory;
    }

    /**
     * getFullPath
     */
    public String getFullPath(SysStorageEntity sysStorage) {
        // name + suffix
        String fileName = getKeyWithSuffix(sysStorage.getFileKey(), sysStorage.getSuffix());
        // path + name + suffix
        String finalName = YsmUtils.path(sysStorage.getPath(), fileName);
        String path = YsmUtils.path(sysStorage.getBasePath(), sysStorage.getBucket(), finalName);
        log.info("path: {}", path);
        return path;
    }
}
