package com.yeshimin.yeahboot.upms.service;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelAnalysisException;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yeshimin.yeahboot.common.common.enums.DataStatusEnum;
import com.yeshimin.yeahboot.common.common.enums.GenderEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.common.properties.YeahBootProperties;
import com.yeshimin.yeahboot.common.service.PasswordService;
import com.yeshimin.yeahboot.data.domain.dto.SysUserQueryDto;
import com.yeshimin.yeahboot.data.domain.entity.SysUserEntity;
import com.yeshimin.yeahboot.data.repository.SysUserRepo;
import com.yeshimin.yeahboot.upms.domain.excel.SysUserExportExcelRow;
import com.yeshimin.yeahboot.upms.domain.excel.SysUserImportExcelRow;
import com.yeshimin.yeahboot.upms.domain.excel.SysUserImportInstructionExcelRow;
import com.yeshimin.yeahboot.upms.domain.vo.SysUserImportResultVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户Excel导入导出服务
 */
@Service
@RequiredArgsConstructor
public class SysUserExcelService {

    private static final long MAX_IMPORT_FILE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_IMPORT_ROWS = 1000;
    private static final int MAX_EXPORT_ROWS = 10000;
    private static final int MAX_ERROR_MESSAGES = 20;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<String> IMPORT_HEADERS = Arrays.asList(
            "用户名*", "初始密码", "状态（1启用/2禁用）", "昵称", "手机号", "邮箱", "性别（0未知/1男/2女）", "备注");

    private final SysUserRepo sysUserRepo;
    private final PasswordService passwordService;
    private final YeahBootProperties yeahBootProperties;

