package com.yeshimin.yeahboot.basic.controller;

import com.yeshimin.yeahboot.data.domain.entity.SysFileEntity;
import com.yeshimin.yeahboot.basic.domain.dto.FileDeleteDto;
import com.yeshimin.yeahboot.common.common.enums.StorageTypeEnum;
import com.yeshimin.yeahboot.basic.domain.vo.FileUploadVo;
import com.yeshimin.yeahboot.data.mapper.SysFileMapper;
import com.yeshimin.yeahboot.data.repository.SysFileRepo;
import com.yeshimin.yeahboot.basic.service.storage.FileService;
import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.controller.base.CrudController;
import com.yeshimin.yeahboot.common.domain.base.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

/**
 * 文件管理 - 底层依赖storage
 */
@RestController
@RequestMapping("/basic/file")
public class FileController extends CrudController<SysFileMapper, SysFileEntity, SysFileRepo> {

    @Autowired
    private FileService fileService;

    public FileController(SysFileRepo service) {
        // 由于lombok方案无法实现构造方法中调用super，只能显式调用
        super(service);
        super.setModule("basic:file").disableCreate().disableUpdate().disableDelete();
    }

    /**
     * 上传文件
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':upload')")
    @PostMapping("/upload")
    public R<FileUploadVo> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam(required = false) Integer storageType) {
        // 检查参数：【存储类型】
        StorageTypeEnum storageTypeEnum = null;
        if (storageType != null) {
            storageTypeEnum = StorageTypeEnum.of(storageType);
            if (storageTypeEnum == null) {
                throw new BaseException(ErrorCodeEnum.FAIL, "不支持的存储类型");
            }
        }
        FileUploadVo vo = fileService.upload(file, storageTypeEnum);
        return R.ok(vo);
    }

    /**
     * 下载文件
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':download')")
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> download(@RequestParam("fileKey") String fileKey) {
        return fileService.download(fileKey);
    }

    /**
     * 删除文件
     */
    @PreAuthorize("@pms.hasPermission(this.getModule() + ':delete')")
    @PostMapping("/delete")
    public R<Void> delete(@Valid @RequestBody FileDeleteDto dto) {
        fileService.delete(dto);
        return R.ok();
    }
}
