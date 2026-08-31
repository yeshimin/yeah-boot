-- 资源权限分层管理改造
-- 执行前请先切换到目标库，例如：USE yeah_boot;
-- 说明：
-- 1. sys_res 仍然是最终鉴权资源表。
-- 2. sys_res_group 仅用于接口资源分组管理，不参与鉴权。
-- 3. sys_res_mount 仅用于视图资源挂载接口资源，不参与鉴权。
-- 4. 脚本可重复执行；不会重复新增字段、权限或挂载关系。

SET @now := NOW();
SET @max_time := '9999-12-31 23:59:59';
SET @operator := 'system';
SET @grant_all_role_code := 'admin';
SET @schema_name := DATABASE();

-- =============================================================================
-- 表结构
-- =============================================================================

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_res ADD COLUMN group_id BIGINT NOT NULL DEFAULT 0 COMMENT ''接口资源分组ID，仅接口资源使用'' AFTER parent_id',
        'SELECT ''sys_res.group_id already exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_res'
      AND COLUMN_NAME = 'group_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl := (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE sys_role_res ADD COLUMN mount_id BIGINT NOT NULL DEFAULT 0 COMMENT ''挂载ID；0表示非挂载授权'' AFTER res_id',
        'SELECT ''sys_role_res.mount_id already exists'''
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name
      AND TABLE_NAME = 'sys_role_res'
      AND COLUMN_NAME = 'mount_id'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS sys_res_group (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    delete_time DATETIME NOT NULL DEFAULT '9999-12-31 23:59:59' COMMENT '删除时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：1-是 0-否',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父分组ID',
    name VARCHAR(64) NOT NULL COMMENT '分组名称',
    sort INT NOT NULL DEFAULT 1 COMMENT '排序',
    remark VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE = InnoDB COMMENT = '接口资源分组表';

CREATE TABLE IF NOT EXISTS sys_res_mount (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    delete_time DATETIME NOT NULL DEFAULT '9999-12-31 23:59:59' COMMENT '删除时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：1-是 0-否',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '创建者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    update_by VARCHAR(64) NOT NULL DEFAULT '' COMMENT '更新者',
    view_res_id BIGINT NOT NULL COMMENT '视图/分组资源ID',
    api_res_id BIGINT NOT NULL COMMENT '接口资源ID',
    sort INT NOT NULL DEFAULT 1 COMMENT '排序',
    remark VARCHAR(255) NOT NULL DEFAULT '' COMMENT '备注',
    PRIMARY KEY (id),
    KEY idx_view_res_id (view_res_id),
    KEY idx_api_res_id (api_res_id)
) ENGINE = InnoDB COMMENT = '资源接口挂载表';

-- =============================================================================
-- 历史数据迁移：原本挂在 sys_res.parent_id 下的接口资源，迁移到 sys_res_mount
-- =============================================================================

INSERT INTO sys_res_mount (
    delete_time,
    deleted,
    create_time,
    create_by,
    update_time,
    update_by,
    view_res_id,
    api_res_id,
    sort,
    remark
)
SELECT
    @max_time,
    0,
    @now,
    @operator,
    @now,
    @operator,
    r.parent_id,
    r.id,
    COALESCE(r.sort, 1),
    '由历史接口父级关系迁移'
FROM sys_res r
WHERE r.deleted = 0
  AND r.type = 4
  AND COALESCE(r.parent_id, 0) > 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_res_mount m
      WHERE m.deleted = 0
        AND m.view_res_id = r.parent_id
        AND m.api_res_id = r.id
  );

UPDATE sys_res
SET parent_id = 0,
    group_id = 0,
    update_time = @now,
    update_by = @operator
WHERE deleted = 0
  AND type = 4
  AND COALESCE(parent_id, 0) <> 0;

-- =============================================================================
-- 新增资源管理相关接口权限
-- =============================================================================

SET @resource_page_id := (
    SELECT id
    FROM sys_res
    WHERE deleted = 0
      AND type = 2
      AND path = '/system/resource'
    ORDER BY id
    LIMIT 1
);
SET @resource_page_id := COALESCE(@resource_page_id, 103);

INSERT INTO sys_res (
    delete_time,
    deleted,
    create_time,
    create_by,
    update_time,
    update_by,
    type,
    parent_id,
    group_id,
    name,
    permission,
    path,
    component,
    icon,
    is_link,
    link_url,
    status,
    visible,
    sort,
    remark
)
SELECT @max_time, 0, @now, @operator, @now, @operator, 4, @resource_page_id, 0,
       '接口分组树', 'admin:sysResGroup:tree', '', '', '', 0, '', '1', 1, 80,
       'GET /admin/sysResGroup/tree'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_res WHERE deleted = 0 AND permission = 'admin:sysResGroup:tree'
);