    /**
     * 生成用户导入模板
     */
    public byte[] createImportTemplate() {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ExcelWriter writer = EasyExcel.write(outputStream).autoCloseStream(false).build();
            try {
                WriteSheet importSheet = EasyExcel.writerSheet(0, "用户导入")
                        .head(SysUserImportExcelRow.class).build();
                writer.write(Collections.emptyList(), importSheet);

                WriteSheet instructionSheet = EasyExcel.writerSheet(1, "填写说明")
                        .head(SysUserImportInstructionExcelRow.class).build();
                writer.write(this.buildInstructions(), instructionSheet);
            } finally {
                writer.finish();
            }
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BaseException("生成用户导入模板失败");
        }
    }

    /**
     * 导入用户；全部数据校验通过后才写入数据库
     */
    @Transactional(rollbackFor = Exception.class)
    public SysUserImportResultVo importUsers(MultipartFile file) {
        this.validateImportFile(file);
        List<SysUserImportExcelRow> rows = this.readImportRows(file);
        this.validateImportRows(rows);

        List<SysUserEntity> entities = new ArrayList<>();
        int generatedPasswordCount = 0;
        for (SysUserImportExcelRow row : rows) {
            String initialPassword = row.getPassword();
            if (StrUtil.isBlank(initialPassword)) {
                initialPassword = passwordService.generateRandomPassword();
                generatedPasswordCount++;
            }

            SysUserEntity entity = new SysUserEntity();
            entity.setUsername(row.getUsername());
            entity.setPassword(passwordService.encodePassword(DigestUtil.sha256Hex(initialPassword)));
            entity.setStatus(row.getStatus());
            entity.setNickname(row.getNickname());
            entity.setMobile(row.getMobile());
            entity.setEmail(row.getEmail());
            entity.setGender(row.getGender());
            entity.setRemark(row.getRemark());
            entities.add(entity);
        }

        if (!sysUserRepo.saveBatch(entities)) {
            throw new BaseException("用户导入失败");
        }

        SysUserImportResultVo result = new SysUserImportResultVo();
        result.setImportedCount(entities.size());
        result.setGeneratedPasswordCount(generatedPasswordCount);
        return result;
    }

    /**
     * 导出用户基础信息，不导出密码、头像和关联数据
     */
    public byte[] exportUsers(SysUserQueryDto dto) {
        List<SysUserEntity> users = sysUserRepo.query(Page.of(1, MAX_EXPORT_ROWS), dto).getRecords();
        if (users.size() > MAX_EXPORT_ROWS) {
            throw new BaseException("单次最多导出" + MAX_EXPORT_ROWS + "条用户数据，请增加筛选条件后重试");
        }

        List<SysUserExportExcelRow> rows = users.stream().map(this::toExportRow).collect(Collectors.toList());
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            EasyExcel.write(outputStream, SysUserExportExcelRow.class)
                    .autoCloseStream(false)
                    .sheet("用户数据")
                    .doWrite(rows);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BaseException("导出用户数据失败");
        }
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException("请选择用户导入文件");
        }
        if (file.getSize() > MAX_IMPORT_FILE_SIZE) {
            throw new BaseException("用户导入文件不能超过5MB");
        }
        String fileName = file.getOriginalFilename();
        if (StrUtil.isBlank(fileName) || !StrUtil.endWithIgnoreCase(fileName, ".xlsx")) {
            throw new BaseException("仅支持.xlsx格式的用户导入文件");
        }
    }

    private List<SysUserImportExcelRow> readImportRows(MultipartFile file) {
        ImportListener listener = new ImportListener();
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, SysUserImportExcelRow.class, listener)
                    .autoCloseStream(false)
                    .sheet(0)
                    .headRowNumber(1)
                    .doRead();
            if (listener.getRows().isEmpty()) {
                throw new BaseException("用户导入文件没有可导入的数据");
            }
            return listener.getRows();
        } catch (BaseException e) {
            throw e;
        } catch (ExcelAnalysisException e) {
            if (e.getCause() instanceof BaseException) {
                throw (BaseException) e.getCause();
            }
            throw new BaseException("解析用户导入文件失败，请使用系统提供的模板");
        } catch (Exception e) {
            throw new BaseException("解析用户导入文件失败，请使用系统提供的模板");
        }
    }

    private void validateImportRows(List<SysUserImportExcelRow> rows) {
        List<String> errors = new ArrayList<>();
        Set<String> usernames = new HashSet<>();
        for (SysUserImportExcelRow row : rows) {
            this.normalizeImportRow(row);
            this.validateImportRow(row, usernames, errors);
        }

        Set<String> existingUsernames = sysUserRepo.findExistingUsernames(
                        rows.stream().map(SysUserImportExcelRow::getUsername)
                                .filter(StrUtil::isNotBlank).collect(Collectors.toSet()))
                .stream().map(username -> username.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        rows.stream()
                .filter(row -> StrUtil.isNotBlank(row.getUsername()))
                .filter(row -> existingUsernames.contains(row.getUsername().toLowerCase(Locale.ROOT)))
                .forEach(row -> errors.add("第" + row.getRowNumber() + "行：用户名已存在"));

        if (!errors.isEmpty()) {
            int errorCount = errors.size();
            String message = errors.stream().limit(MAX_ERROR_MESSAGES).collect(Collectors.joining("；"));
            if (errorCount > MAX_ERROR_MESSAGES) {
                message += "；另有" + (errorCount - MAX_ERROR_MESSAGES) + "条错误未展示";
            }
            throw new BaseException("用户导入校验失败：" + message);
        }
    }

    private void normalizeImportRow(SysUserImportExcelRow row) {
        row.setUsername(StrUtil.trim(row.getUsername()));
        row.setPassword(StrUtil.trim(row.getPassword()));
        row.setStatus(StrUtil.blankToDefault(StrUtil.trim(row.getStatus()), DataStatusEnum.ENABLED.getValue()));
        row.setNickname(StrUtil.trim(row.getNickname()));
        row.setMobile(StrUtil.trim(row.getMobile()));
        row.setEmail(StrUtil.trim(row.getEmail()));
        row.setGender(StrUtil.blankToDefault(StrUtil.trim(row.getGender()), GenderEnum.UNKNOWN.getValue()));
        row.setRemark(StrUtil.trim(row.getRemark()));
    }

    private void validateImportRow(SysUserImportExcelRow row, Set<String> usernames, List<String> errors) {
        String rowPrefix = "第" + row.getRowNumber() + "行：";
        if (StrUtil.isBlank(row.getUsername())) {
            errors.add(rowPrefix + "用户名不能为空");
        } else {
            if (row.getUsername().length() < 2 || row.getUsername().length() > 32) {
                errors.add(rowPrefix + "用户名长度必须在2到32个字符之间");
            }
            if (!usernames.add(row.getUsername().toLowerCase(Locale.ROOT))) {
                errors.add(rowPrefix + "用户名在导入文件中重复");
            }
        }
        if (StrUtil.isNotBlank(row.getPassword())
                && (row.getPassword().length() < 6 || row.getPassword().length() > 20)) {
            errors.add(rowPrefix + "初始密码长度必须在6到20个字符之间");
        }
        if (DataStatusEnum.of(row.getStatus()) == null) {
            errors.add(rowPrefix + "状态只能填写1或2");
        }
        if (StrUtil.length(row.getNickname()) > 32) {
            errors.add(rowPrefix + "昵称不能超过32个字符");
        }
        if (StrUtil.isNotBlank(row.getMobile()) && !Validator.isMobile(row.getMobile())) {
            errors.add(rowPrefix + "手机号格式不正确");
        }
        if (StrUtil.length(row.getMobile()) > 16) {
            errors.add(rowPrefix + "手机号不能超过16个字符");
        }
        if (StrUtil.isNotBlank(row.getEmail()) && !Validator.isEmail(row.getEmail())) {
            errors.add(rowPrefix + "邮箱格式不正确");
        }
        if (StrUtil.length(row.getEmail()) > 64) {
            errors.add(rowPrefix + "邮箱不能超过64个字符");
        }
        if (GenderEnum.of(row.getGender()) == null) {
            errors.add(rowPrefix + "性别只能填写0、1或2");
        }
        if (DataStatusEnum.DISABLED.equalsValue(row.getStatus())
                && Objects.equals(yeahBootProperties.getSuperAdmin(), row.getUsername())) {
            errors.add(rowPrefix + "超级管理员不能禁用");
        }
        if (StrUtil.length(row.getRemark()) > 255) {
            errors.add(rowPrefix + "备注不能超过255个字符");
        }
    }

    private List<SysUserImportInstructionExcelRow> buildInstructions() {
        return Arrays.asList(
                new SysUserImportInstructionExcelRow("用户名*", "必填，2到32个字符，不能与已有用户名重复"),
                new SysUserImportInstructionExcelRow("初始密码", "选填，6到20个字符；为空时系统自动生成随机密码"),
                new SysUserImportInstructionExcelRow("状态", "选填：1-启用，2-禁用；为空默认1"),
                new SysUserImportInstructionExcelRow("昵称", "选填，最多32个字符"),
                new SysUserImportInstructionExcelRow("手机号", "选填，填写时校验手机号格式"),
                new SysUserImportInstructionExcelRow("邮箱", "选填，填写时校验邮箱格式"),
                new SysUserImportInstructionExcelRow("性别", "选填：0-未知，1-男，2-女；为空默认0"),
                new SysUserImportInstructionExcelRow("备注", "选填，最多255个字符"),
                new SysUserImportInstructionExcelRow("关联数据", "本版本暂不导入组织、岗位和角色关系"));
    }

    private SysUserExportExcelRow toExportRow(SysUserEntity user) {
        return new SysUserExportExcelRow(
                user.getId() == null ? "" : String.valueOf(user.getId()),
                StrUtil.nullToEmpty(user.getUsername()),
                this.getStatusLabel(user.getStatus()),
                StrUtil.nullToEmpty(user.getNickname()),
                StrUtil.nullToEmpty(user.getMobile()),
                StrUtil.nullToEmpty(user.getEmail()),
                this.getGenderLabel(user.getGender()),
                StrUtil.nullToEmpty(user.getRemark()),
                user.getCreateTime() == null ? "" : DATE_TIME_FORMATTER.format(user.getCreateTime()));
    }

    private String getStatusLabel(String status) {
        DataStatusEnum statusEnum = DataStatusEnum.of(status);
        return statusEnum == null ? StrUtil.nullToEmpty(status) : statusEnum.getDesc();
    }

    private String getGenderLabel(String gender) {
        GenderEnum genderEnum = GenderEnum.of(gender);
        return genderEnum == null ? StrUtil.nullToEmpty(gender) : genderEnum.getDesc();
    }

    private static class ImportListener extends AnalysisEventListener<SysUserImportExcelRow> {

        private final List<SysUserImportExcelRow> rows = new ArrayList<>();

        @Override
        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
            for (int i = 0; i < IMPORT_HEADERS.size(); i++) {
                if (!IMPORT_HEADERS.get(i).equals(StrUtil.trim(headMap.get(i)))) {
                    throw new BaseException("用户导入文件表头不正确，请使用系统提供的模板");
                }
            }
        }

        @Override
        public void invoke(SysUserImportExcelRow data, AnalysisContext context) {
            if (rows.size() >= MAX_IMPORT_ROWS) {
                throw new BaseException("单次最多导入" + MAX_IMPORT_ROWS + "条用户数据");
            }
            data.setRowNumber(context.readRowHolder().getRowIndex() + 1);
            rows.add(data);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
        }

        public List<SysUserImportExcelRow> getRows() {
            return rows;
        }
    }
}
