-- =============================================================================
-- 系统参数：数据表、初始数据、资源、挂载关系和管理员授权
-- 执行前请填写短信签名和短信模板编号；敏感AccessKey继续保留在application.yml中
-- =============================================================================

SET @now := NOW();
SET @max_time := '9999-12-31 23:59:59';
SET @operator := 'system';
SET @grant_all_role_code := 'admin';
SET @sms_sign_name := '';
SET @sms_template_code := '';

-- =============================================================================
-- 1. 系统参数表
-- =============================================================================

CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `delete_time` DATETIME NOT NULL DEFAULT '9999-12-31 23:59:59' COMMENT '删除时间',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：1-是 0-否',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `create_by` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '创建者',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `update_by` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '更新者',
  `group_code` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '参数分组编码',
  `config_key` VARCHAR(128) NOT NULL COMMENT '参数键',
  `config_name` VARCHAR(64) NOT NULL COMMENT '参数名称',
  `config_value` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '参数值',
  `value_type` TINYINT NOT NULL DEFAULT 1 COMMENT '值类型：1-字符串 2-整数 3-长整数 4-布尔值',
  `status` VARCHAR(8) NOT NULL DEFAULT '1' COMMENT '状态：1-启用 2-禁用',
  `sort` INT NOT NULL DEFAULT 1 COMMENT '排序：自然数',
  `remark` VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_config_key_delete_time` (`config_key`, `delete_time`),
  KEY `idx_sys_config_group_status` (`group_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数表';

START TRANSACTION;

-- =============================================================================
-- 2. 初始动态参数；重复执行不会覆盖已经调整过的参数值
-- =============================================================================

INSERT IGNORE INTO `sys_config` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `group_code`, `config_key`, `config_name`, `config_value`, `value_type`, `status`, `sort`, `remark`
) VALUES
(@max_time, 0, @now, @operator, @now, @operator,
 'auth', 'yeah-boot.captcha-enabled', '登录验证码', 'true', 4, '1', 10, '是否启用管理后台登录验证码'),
(@max_time, 0, @now, @operator, @now, @operator,
 'auth', 'auth.admin-login.failure-window-seconds', '登录失败统计窗口', '600', 3, '1', 20,
 '统计登录失败次数的时间窗口，单位：秒'),
(@max_time, 0, @now, @operator, @now, @operator,
 'auth', 'auth.admin-login.max-failure-count', '最大登录失败次数', '5', 3, '1', 30,
 '统计窗口内达到该次数后临时锁定账号'),
(@max_time, 0, @now, @operator, @now, @operator,
 'auth', 'auth.admin-login.lock-seconds', '登录锁定时间', '600', 3, '1', 40,
 '登录失败超限后的锁定时间，单位：秒'),
(@max_time, 0, @now, @operator, @now, @operator,
 'sms', 'yeah-boot.sms-code-length', '短信验证码长度', '6', 2, '1', 10,
 '短信验证码位数，允许4到8'),
(@max_time, 0, @now, @operator, @now, @operator,
 'sms', 'yeah-boot.sms-code-exp-seconds', '短信验证码有效期', '300', 2, '1', 20,
 '短信验证码有效期，单位：秒'),
(@max_time, 0, @now, @operator, @now, @operator,
 'sms', 'yeah-boot.notification.aliyun.sms.sign-name', '短信签名', @sms_sign_name, 1,
 IF(@sms_sign_name = '', '2', '1'), 30,
 '阿里云短信签名；不属于密钥，可动态调整'),
(@max_time, 0, @now, @operator, @now, @operator,
 'sms', 'yeah-boot.notification.aliyun.sms.template-code', '短信模板编号', @sms_template_code, 1,
 IF(@sms_template_code = '', '2', '1'), 40,
 '阿里云短信模板编号；不属于密钥，可动态调整'),
(@max_time, 0, @now, @operator, @now, @operator,
 'excel', 'yeah-boot.sys-user-excel.max-import-file-size-mb', '用户导入文件上限', '5', 3, '1', 10,
 '用户导入文件最大大小，单位：MB'),
(@max_time, 0, @now, @operator, @now, @operator,
 'excel', 'yeah-boot.sys-user-excel.max-import-rows', '用户最大导入条数', '1000', 2, '1', 20,
 '单次允许导入的最大用户数量'),
(@max_time, 0, @now, @operator, @now, @operator,
 'excel', 'yeah-boot.sys-user-excel.max-export-rows', '用户最大导出条数', '10000', 2, '1', 30,
 '单次允许导出的最大用户数量'),
