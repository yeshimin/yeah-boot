package com.yeshimin.yeahboot.pub;

import com.yeshimin.yeahboot.auth.common.config.security.PublicAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 存储管理
 */
@RestController
@RequestMapping("/public/storage")
public class PublicStorageController {

    @Autowired
    private PublicStorageService storageService;

    /**
     * 公开下载
     */
    @PublicAccess
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> publicDownload(@RequestParam("fileKey") String fileKey) {
        return storageService.download(fileKey, true);
    }

    /**
     * 公开下载-预览（一般是图片）
     */
    @PublicAccess
    @GetMapping("/preview")
    public ResponseEntity<InputStreamResource> preview(@RequestParam("fileKey") String fileKey) {
        return storageService.preview(fileKey, true);
    }
}
