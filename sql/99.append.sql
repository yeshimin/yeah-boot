-- 已初始化数据库的累计增量结构补丁。
--
-- 新建数据库请先按 README 的顺序导入 00 至 03 初始化包，再执行本文件。
-- 后续升级时，按时间顺序执行尚未应用的版本段落；每个段落仅执行一次。

-- 2026-10-23：扩展系统日志额外信息字段长度。
ALTER TABLE `sys_log`
    CHANGE COLUMN `extra` `extra` VARCHAR(512) NULL DEFAULT NULL COMMENT '额外信息（json字符串）';