(@max_time, 0, @now, @operator, @now, @operator,
 'excel', 'yeah-boot.sys-user-excel.max-error-messages', '导入错误展示数量', '20', 2, '1', 40,
 '用户导入失败时最多展示的错误数量');

-- =============================================================================
-- 3. 视图资源
-- =============================================================================

SET @system_menu_id := (
  SELECT `id` FROM `sys_res`
  WHERE `deleted` = 0 AND `type` = 1 AND `path` = '/system'
  ORDER BY `id` LIMIT 1
);

INSERT INTO `sys_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `type`, `parent_id`, `group_id`, `name`, `permission`, `path`, `component`, `icon`,
  `is_link`, `link_url`, `status`, `visible`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       2, @system_menu_id, 0, '系统参数', '', '/system/config', 'system/config/index', 'Tools',
       0, '', '1', 1, 80, '运行时动态系统参数管理'
WHERE @system_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_res`
    WHERE `deleted` = 0 AND `type` = 2 AND `path` = '/system/config'
  );

SET @config_page_id := (
  SELECT `id` FROM `sys_res`
  WHERE `deleted` = 0 AND `type` = 2 AND `path` = '/system/config'
  ORDER BY `id` LIMIT 1
);

INSERT INTO `sys_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `type`, `parent_id`, `group_id`, `name`, `permission`, `path`, `component`, `icon`,
  `is_link`, `link_url`, `status`, `visible`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       3, @config_page_id, 0, '新增参数', 'view:admin:sysConfig:create', '', '', '',
       0, '', '1', 1, 10, '控制新增系统参数按钮'
WHERE @config_page_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_res`
    WHERE `deleted` = 0 AND `permission` = 'view:admin:sysConfig:create'
  );

INSERT INTO `sys_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `type`, `parent_id`, `group_id`, `name`, `permission`, `path`, `component`, `icon`,
  `is_link`, `link_url`, `status`, `visible`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       3, @config_page_id, 0, '编辑参数', 'view:admin:sysConfig:update', '', '', '',
       0, '', '1', 1, 20, '控制编辑和启禁系统参数'
WHERE @config_page_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_res`
    WHERE `deleted` = 0 AND `permission` = 'view:admin:sysConfig:update'
  );

INSERT INTO `sys_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `type`, `parent_id`, `group_id`, `name`, `permission`, `path`, `component`, `icon`,
  `is_link`, `link_url`, `status`, `visible`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       3, @config_page_id, 0, '删除参数', 'view:admin:sysConfig:delete', '', '', '',
       0, '', '1', 1, 30, '控制单个和批量删除系统参数'
WHERE @config_page_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_res`
    WHERE `deleted` = 0 AND `permission` = 'view:admin:sysConfig:delete'
  );

INSERT INTO `sys_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `type`, `parent_id`, `group_id`, `name`, `permission`, `path`, `component`, `icon`,
  `is_link`, `link_url`, `status`, `visible`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       3, @config_page_id, 0, '刷新缓存', 'view:admin:sysConfig:refreshCache', '', '', '',
       0, '', '1', 1, 40, '控制手动刷新系统参数缓存按钮'
WHERE @config_page_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_res`
    WHERE `deleted` = 0 AND `permission` = 'view:admin:sysConfig:refreshCache'
  );

-- =============================================================================
-- 4. 接口资源分组和接口资源
-- =============================================================================

SET @system_api_group_id := (
  SELECT `id` FROM `sys_res_group`
  WHERE `deleted` = 0 AND `parent_id` = 0 AND `name` = '系统管理接口'
  ORDER BY `id` LIMIT 1
);

INSERT INTO `sys_res_group` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `parent_id`, `name`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       0, '系统管理接口', 10, '系统管理接口资源分组'
WHERE @system_api_group_id IS NULL;

SET @system_api_group_id := (
  SELECT `id` FROM `sys_res_group`
  WHERE `deleted` = 0 AND `parent_id` = 0 AND `name` = '系统管理接口'
  ORDER BY `id` LIMIT 1
);

