package com.yeshimin.yeahboot.basic.service.storage;

import com.yeshimin.yeahboot.common.common.enums.SysConfigEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.data.service.DynamicConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UploadPolicyServiceTest {

    @Mock
    private DynamicConfigService dynamicConfigService;

    private UploadPolicyService uploadPolicyService;

    @BeforeEach
    void setUp() {
        uploadPolicyService = new UploadPolicyService(dynamicConfigService);
    }

    @Test
    void acceptsOfficeDocumentWithRequiredEntries() throws IOException {
        this.stubDocxValidation();
        MockMultipartFile file = new MockMultipartFile("file", "example.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                this.createOfficeArchive("word/document.xml"));

        assertDoesNotThrow(() -> uploadPolicyService.validate(file));
    }

    @Test
    void rejectsZipFilePretendingToBeOfficeDocument() throws IOException {
        this.stubDocxValidation();
        MockMultipartFile file = new MockMultipartFile("file", "example.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                this.createOfficeArchive("other/document.xml"));

        assertThrows(BaseException.class, () -> uploadPolicyService.validate(file));
    }

    @Test
    void enforcesSizeLimitBeforeTypeValidation() {
        when(dynamicConfigService.getLong(SysConfigEnum.ADMIN_STORAGE_UPLOAD_MAX_SIZE_MB)).thenReturn(1L);
        MockMultipartFile file = new MockMultipartFile("file", "unrestricted.bin", "application/octet-stream",
                new byte[1024 * 1024 + 1]);

        assertThrows(BaseException.class, () -> uploadPolicyService.validate(file));
    }

    @Test
    void allowsUnsupportedExtensionWhenTypeValidationIsDisabled() {
        when(dynamicConfigService.getLong(SysConfigEnum.ADMIN_STORAGE_UPLOAD_MAX_SIZE_MB)).thenReturn(10L);
        when(dynamicConfigService.getBoolean(SysConfigEnum.ADMIN_STORAGE_UPLOAD_VALIDATION_ENABLED)).thenReturn(false);
        MockMultipartFile file = new MockMultipartFile("file", "unrestricted.bin", "application/octet-stream",
                new byte[]{1, 2, 3});

        assertDoesNotThrow(() -> uploadPolicyService.validate(file));
    }

    @Test
    void enforcesSeparatePublicPreviewWhitelist() {
        when(dynamicConfigService.getBoolean(SysConfigEnum.ADMIN_STORAGE_UPLOAD_PUBLIC_PREVIEW_ENABLED)).thenReturn(true);
        when(dynamicConfigService.getString(SysConfigEnum.ADMIN_STORAGE_UPLOAD_PUBLIC_PREVIEW_ALLOWED_EXTENSIONS))
                .thenReturn("png");
        MockMultipartFile file = new MockMultipartFile("file", "example.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[0]);

        assertThrows(BaseException.class, () -> uploadPolicyService.validatePublicPreview(file, true));
    }

    private byte[] createOfficeArchive(String mainDocumentEntry) throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
            this.writeEntry(zipOutputStream, "[Content_Types].xml", "<Types/>");
            this.writeEntry(zipOutputStream, mainDocumentEntry, "<document/>");
            zipOutputStream.finish();
            return output.toByteArray();
        }
    }

    private void writeEntry(ZipOutputStream zipOutputStream, String name, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        zipOutputStream.write(content.getBytes("UTF-8"));
        zipOutputStream.closeEntry();
    }

    private void stubDocxValidation() {
        when(dynamicConfigService.getLong(SysConfigEnum.ADMIN_STORAGE_UPLOAD_MAX_SIZE_MB)).thenReturn(10L);
        when(dynamicConfigService.getBoolean(SysConfigEnum.ADMIN_STORAGE_UPLOAD_VALIDATION_ENABLED)).thenReturn(true);
        when(dynamicConfigService.getString(SysConfigEnum.ADMIN_STORAGE_UPLOAD_ALLOWED_EXTENSIONS)).thenReturn("docx");
        when(dynamicConfigService.getString(SysConfigEnum.ADMIN_STORAGE_UPLOAD_ALLOWED_MIME_TYPES))
                .thenReturn("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }
}
