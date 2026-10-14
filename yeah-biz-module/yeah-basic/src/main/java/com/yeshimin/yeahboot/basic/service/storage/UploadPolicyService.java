package com.yeshimin.yeahboot.basic.service.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.data.service.DynamicConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文件上传安全策略。
 */
@Service
@RequiredArgsConstructor
public class UploadPolicyService {

    /** MB 与字节的换算关系。 */
    private static final long MB = 1024L * 1024L;

    /** Office ZIP 容器最多检查的条目数量，避免异常压缩包消耗过多解析资源。 */
    private static final int OFFICE_ARCHIVE_MAX_ENTRIES = 128;

    /** Office ZIP 容器结构检查最多解压的内容量，单位：字节。 */
    private static final long OFFICE_ARCHIVE_MAX_SCAN_BYTES = 8L * MB;

    /** 提供上传策略动态参数的运行时配置服务。 */
    private final DynamicConfigService dynamicConfigService;

    /**
     * 校验上传文件大小，以及按系统参数启用的名称、MIME 类型和可识别文件头。
     */
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(ErrorCodeEnum.FAIL, "上传文件不能为空");
        }

        long maxSizeBytes = this.getMaxSizeBytes();
        if (file.getSize() > maxSizeBytes) {
            throw new BaseException(ErrorCodeEnum.FAIL, "上传文件超过大小限制");
        }

        if (!Boolean.TRUE.equals(dynamicConfigService.getBoolean(SysConfigEnum.ADMIN_STORAGE_UPLOAD_VALIDATION_ENABLED))) {
            return;
        }

        String originalName = file.getOriginalFilename();
        String extension = this.getExtension(originalName);
        if (StrUtil.isBlank(extension)) {
            throw new BaseException(ErrorCodeEnum.FAIL, "上传文件必须包含扩展名");
        }
        if (!this.getAllowedExtensions().contains(extension)) {
            throw new BaseException(ErrorCodeEnum.FAIL, "当前文件类型不允许上传");
        }

        String contentType = StrUtil.blankToDefault(file.getContentType(), "").toLowerCase(Locale.ROOT);
        if (StrUtil.isNotBlank(contentType)
                && !"application/octet-stream".equals(contentType)
                && !this.getAllowedMimeTypes().contains(contentType)) {
            throw new BaseException(ErrorCodeEnum.FAIL, "当前文件MIME类型不允许上传");
        }

        this.validateSignature(file, extension);
    }

    /**
     * 校验标记为公开预览的文件是否符合独立的预览类型白名单。
     */
    public void validatePublicPreview(MultipartFile file, boolean isPublic) {
        if (!isPublic) {
            return;
        }
        if (!Boolean.TRUE.equals(dynamicConfigService.getBoolean(SysConfigEnum.ADMIN_STORAGE_UPLOAD_PUBLIC_PREVIEW_ENABLED))) {
            throw new BaseException(ErrorCodeEnum.FAIL, "当前未开放公开文件上传");
        }
        if (!this.getPublicPreviewAllowedExtensions().contains(this.getExtension(file.getOriginalFilename()))) {
            throw new BaseException(ErrorCodeEnum.FAIL, "当前文件类型不允许公开预览");
        }
    }

    /**
     * 对支持识别的文件类型校验魔数，防止仅伪造扩展名绕过策略。
     */
    private void validateSignature(MultipartFile file, String extension) {
        byte[] header = this.readHeader(file);
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            this.requireSignature(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        } else if ("png".equals(extension)) {
            this.requireSignature(header, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        } else if ("webp".equals(extension)) {
            this.requireWebpSignature(header);
        } else if ("pdf".equals(extension)) {
            this.requireSignature(header, new byte[]{0x25, 0x50, 0x44, 0x46});
        } else if ("zip".equals(extension)) {
            this.requireSignature(header, new byte[]{0x50, 0x4B});
        } else if ("docx".equals(extension)) {
            this.requireSignature(header, new byte[]{0x50, 0x4B});
            this.validateOfficeArchive(file, "word/document.xml");
        } else if ("xlsx".equals(extension)) {
            this.requireSignature(header, new byte[]{0x50, 0x4B});
            this.validateOfficeArchive(file, "xl/workbook.xml");
        } else if ("doc".equals(extension) || "xls".equals(extension)) {
            this.requireSignature(header,
                    new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1});
        }
    }

    /**
     * 只读取文件头用于魔数校验，避免将整个上传文件加载进内存。
     */
    private byte[] readHeader(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[16];
            int offset = 0;
            while (offset < buffer.length) {
                int count = inputStream.read(buffer, offset, buffer.length - offset);
                if (count == -1) {
                    break;
                }
                offset += count;
            }
            return offset == buffer.length ? buffer : Arrays.copyOf(buffer, offset);
        } catch (IOException e) {
            throw new BaseException(ErrorCodeEnum.FAIL, "读取上传文件失败");
        }
    }

    /**
     * 校验 WebP 的 RIFF 和 WEBP 标记。
     */
    private void requireWebpSignature(byte[] header) {
        if (header.length < 12
                || header[0] != 'R' || header[1] != 'I' || header[2] != 'F' || header[3] != 'F'
                || header[8] != 'W' || header[9] != 'E' || header[10] != 'B' || header[11] != 'P') {
            throw new BaseException(ErrorCodeEnum.FAIL, "文件内容与扩展名不匹配");
        }
    }

    /**
     * 校验文件头是否以指定魔数开始。
     */
    private void requireSignature(byte[] header, byte[] expected) {
        if (header.length < expected.length) {
            throw new BaseException(ErrorCodeEnum.FAIL, "文件内容与扩展名不匹配");
        }
        for (int index = 0; index < expected.length; index++) {
            if (header[index] != expected[index]) {
                throw new BaseException(ErrorCodeEnum.FAIL, "文件内容与扩展名不匹配");
            }
        }
    }

    /**
     * 校验 Office Open XML 文档所需的 ZIP 条目，避免任意 ZIP 文件伪装为 docx 或 xlsx。
     */
    private void validateOfficeArchive(MultipartFile file, String mainDocumentEntry) {
        boolean hasContentTypes = false;
        boolean hasMainDocument = false;
        int entryCount = 0;
        long scannedBytes = 0;
        byte[] buffer = new byte[8192];

        try (InputStream inputStream = file.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (++entryCount > OFFICE_ARCHIVE_MAX_ENTRIES) {
                    throw new BaseException(ErrorCodeEnum.FAIL, "Office文档结构不合法");
                }

                String entryName = entry.getName();
                hasContentTypes = hasContentTypes || "[Content_Types].xml".equals(entryName);
                hasMainDocument = hasMainDocument || mainDocumentEntry.equals(entryName);

                // 逐项受限读取，既验证 ZIP 条目可解析，也避免在异常压缩包上无限解压。
                int count;
                while ((count = zipInputStream.read(buffer)) != -1) {
                    scannedBytes += count;
                    if (scannedBytes > OFFICE_ARCHIVE_MAX_SCAN_BYTES) {
                        throw new BaseException(ErrorCodeEnum.FAIL, "Office文档结构不合法");
                    }
                }
                if (hasContentTypes && hasMainDocument) {
                    return;
                }
            }
        } catch (BaseException e) {
            throw e;
        } catch (IOException e) {
            throw new BaseException(ErrorCodeEnum.FAIL, "读取上传文件失败");
        }

        throw new BaseException(ErrorCodeEnum.FAIL, "文件内容与扩展名不匹配");
    }

    /**
     * 读取当前动态配置允许的文件扩展名。
     */
    private Set<String> getAllowedExtensions() {
        return this.splitConfig(SysConfigEnum.ADMIN_STORAGE_UPLOAD_ALLOWED_EXTENSIONS);
    }

    /**
     * 读取当前动态配置允许的 MIME 类型。
     */
    private Set<String> getAllowedMimeTypes() {
        return this.splitConfig(SysConfigEnum.ADMIN_STORAGE_UPLOAD_ALLOWED_MIME_TYPES);
    }

    /**
     * 读取当前动态配置允许公开预览的文件扩展名。
     */
    private Set<String> getPublicPreviewAllowedExtensions() {
        return this.splitConfig(SysConfigEnum.ADMIN_STORAGE_UPLOAD_PUBLIC_PREVIEW_ALLOWED_EXTENSIONS);
    }

    /**
     * 将 MB 配置转换为字节数，过大的值按 Long 上限处理，避免乘法溢出。
     */
    private long getMaxSizeBytes() {
        long maxSizeMb = dynamicConfigService.getLong(SysConfigEnum.ADMIN_STORAGE_UPLOAD_MAX_SIZE_MB);
        return maxSizeMb > Long.MAX_VALUE / MB ? Long.MAX_VALUE : maxSizeMb * MB;
    }

    /**
     * 将逗号分隔的配置值标准化为小写集合。
     */
    private Set<String> splitConfig(SysConfigEnum config) {
        return Arrays.stream(dynamicConfigService.getString(config).split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 从原始文件名中取得小写扩展名。
     */
    private String getExtension(String originalName) {
        return FileUtil.getSuffix(StrUtil.blankToDefault(originalName, "")).toLowerCase(Locale.ROOT);
    }
}