INSERT INTO `sys_res_group` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `parent_id`, `name`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       @system_api_group_id, '系统参数接口', 80, '系统参数管理接口'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_res_group`
  WHERE `deleted` = 0
    AND `parent_id` = @system_api_group_id
    AND `name` = '系统参数接口'
);

SET @config_api_group_id := (
  SELECT `id` FROM `sys_res_group`
  WHERE `deleted` = 0
    AND `parent_id` = @system_api_group_id
    AND `name` = '系统参数接口'
  ORDER BY `id` LIMIT 1
);

INSERT INTO `sys_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `type`, `parent_id`, `group_id`, `name`, `permission`, `path`, `component`, `icon`,
  `is_link`, `link_url`, `status`, `visible`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       4, 0, @config_api_group_id, '参数列表', 'api:admin:sysConfig:crud:query', '', '', '',
       0, '', '1', 1, 10, 'GET /admin/sysConfig/crud/query'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'api:admin:sysConfig:crud:query'
);

INSERT INTO `sys_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `type`, `parent_id`, `group_id`, `name`, `permission`, `path`, `component`, `icon`,
  `is_link`, `link_url`, `status`, `visible`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       4, 0, @config_api_group_id, '参数详情', 'api:admin:sysConfig:crud:detail', '', '', '',
       0, '', '1', 1, 20, 'GET /admin/sysConfig/crud/detail'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'api:admin:sysConfig:crud:detail'
);

INSERT INTO `sys_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `type`, `parent_id`, `group_id`, `name`, `permission`, `path`, `component`, `icon`,
  `is_link`, `link_url`, `status`, `visible`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       4, 0, @config_api_group_id, '新增参数接口', 'api:admin:sysConfig:create', '', '', '',
       0, '', '1', 1, 30, 'POST /admin/sysConfig/create'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'api:admin:sysConfig:create'
);

INSERT INTO `sys_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `type`, `parent_id`, `group_id`, `name`, `permission`, `path`, `component`, `icon`,
  `is_link`, `link_url`, `status`, `visible`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       4, 0, @config_api_group_id, '编辑参数接口', 'api:admin:sysConfig:update', '', '', '',
       0, '', '1', 1, 40, 'POST /admin/sysConfig/update'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'api:admin:sysConfig:update'
);

INSERT INTO `sys_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `type`, `parent_id`, `group_id`, `name`, `permission`, `path`, `component`, `icon`,
  `is_link`, `link_url`, `status`, `visible`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       4, 0, @config_api_group_id, '删除参数接口', 'api:admin:sysConfig:delete', '', '', '',
       0, '', '1', 1, 50, 'POST /admin/sysConfig/delete'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'api:admin:sysConfig:delete'
);

INSERT INTO `sys_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `type`, `parent_id`, `group_id`, `name`, `permission`, `path`, `component`, `icon`,
  `is_link`, `link_url`, `status`, `visible`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       4, 0, @config_api_group_id, '刷新缓存接口', 'api:admin:sysConfig:refreshCache', '', '', '',
       0, '', '1', 1, 60, 'POST /admin/sysConfig/refreshCache'
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'api:admin:sysConfig:refreshCache'
);

-- =============================================================================
-- 5. 视图与接口挂载
-- =============================================================================

SET @config_create_view_id := (
  SELECT `id` FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'view:admin:sysConfig:create'
  ORDER BY `id` LIMIT 1
);
SET @config_update_view_id := (
  SELECT `id` FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'view:admin:sysConfig:update'
  ORDER BY `id` LIMIT 1
);
SET @config_delete_view_id := (
  SELECT `id` FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'view:admin:sysConfig:delete'
  ORDER BY `id` LIMIT 1
);
SET @config_refresh_view_id := (
  SELECT `id` FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'view:admin:sysConfig:refreshCache'
  ORDER BY `id` LIMIT 1
);

SET @config_query_api_id := (
  SELECT `id` FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'api:admin:sysConfig:crud:query'
  ORDER BY `id` LIMIT 1
);
SET @config_detail_api_id := (
  SELECT `id` FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'api:admin:sysConfig:crud:detail'
  ORDER BY `id` LIMIT 1
);
SET @config_create_api_id := (
  SELECT `id` FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'api:admin:sysConfig:create'
  ORDER BY `id` LIMIT 1
);
SET @config_update_api_id := (
  SELECT `id` FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'api:admin:sysConfig:update'
  ORDER BY `id` LIMIT 1
);
SET @config_delete_api_id := (
  SELECT `id` FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'api:admin:sysConfig:delete'
  ORDER BY `id` LIMIT 1
);
SET @config_refresh_api_id := (
  SELECT `id` FROM `sys_res`
  WHERE `deleted` = 0 AND `permission` = 'api:admin:sysConfig:refreshCache'
  ORDER BY `id` LIMIT 1
);

INSERT INTO `sys_res_mount` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `view_res_id`, `api_res_id`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       @config_page_id, @config_query_api_id, 10, '进入页面后查询参数列表'
WHERE @config_page_id IS NOT NULL AND @config_query_api_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_res_mount`
    WHERE `deleted` = 0 AND `view_res_id` = @config_page_id AND `api_res_id` = @config_query_api_id
  );