INSERT INTO sys_res (
    delete_time,
    deleted,
    create_time,
    create_by,
    update_time,
    update_by,
    type,
    parent_id,
    group_id,
    name,
    permission,
    path,
    component,
    icon,
    is_link,
    link_url,
    status,
    visible,
    sort,
    remark
)
SELECT @max_time, 0, @now, @operator, @now, @operator, 3, @resource_page_id, 0,
       '新增接口分组', 'admin:sysResGroup:create', '', '', '', 0, '', '1', 1, 90,
       'POST /admin/sysResGroup/create'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_res WHERE deleted = 0 AND permission = 'admin:sysResGroup:create'
);

INSERT INTO sys_res (
    delete_time,
    deleted,
    create_time,
    create_by,
    update_time,
    update_by,
    type,
    parent_id,
    group_id,
    name,
    permission,
    path,
    component,
    icon,
    is_link,
    link_url,
    status,
    visible,
    sort,
    remark
)
SELECT @max_time, 0, @now, @operator, @now, @operator, 3, @resource_page_id, 0,
       '编辑接口分组', 'admin:sysResGroup:update', '', '', '', 0, '', '1', 1, 100,
       'POST /admin/sysResGroup/update'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_res WHERE deleted = 0 AND permission = 'admin:sysResGroup:update'
);

INSERT INTO sys_res (
    delete_time,
    deleted,
    create_time,
    create_by,
    update_time,
    update_by,
    type,
    parent_id,
    group_id,
    name,
    permission,
    path,
    component,
    icon,
    is_link,
    link_url,
    status,
    visible,
    sort,
    remark
)
SELECT @max_time, 0, @now, @operator, @now, @operator, 3, @resource_page_id, 0,
       '删除接口分组', 'admin:sysResGroup:delete', '', '', '', 0, '', '1', 1, 110,
       'POST /admin/sysResGroup/delete'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_res WHERE deleted = 0 AND permission = 'admin:sysResGroup:delete'
);

INSERT INTO sys_res (
    delete_time,
    deleted,
    create_time,
    create_by,
    update_time,
    update_by,
    type,
    parent_id,
    group_id,
    name,
    permission,
    path,
    component,
    icon,
    is_link,
    link_url,
    status,
    visible,
    sort,
    remark
)
SELECT @max_time, 0, @now, @operator, @now, @operator, 4, @resource_page_id, 0,
       '查询资源挂载接口', 'admin:sysResMount:query', '', '', '', 0, '', '1', 1, 120,
       'GET /admin/sysResMount/queryByViewResId'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_res WHERE deleted = 0 AND permission = 'admin:sysResMount:query'
);

INSERT INTO sys_res (
    delete_time,
    deleted,
    create_time,
    create_by,
    update_time,
    update_by,
    type,
    parent_id,
    group_id,
    name,
    permission,
    path,
    component,
    icon,
    is_link,
    link_url,
    status,
    visible,
    sort,
    remark
)
SELECT @max_time, 0, @now, @operator, @now, @operator, 3, @resource_page_id, 0,
       '保存资源挂载接口', 'admin:sysResMount:save', '', '', '', 0, '', '1', 1, 130,
       'POST /admin/sysResMount/saveByViewResId'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_res WHERE deleted = 0 AND permission = 'admin:sysResMount:save'
);

-- 授权给 admin 角色，避免新增接口上线后管理员看不到按钮或调用 403。
INSERT INTO sys_role_res (
    delete_time,
    deleted,
    create_time,
    create_by,
    update_time,
    update_by,
    role_id,
    res_id,
    mount_id
)
SELECT
    @max_time,
    0,
    @now,
    @operator,
    @now,
    @operator,
    role.id,
    res.id,
    0
FROM sys_role role
JOIN sys_res res
  ON res.deleted = 0
 AND res.permission IN (
     'admin:sysResGroup:tree',
     'admin:sysResGroup:create',
     'admin:sysResGroup:update',
     'admin:sysResGroup:delete',
     'admin:sysResMount:query',
     'admin:sysResMount:save'
 )
WHERE role.deleted = 0
  AND role.code = @grant_all_role_code
   AND NOT EXISTS (
       SELECT 1
       FROM sys_role_res rr
       WHERE rr.deleted = 0
         AND rr.role_id = role.id
         AND rr.res_id = res.id
         AND rr.mount_id = 0
   );

