package com.yeshimin.yeahboot.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.common.common.enums.StorageTypeEnum;
import com.yeshimin.yeahboot.common.common.utils.YsmUtils;
import com.yeshimin.yeahboot.data.domain.entity.SysStorageEntity;
import com.yeshimin.yeahboot.storage.common.properties.StorageProperties;
import io.minio.MinioClient;
import io.minio.PutObjectOptions;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "yeah-boot.storage.impl.minio", name = "enabled", havingValue = "true")
public class MinioStorageProvider implements StorageProvider {

    private MinioClient minioClient;

    private final StorageProperties storageProperties;

    private StorageProperties.MinioImpl minio;

    @SneakyThrows
    @PostConstruct
    public void init() {
        this.minio = storageProperties.getImpl().getMinio();

        if (!minio.getEnabled()) {
            log.info("[yeah-boot.storage.impl.minio] is disabled.");
            return;
        }

        minioClient = new MinioClient(
                minio.getEndpoint(), minio.getAccessKey(), minio.getSecretKey());
    }

    @Override
    public SysStorageEntity put(@Nullable String bucket, @Nullable String path, Object file, boolean isPublic) {
        MultipartFile mFile = (MultipartFile) file;

        // 生成fileKey
        String fileKey = IdUtil.simpleUUID();
        @Nullable String suffix = FileUtil.getSuffix(mFile.getOriginalFilename());
        // 最终的fileKey
        String finalKey = getKeyWithSuffix(fileKey, suffix);
        finalKey = YsmUtils.path(path, finalKey);
        log.info("finalKey: {}", finalKey);

        String finalBucket = isPublic ? minio.getPublicBucket() : StrUtil.isBlank(bucket) ? minio.getBucket() : bucket;

        boolean success;
        try {
            minioClient.putObject(finalBucket, finalKey, mFile.getInputStream(),
                    new PutObjectOptions(-1, PutObjectOptions.MIN_MULTIPART_SIZE));
            success = true;
        } catch (Exception e) {
            log.error("MinIO存储失败: {}", e.getMessage(), e);
            success = false;
        }

        SysStorageEntity result = new SysStorageEntity();
        result.setStorageType(StorageTypeEnum.MINIO.getValue());
        result.setSuccess(success);
        result.setBasePath("");
        result.setBucket(finalBucket);
        result.setPath(path);
        result.setFileKey(fileKey);
        result.setSuffix(suffix);
        result.setOriginalName(mFile.getOriginalFilename());

        return result;
    }

    @Override
    public SysStorageEntity put(String bucket, String path, byte[] fileBytes, String fileOriginName, boolean isPublic) {
        // 生成fileKey
        String fileKey = IdUtil.simpleUUID();
        @Nullable String suffix = FileUtil.getSuffix(fileOriginName);
        // 最终的fileKey
        String finalKey = getKeyWithSuffix(fileKey, suffix);
        finalKey = YsmUtils.path(path, finalKey);
        log.info("finalKey: {}", finalKey);

        String finalBucket = isPublic ? minio.getPublicBucket() : StrUtil.isBlank(bucket) ? minio.getBucket() : bucket;

        boolean success;
        try (ByteArrayInputStream in = new ByteArrayInputStream(fileBytes)) {
            PutObjectOptions options = new PutObjectOptions(fileBytes.length, -1);
            options.setContentType(FileUtil.getMimeType(fileOriginName));

            minioClient.putObject(bucket, finalKey, in, options);
            success = true;
        } catch (Exception e) {
            log.error("MinIO存储失败", e);
            success = false;
        }

        SysStorageEntity result = new SysStorageEntity();
        result.setStorageType(StorageTypeEnum.MINIO.getValue());
        result.setSuccess(success);
        result.setBasePath("");
        result.setBucket(finalBucket);
        result.setPath(path);
        result.setFileKey(fileKey);
        result.setSuffix(suffix);
        result.setOriginalName(fileOriginName);

        return result;
    }

    @Override
    public StorageGetResult get(String fileKey, SysStorageEntity sysStorage) {
        final String key = getKeyWithSuffix(fileKey, sysStorage.getSuffix());

        try {
            return new StorageGetResult(minioClient.getObject(minio.getBucket(), key), false);
        } catch (Exception e) {
            log.error("MinIO获取文件失败: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public void delete(String fileKey, SysStorageEntity sysStorage) {
        final String key = getKeyWithSuffix(fileKey, sysStorage.getSuffix());

        try {
            minioClient.removeObject(minio.getBucket(), key);
        } catch (Exception e) {
            log.error("MinIO删除文件失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public String getDownloadInfo(String fileKey, String fileName, SysStorageEntity sysStorage) {
        throw new UnsupportedOperationException("MinIO存储方式不支持获取下载链接");
    }

    @Override
    public int getPriority() {
        return minio.getPriority();
    }

    @Override
    public StorageTypeEnum getStorageType() {
        return StorageTypeEnum.MINIO;
    }
}