INSERT INTO `sys_res_mount` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `view_res_id`, `api_res_id`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       @config_create_view_id, @config_create_api_id, 10, '新增参数'
WHERE @config_create_view_id IS NOT NULL AND @config_create_api_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_res_mount`
    WHERE `deleted` = 0 AND `view_res_id` = @config_create_view_id AND `api_res_id` = @config_create_api_id
  );

INSERT INTO `sys_res_mount` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `view_res_id`, `api_res_id`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       @config_update_view_id, @config_detail_api_id, 10, '编辑前查询参数详情'
WHERE @config_update_view_id IS NOT NULL AND @config_detail_api_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_res_mount`
    WHERE `deleted` = 0 AND `view_res_id` = @config_update_view_id AND `api_res_id` = @config_detail_api_id
  );

INSERT INTO `sys_res_mount` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `view_res_id`, `api_res_id`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       @config_update_view_id, @config_update_api_id, 20, '编辑或启禁参数'
WHERE @config_update_view_id IS NOT NULL AND @config_update_api_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_res_mount`
    WHERE `deleted` = 0 AND `view_res_id` = @config_update_view_id AND `api_res_id` = @config_update_api_id
  );

INSERT INTO `sys_res_mount` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `view_res_id`, `api_res_id`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       @config_delete_view_id, @config_delete_api_id, 10, '删除参数'
WHERE @config_delete_view_id IS NOT NULL AND @config_delete_api_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_res_mount`
    WHERE `deleted` = 0 AND `view_res_id` = @config_delete_view_id AND `api_res_id` = @config_delete_api_id
  );

INSERT INTO `sys_res_mount` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `view_res_id`, `api_res_id`, `sort`, `remark`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       @config_refresh_view_id, @config_refresh_api_id, 10, '手动刷新参数缓存'
WHERE @config_refresh_view_id IS NOT NULL AND @config_refresh_api_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_res_mount`
    WHERE `deleted` = 0 AND `view_res_id` = @config_refresh_view_id AND `api_res_id` = @config_refresh_api_id
  );

-- =============================================================================
-- 6. 将新增视图资源及其挂载接口授权给指定角色
-- =============================================================================

SET @grant_all_role_id := (
  SELECT `id` FROM `sys_role`
  WHERE `deleted` = 0 AND `code` = @grant_all_role_code
  ORDER BY `id` LIMIT 1
);

INSERT INTO `sys_role_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `role_id`, `res_id`, `mount_id`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       @grant_all_role_id, resource_ids.`res_id`, 0
FROM (
  SELECT @config_page_id AS `res_id`
  UNION ALL SELECT @config_create_view_id
  UNION ALL SELECT @config_update_view_id
  UNION ALL SELECT @config_delete_view_id
  UNION ALL SELECT @config_refresh_view_id
) resource_ids
WHERE @grant_all_role_id IS NOT NULL AND resource_ids.`res_id` IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_res` rr
    WHERE rr.`deleted` = 0
      AND rr.`role_id` = @grant_all_role_id
      AND rr.`res_id` = resource_ids.`res_id`
      AND rr.`mount_id` = 0
  );

INSERT INTO `sys_role_res` (
  `delete_time`, `deleted`, `create_time`, `create_by`, `update_time`, `update_by`,
  `role_id`, `res_id`, `mount_id`
)
SELECT @max_time, 0, @now, @operator, @now, @operator,
       @grant_all_role_id, mount.`api_res_id`, mount.`id`
FROM `sys_res_mount` mount
WHERE @grant_all_role_id IS NOT NULL
  AND mount.`deleted` = 0
  AND mount.`view_res_id` IN (
    @config_page_id,
    @config_create_view_id,
    @config_update_view_id,
    @config_delete_view_id,
    @config_refresh_view_id
  )
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_res` rr
    WHERE rr.`deleted` = 0
      AND rr.`role_id` = @grant_all_role_id
      AND rr.`res_id` = mount.`api_res_id`
      AND rr.`mount_id` = mount.`id`
  );

COMMIT;

-- 执行完成后重启应用，或在“系统参数”页面点击“刷新缓存”。
