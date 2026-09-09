-- 资源权限彻底重建脚本（TRUNCATE 版）
-- 执行前请先切换到目标库，例如：USE yeah_boot;
--
-- 警告：
-- 1. 本脚本会 TRUNCATE sys_role_res、sys_res_mount、sys_res_group、sys_res。
-- 2. 执行后会清空所有角色授权关系，并只给 code = 'admin' 的角色重新授予全部资源。
-- 3. operator / guest / test 等角色需要执行后在后台重新勾选权限。
-- 4. sys_res 仍然是最终鉴权资源表；sys_res_group / sys_res_mount 只辅助管理，不参与鉴权。

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

ALTER TABLE sys_res
    MODIFY COLUMN permission VARCHAR(128) NOT NULL DEFAULT '' COMMENT '权限标识（全局唯一）';

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
-- 清空菜单权限相关数据
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE sys_role_res;
TRUNCATE TABLE sys_res_mount;
TRUNCATE TABLE sys_res_group;
TRUNCATE TABLE sys_res;
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- 接口资源分组（辅助管理，不参与鉴权）
-- =============================================================================

INSERT INTO sys_res_group (
    id,
    delete_time,
    deleted,
    create_time,
    create_by,
    update_time,
    update_by,
    parent_id,
    name,
    sort,
    remark
) VALUES
(1000, @max_time, 0, @now, @operator, @now, @operator, 0, '系统管理接口', 10, ''),
(1100, @max_time, 0, @now, @operator, @now, @operator, 1000, '用户管理接口', 10, ''),
(1200, @max_time, 0, @now, @operator, @now, @operator, 1000, '角色管理接口', 20, ''),
(1300, @max_time, 0, @now, @operator, @now, @operator, 1000, '资源管理接口', 30, ''),
(1400, @max_time, 0, @now, @operator, @now, @operator, 1000, '组织管理接口', 40, ''),
(1500, @max_time, 0, @now, @operator, @now, @operator, 1000, '岗位管理接口', 50, ''),
(1600, @max_time, 0, @now, @operator, @now, @operator, 1000, '字典管理接口', 60, ''),
(1700, @max_time, 0, @now, @operator, @now, @operator, 1000, '系统日志接口', 70, ''),
(2000, @max_time, 0, @now, @operator, @now, @operator, 0, '基础管理接口', 20, ''),
(2100, @max_time, 0, @now, @operator, @now, @operator, 2000, '地区管理接口', 10, ''),
(2200, @max_time, 0, @now, @operator, @now, @operator, 2000, '文件管理接口', 20, ''),
(2300, @max_time, 0, @now, @operator, @now, @operator, 2000, '存储管理接口', 30, '');

-- =============================================================================
-- 资源表：视图资源 + 接口资源
-- 资源类型：1-菜单 2-页面 3-按钮 4-接口 5-分组
-- 状态：1-启用 2-禁用
-- visible：1-左侧菜单可见 0-左侧菜单不可见；角色权限树仍可见
-- =============================================================================

INSERT INTO sys_res (
    id,
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
) VALUES
-- 系统管理：视图资源
(100, @max_time, 0, @now, @operator, @now, @operator, 1, 0, 0, '系统管理', '', '/system', 'Layout', 'Setting', 0, '', '1', 1, 10, '系统管理菜单分组'),
(101, @max_time, 0, @now, @operator, @now, @operator, 2, 100, 0, '用户管理', '', '/system/user', 'system/user/index', 'User', 0, '', '1', 1, 10, '用户管理页面'),
(102, @max_time, 0, @now, @operator, @now, @operator, 2, 100, 0, '角色管理', '', '/system/role', 'system/role/index', 'UserFilled', 0, '', '1', 1, 20, '角色管理页面'),
(103, @max_time, 0, @now, @operator, @now, @operator, 2, 100, 0, '资源管理', '', '/system/resource', 'system/menu/index', 'Menu', 0, '', '1', 1, 30, '资源管理页面'),
(104, @max_time, 0, @now, @operator, @now, @operator, 2, 100, 0, '组织管理', '', '/system/org', 'system/dept/index', 'OfficeBuilding', 0, '', '1', 1, 40, '组织管理页面'),
(105, @max_time, 0, @now, @operator, @now, @operator, 2, 100, 0, '岗位管理', '', '/system/position', 'system/post/index', 'Postcard', 0, '', '1', 1, 50, '岗位管理页面'),
(106, @max_time, 0, @now, @operator, @now, @operator, 2, 100, 0, '字典管理', '', '/system/dict', 'system/dict/index', 'Collection', 0, '', '1', 1, 60, '字典管理页面'),
(107, @max_time, 0, @now, @operator, @now, @operator, 2, 100, 0, '系统日志', '', '/system/log', 'system/log/index', 'Document', 0, '', '1', 1, 70, '系统日志页面'),
(1101, @max_time, 0, @now, @operator, @now, @operator, 3, 101, 0, '查看用户', 'view:admin:sysUser:detail', '', '', '', 0, '', '1', 1, 20, ''),
(1102, @max_time, 0, @now, @operator, @now, @operator, 3, 101, 0, '新增用户', 'view:admin:sysUser:create', '', '', '', 0, '', '1', 1, 30, ''),
(1103, @max_time, 0, @now, @operator, @now, @operator, 3, 101, 0, '编辑用户', 'view:admin:sysUser:update', '', '', '', 0, '', '1', 1, 40, ''),
(1104, @max_time, 0, @now, @operator, @now, @operator, 3, 101, 0, '删除用户', 'view:admin:sysUser:delete', '', '', '', 0, '', '1', 1, 50, ''),
(1201, @max_time, 0, @now, @operator, @now, @operator, 3, 102, 0, '新增角色', 'view:admin:sysRole:create', '', '', '', 0, '', '1', 1, 30, ''),
(1202, @max_time, 0, @now, @operator, @now, @operator, 3, 102, 0, '编辑角色', 'view:admin:sysRole:update', '', '', '', 0, '', '1', 1, 40, ''),
(1203, @max_time, 0, @now, @operator, @now, @operator, 3, 102, 0, '删除角色', 'view:admin:sysRole:delete', '', '', '', 0, '', '1', 1, 50, ''),
(1204, @max_time, 0, @now, @operator, @now, @operator, 3, 102, 0, '分配权限', 'view:admin:sysRole:setResources', '', '', '', 0, '', '1', 1, 60, ''),
(1301, @max_time, 0, @now, @operator, @now, @operator, 3, 103, 0, '新增资源', 'view:admin:sysRes:create', '', '', '', 0, '', '1', 1, 30, ''),
(1302, @max_time, 0, @now, @operator, @now, @operator, 3, 103, 0, '编辑资源', 'view:admin:sysRes:update', '', '', '', 0, '', '1', 1, 40, ''),
(1303, @max_time, 0, @now, @operator, @now, @operator, 3, 103, 0, '删除资源', 'view:admin:sysRes:delete', '', '', '', 0, '', '1', 1, 50, ''),
(1304, @max_time, 0, @now, @operator, @now, @operator, 3, 103, 0, '新增接口分组', 'view:admin:sysResGroup:create', '', '', '', 0, '', '1', 1, 60, ''),
(1305, @max_time, 0, @now, @operator, @now, @operator, 3, 103, 0, '编辑接口分组', 'view:admin:sysResGroup:update', '', '', '', 0, '', '1', 1, 70, ''),
(1306, @max_time, 0, @now, @operator, @now, @operator, 3, 103, 0, '删除接口分组', 'view:admin:sysResGroup:delete', '', '', '', 0, '', '1', 1, 80, ''),
(1307, @max_time, 0, @now, @operator, @now, @operator, 3, 103, 0, '挂载接口', 'view:admin:sysResMount:save', '', '', '', 0, '', '1', 1, 90, ''),
(1401, @max_time, 0, @now, @operator, @now, @operator, 3, 104, 0, '新增组织', 'view:admin:sysOrg:create', '', '', '', 0, '', '1', 1, 30, ''),
(1402, @max_time, 0, @now, @operator, @now, @operator, 3, 104, 0, '编辑组织', 'view:admin:sysOrg:update', '', '', '', 0, '', '1', 1, 40, ''),
(1403, @max_time, 0, @now, @operator, @now, @operator, 3, 104, 0, '删除组织', 'view:admin:sysOrg:delete', '', '', '', 0, '', '1', 1, 50, ''),
(1501, @max_time, 0, @now, @operator, @now, @operator, 3, 105, 0, '新增岗位', 'view:admin:sysPost:create', '', '', '', 0, '', '1', 1, 30, ''),
(1502, @max_time, 0, @now, @operator, @now, @operator, 3, 105, 0, '编辑岗位', 'view:admin:sysPost:update', '', '', '', 0, '', '1', 1, 40, ''),
(1503, @max_time, 0, @now, @operator, @now, @operator, 3, 105, 0, '删除岗位', 'view:admin:sysPost:delete', '', '', '', 0, '', '1', 1, 50, ''),
(1601, @max_time, 0, @now, @operator, @now, @operator, 3, 106, 0, '新增字典', 'view:admin:sysDict:create', '', '', '', 0, '', '1', 1, 30, ''),
(1602, @max_time, 0, @now, @operator, @now, @operator, 3, 106, 0, '编辑字典', 'view:admin:sysDict:update', '', '', '', 0, '', '1', 1, 40, ''),
(1603, @max_time, 0, @now, @operator, @now, @operator, 3, 106, 0, '删除字典', 'view:admin:sysDict:delete', '', '', '', 0, '', '1', 1, 50, ''),

-- 基础管理：视图资源
(200, @max_time, 0, @now, @operator, @now, @operator, 1, 0, 0, '基础管理', '', '/basic', 'Layout', 'Files', 0, '', '1', 1, 20, '基础能力菜单分组'),
(201, @max_time, 0, @now, @operator, @now, @operator, 2, 200, 0, '地区管理', '', '/system/area', 'system/area/index', 'Location', 0, '', '1', 1, 10, '地区管理页面'),
(202, @max_time, 0, @now, @operator, @now, @operator, 2, 200, 0, '文件管理', '', '/system/file', 'system/file/index', 'Folder', 0, '', '1', 1, 20, '文件管理页面'),
(203, @max_time, 0, @now, @operator, @now, @operator, 2, 200, 0, '存储管理', '', '/system/storage', 'system/storage/index', 'UploadFilled', 0, '', '1', 1, 30, '存储管理页面'),
(2101, @max_time, 0, @now, @operator, @now, @operator, 3, 201, 0, '查看省份', 'view:admin:areaProvince:crud:detail', '', '', '', 0, '', '1', 1, 20, ''),
(2102, @max_time, 0, @now, @operator, @now, @operator, 3, 201, 0, '新增省份', 'view:admin:areaProvince:crud:create', '', '', '', 0, '', '1', 1, 30, ''),
(2103, @max_time, 0, @now, @operator, @now, @operator, 3, 201, 0, '编辑省份', 'view:admin:areaProvince:crud:update', '', '', '', 0, '', '1', 1, 40, ''),
(2104, @max_time, 0, @now, @operator, @now, @operator, 3, 201, 0, '删除省份', 'view:admin:areaProvince:crud:delete', '', '', '', 0, '', '1', 1, 50, ''),
(2105, @max_time, 0, @now, @operator, @now, @operator, 3, 201, 0, '查看城市', 'view:admin:areaCity:crud:detail', '', '', '', 0, '', '1', 1, 60, ''),
(2106, @max_time, 0, @now, @operator, @now, @operator, 3, 201, 0, '新增城市', 'view:admin:areaCity:crud:create', '', '', '', 0, '', '1', 1, 70, ''),
(2107, @max_time, 0, @now, @operator, @now, @operator, 3, 201, 0, '编辑城市', 'view:admin:areaCity:crud:update', '', '', '', 0, '', '1', 1, 80, ''),
(2108, @max_time, 0, @now, @operator, @now, @operator, 3, 201, 0, '删除城市', 'view:admin:areaCity:crud:delete', '', '', '', 0, '', '1', 1, 90, ''),
(2109, @max_time, 0, @now, @operator, @now, @operator, 3, 201, 0, '查看区县', 'view:admin:areaDistrict:crud:detail', '', '', '', 0, '', '1', 1, 100, ''),
(2110, @max_time, 0, @now, @operator, @now, @operator, 3, 201, 0, '新增区县', 'view:admin:areaDistrict:crud:create', '', '', '', 0, '', '1', 1, 110, ''),
(2111, @max_time, 0, @now, @operator, @now, @operator, 3, 201, 0, '编辑区县', 'view:admin:areaDistrict:crud:update', '', '', '', 0, '', '1', 1, 120, ''),
(2112, @max_time, 0, @now, @operator, @now, @operator, 3, 201, 0, '删除区县', 'view:admin:areaDistrict:crud:delete', '', '', '', 0, '', '1', 1, 130, ''),
(2201, @max_time, 0, @now, @operator, @now, @operator, 3, 202, 0, '查看文件', 'view:basic:file:crud:detail', '', '', '', 0, '', '1', 1, 20, ''),
(2202, @max_time, 0, @now, @operator, @now, @operator, 3, 202, 0, '上传文件', 'view:basic:file:upload', '', '', '', 0, '', '1', 1, 30, ''),
(2203, @max_time, 0, @now, @operator, @now, @operator, 3, 202, 0, '下载文件', 'view:basic:file:download', '', '', '', 0, '', '1', 1, 40, ''),
(2204, @max_time, 0, @now, @operator, @now, @operator, 3, 202, 0, '删除文件', 'view:basic:file:delete', '', '', '', 0, '', '1', 1, 50, ''),
(2301, @max_time, 0, @now, @operator, @now, @operator, 3, 203, 0, '查看存储', 'view:basic:storage:crud:detail', '', '', '', 0, '', '1', 1, 20, ''),
(2302, @max_time, 0, @now, @operator, @now, @operator, 3, 203, 0, '上传存储', 'view:basic:storage:upload', '', '', '', 0, '', '1', 1, 30, ''),
(2303, @max_time, 0, @now, @operator, @now, @operator, 3, 203, 0, '下载存储', 'view:basic:storage:download', '', '', '', 0, '', '1', 1, 40, ''),
(2304, @max_time, 0, @now, @operator, @now, @operator, 3, 203, 0, '删除存储', 'view:basic:storage:delete', '', '', '', 0, '', '1', 1, 50, ''),

-- 用户管理：接口资源
(10101, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1100, '用户列表', 'api:admin:sysUser:query', '', '', '', 0, '', '1', 1, 10, 'GET /admin/sysUser/query'),
(10102, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1100, '用户详情', 'api:admin:sysUser:detail', '', '', '', 0, '', '1', 1, 20, 'GET /admin/sysUser/detail'),
(10103, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1100, '新增用户接口', 'api:admin:sysUser:create', '', '', '', 0, '', '1', 1, 30, 'POST /admin/sysUser/create'),
(10104, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1100, '编辑用户接口', 'api:admin:sysUser:update', '', '', '', 0, '', '1', 1, 40, 'POST /admin/sysUser/update'),
(10105, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1100, '删除用户接口', 'api:admin:sysUser:delete', '', '', '', 0, '', '1', 1, 50, 'POST /admin/sysUser/delete'),

-- 角色管理：接口资源
(10201, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1200, '角色列表', 'api:admin:sysRole:crud:query', '', '', '', 0, '', '1', 1, 10, 'GET /admin/sysRole/crud/query'),
(10202, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1200, '角色详情', 'api:admin:sysRole:detail', '', '', '', 0, '', '1', 1, 20, 'GET /admin/sysRole/detail'),
(10203, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1200, '新增角色接口', 'api:admin:sysRole:create', '', '', '', 0, '', '1', 1, 30, 'POST /admin/sysRole/create'),
(10204, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1200, '编辑角色接口', 'api:admin:sysRole:update', '', '', '', 0, '', '1', 1, 40, 'POST /admin/sysRole/update'),
(10205, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1200, '删除角色接口', 'api:admin:sysRole:delete', '', '', '', 0, '', '1', 1, 50, 'POST /admin/sysRole/delete'),
(10206, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1200, '角色资源树', 'api:admin:sysRole:queryResourceTree', '', '', '', 0, '', '1', 1, 60, 'GET /admin/sysRole/queryResourceTree'),
(10207, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1200, '保存角色资源', 'api:admin:sysRole:setResources', '', '', '', 0, '', '1', 1, 70, 'POST /admin/sysRole/setResources'),

-- 资源管理：接口资源
(10301, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1300, '资源树', 'api:admin:sysRes:tree', '', '', '', 0, '', '1', 1, 10, 'GET /admin/sysRes/tree、/viewTree、/apiTree'),
(10302, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1300, '资源详情', 'api:admin:sysRes:crud:detail', '', '', '', 0, '', '1', 1, 20, 'GET /admin/sysRes/crud/detail'),
(10303, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1300, '新增资源接口', 'api:admin:sysRes:create', '', '', '', 0, '', '1', 1, 30, 'POST /admin/sysRes/create'),
(10304, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1300, '编辑资源接口', 'api:admin:sysRes:update', '', '', '', 0, '', '1', 1, 40, 'POST /admin/sysRes/update'),
(10305, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1300, '删除资源接口', 'api:admin:sysRes:delete', '', '', '', 0, '', '1', 1, 50, 'POST /admin/sysRes/delete'),
(10306, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1300, '接口分组树', 'api:admin:sysResGroup:tree', '', '', '', 0, '', '1', 1, 60, 'GET /admin/sysResGroup/tree'),
(10307, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1300, '新增接口分组接口', 'api:admin:sysResGroup:create', '', '', '', 0, '', '1', 1, 70, 'POST /admin/sysResGroup/create'),
(10308, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1300, '编辑接口分组接口', 'api:admin:sysResGroup:update', '', '', '', 0, '', '1', 1, 80, 'POST /admin/sysResGroup/update'),
(10309, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1300, '删除接口分组接口', 'api:admin:sysResGroup:delete', '', '', '', 0, '', '1', 1, 90, 'POST /admin/sysResGroup/delete'),
(10310, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1300, '查询资源挂载接口', 'api:admin:sysResMount:query', '', '', '', 0, '', '1', 1, 100, 'GET /admin/sysResMount/queryByViewResId'),
(10311, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1300, '保存资源挂载接口', 'api:admin:sysResMount:save', '', '', '', 0, '', '1', 1, 110, 'POST /admin/sysResMount/saveByViewResId'),

-- 组织管理：接口资源
(10401, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1400, '组织树', 'api:admin:sysOrg:tree', '', '', '', 0, '', '1', 1, 10, 'GET /admin/sysOrg/tree'),
(10402, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1400, '组织详情', 'api:admin:sysOrg:crud:detail', '', '', '', 0, '', '1', 1, 20, 'GET /admin/sysOrg/crud/detail'),
(10403, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1400, '新增组织接口', 'api:admin:sysOrg:create', '', '', '', 0, '', '1', 1, 30, 'POST /admin/sysOrg/create'),
(10404, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1400, '编辑组织接口', 'api:admin:sysOrg:update', '', '', '', 0, '', '1', 1, 40, 'POST /admin/sysOrg/update'),
(10405, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1400, '删除组织接口', 'api:admin:sysOrg:delete', '', '', '', 0, '', '1', 1, 50, 'POST /admin/sysOrg/delete'),

-- 岗位管理：接口资源
(10501, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1500, '岗位列表', 'api:admin:sysPost:crud:query', '', '', '', 0, '', '1', 1, 10, 'GET /admin/sysPost/crud/query'),
(10502, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1500, '岗位详情', 'api:admin:sysPost:crud:detail', '', '', '', 0, '', '1', 1, 20, 'GET /admin/sysPost/crud/detail'),
(10503, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1500, '新增岗位接口', 'api:admin:sysPost:create', '', '', '', 0, '', '1', 1, 30, 'POST /admin/sysPost/create'),
(10504, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1500, '编辑岗位接口', 'api:admin:sysPost:update', '', '', '', 0, '', '1', 1, 40, 'POST /admin/sysPost/update'),
(10505, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1500, '删除岗位接口', 'api:admin:sysPost:delete', '', '', '', 0, '', '1', 1, 50, 'POST /admin/sysPost/delete'),

-- 字典管理：接口资源
(10601, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1600, '字典列表', 'api:admin:sysDict:crud:query', '', '', '', 0, '', '1', 1, 10, 'GET /admin/sysDict/crud/query'),
(10602, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1600, '字典树', 'api:admin:sysDict:tree', '', '', '', 0, '', '1', 1, 20, 'GET /admin/sysDict/tree'),
(10603, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1600, '字典详情', 'api:admin:sysDict:crud:detail', '', '', '', 0, '', '1', 1, 30, 'GET /admin/sysDict/crud/detail'),
(10604, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1600, '新增字典接口', 'api:admin:sysDict:create', '', '', '', 0, '', '1', 1, 40, 'POST /admin/sysDict/create'),
(10605, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1600, '编辑字典接口', 'api:admin:sysDict:update', '', '', '', 0, '', '1', 1, 50, 'POST /admin/sysDict/update'),
(10606, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1600, '删除字典接口', 'api:admin:sysDict:delete', '', '', '', 0, '', '1', 1, 60, 'POST /admin/sysDict/delete'),

-- 系统日志：接口资源
(10701, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 1700, '日志列表', 'api:admin:sysLog:crud:query', '', '', '', 0, '', '1', 1, 10, 'GET /admin/sysLog/crud/query'),

-- 地区管理：接口资源
(20101, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '地区树', 'api:admin:area:tree', '', '', '', 0, '', '1', 1, 10, 'GET /area/tree'),
(20102, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '省份详情', 'api:admin:areaProvince:crud:detail', '', '', '', 0, '', '1', 1, 20, 'GET /area/province/crud/detail'),
(20103, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '新增省份接口', 'api:admin:areaProvince:crud:create', '', '', '', 0, '', '1', 1, 30, 'POST /area/province/crud/create'),
(20104, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '编辑省份接口', 'api:admin:areaProvince:crud:update', '', '', '', 0, '', '1', 1, 40, 'POST /area/province/crud/update'),
(20105, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '删除省份接口', 'api:admin:areaProvince:crud:delete', '', '', '', 0, '', '1', 1, 50, 'POST /area/province/crud/delete'),
(20106, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '城市列表', 'api:admin:areaCity:crud:query', '', '', '', 0, '', '1', 1, 60, 'GET /area/city/crud/query'),
(20107, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '城市详情', 'api:admin:areaCity:crud:detail', '', '', '', 0, '', '1', 1, 70, 'GET /area/city/crud/detail'),
(20108, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '新增城市接口', 'api:admin:areaCity:crud:create', '', '', '', 0, '', '1', 1, 80, 'POST /area/city/crud/create'),
(20109, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '编辑城市接口', 'api:admin:areaCity:crud:update', '', '', '', 0, '', '1', 1, 90, 'POST /area/city/crud/update'),
(20110, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '删除城市接口', 'api:admin:areaCity:crud:delete', '', '', '', 0, '', '1', 1, 100, 'POST /area/city/crud/delete'),
(20111, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '区县列表', 'api:admin:areaDistrict:crud:query', '', '', '', 0, '', '1', 1, 110, 'GET /area/district/crud/query'),
(20112, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '区县详情', 'api:admin:areaDistrict:crud:detail', '', '', '', 0, '', '1', 1, 120, 'GET /area/district/crud/detail'),
(20113, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '新增区县接口', 'api:admin:areaDistrict:crud:create', '', '', '', 0, '', '1', 1, 130, 'POST /area/district/crud/create'),
(20114, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '编辑区县接口', 'api:admin:areaDistrict:crud:update', '', '', '', 0, '', '1', 1, 140, 'POST /area/district/crud/update'),
(20115, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2100, '删除区县接口', 'api:admin:areaDistrict:crud:delete', '', '', '', 0, '', '1', 1, 150, 'POST /area/district/crud/delete'),

-- 文件管理：接口资源
(20201, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2200, '文件列表', 'api:basic:file:crud:query', '', '', '', 0, '', '1', 1, 10, 'GET /basic/file/crud/query'),
(20202, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2200, '文件详情', 'api:basic:file:crud:detail', '', '', '', 0, '', '1', 1, 20, 'GET /basic/file/crud/detail'),
(20203, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2200, '上传文件接口', 'api:basic:file:upload', '', '', '', 0, '', '1', 1, 30, 'POST /basic/file/upload'),
(20204, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2200, '下载文件接口', 'api:basic:file:download', '', '', '', 0, '', '1', 1, 40, 'GET /basic/file/download'),
(20205, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2200, '删除文件接口', 'api:basic:file:delete', '', '', '', 0, '', '1', 1, 50, 'POST /basic/file/delete'),

-- 存储管理：接口资源
(20301, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2300, '存储列表', 'api:basic:storage:crud:query', '', '', '', 0, '', '1', 1, 10, 'GET /basic/storage/crud/query'),
(20302, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2300, '存储详情', 'api:basic:storage:crud:detail', '', '', '', 0, '', '1', 1, 20, 'GET /basic/storage/crud/detail'),
(20303, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2300, '上传存储接口', 'api:basic:storage:upload', '', '', '', 0, '', '1', 1, 30, 'POST /basic/storage/upload'),
(20304, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2300, '下载存储接口', 'api:basic:storage:download', '', '', '', 0, '', '1', 1, 40, 'GET /basic/storage/download'),
(20305, @max_time, 0, @now, @operator, @now, @operator, 4, 0, 2300, '删除存储接口', 'api:basic:storage:delete', '', '', '', 0, '', '1', 1, 50, 'POST /basic/storage/delete');

-- =============================================================================
-- 视图资源挂载接口资源（辅助管理，不参与鉴权）
-- 勾选视图节点时，授权树会自动带出挂载的接口资源；仍可单独取消接口资源。
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
) VALUES
-- 用户管理
(@max_time, 0, @now, @operator, @now, @operator, 101, 10101, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1101, 10102, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1102, 10103, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1103, 10102, 10, '编辑前加载详情'),
(@max_time, 0, @now, @operator, @now, @operator, 1103, 10104, 20, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1104, 10105, 10, ''),

-- 角色管理
(@max_time, 0, @now, @operator, @now, @operator, 102, 10201, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1201, 10203, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1202, 10202, 10, '编辑前加载详情'),
(@max_time, 0, @now, @operator, @now, @operator, 1202, 10204, 20, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1203, 10205, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1204, 10206, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1204, 10207, 20, ''),

-- 资源管理
(@max_time, 0, @now, @operator, @now, @operator, 103, 10301, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 103, 10306, 20, '接口资源 Tab 加载接口分组树'),
(@max_time, 0, @now, @operator, @now, @operator, 1301, 10303, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1302, 10302, 10, '编辑前加载详情'),
(@max_time, 0, @now, @operator, @now, @operator, 1302, 10304, 20, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1303, 10305, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1304, 10307, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1305, 10308, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1306, 10309, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1307, 10310, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1307, 10311, 20, ''),

-- 组织管理
(@max_time, 0, @now, @operator, @now, @operator, 104, 10401, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1401, 10403, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1402, 10402, 10, '编辑前加载详情'),
(@max_time, 0, @now, @operator, @now, @operator, 1402, 10404, 20, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1403, 10405, 10, ''),

-- 岗位管理
(@max_time, 0, @now, @operator, @now, @operator, 105, 10501, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1501, 10503, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1502, 10502, 10, '编辑前加载详情'),
(@max_time, 0, @now, @operator, @now, @operator, 1502, 10504, 20, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1503, 10505, 10, ''),

-- 字典管理
(@max_time, 0, @now, @operator, @now, @operator, 106, 10601, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 106, 10602, 20, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1601, 10604, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1602, 10603, 10, '编辑前加载详情'),
(@max_time, 0, @now, @operator, @now, @operator, 1602, 10605, 20, ''),
(@max_time, 0, @now, @operator, @now, @operator, 1603, 10606, 10, ''),

-- 系统日志
(@max_time, 0, @now, @operator, @now, @operator, 107, 10701, 10, ''),

-- 地区管理
(@max_time, 0, @now, @operator, @now, @operator, 201, 20101, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 201, 20106, 20, '右侧下级城市列表'),
(@max_time, 0, @now, @operator, @now, @operator, 201, 20111, 30, '右侧下级区县列表'),
(@max_time, 0, @now, @operator, @now, @operator, 2101, 20102, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2102, 20103, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2103, 20102, 10, '编辑前加载详情'),
(@max_time, 0, @now, @operator, @now, @operator, 2103, 20104, 20, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2104, 20105, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2105, 20107, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2106, 20108, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2107, 20107, 10, '编辑前加载详情'),
(@max_time, 0, @now, @operator, @now, @operator, 2107, 20109, 20, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2108, 20110, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2109, 20112, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2110, 20113, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2111, 20112, 10, '编辑前加载详情'),
(@max_time, 0, @now, @operator, @now, @operator, 2111, 20114, 20, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2112, 20115, 10, ''),

-- 文件管理
(@max_time, 0, @now, @operator, @now, @operator, 202, 20201, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2201, 20202, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2202, 20203, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2203, 20204, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2204, 20205, 10, ''),

-- 存储管理
(@max_time, 0, @now, @operator, @now, @operator, 203, 20301, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2301, 20302, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2302, 20303, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2303, 20304, 10, ''),
(@max_time, 0, @now, @operator, @now, @operator, 2304, 20305, 10, '');

-- =============================================================================
-- admin 角色全量授权
-- =============================================================================

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
JOIN sys_res res ON res.deleted = 0
WHERE role.deleted = 0
  AND role.code = @grant_all_role_code
  AND res.type <> 4;

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
    api.id,
    mount.id
FROM sys_role role
JOIN sys_res_mount mount
  ON mount.deleted = 0
JOIN sys_res api
  ON api.deleted = 0
 AND api.id = mount.api_res_id
 AND api.type = 4
WHERE role.deleted = 0
  AND role.code = @grant_all_role_code
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_res rr
      WHERE rr.deleted = 0
        AND rr.role_id = role.id
        AND rr.res_id = api.id
        AND rr.mount_id = mount.id
  );

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
    api.id,
    0
FROM sys_role role
JOIN sys_res api
  ON api.deleted = 0
 AND api.type = 4
LEFT JOIN sys_res_mount mount
  ON mount.deleted = 0
 AND mount.api_res_id = api.id
WHERE role.deleted = 0
  AND role.code = @grant_all_role_code
  AND mount.id IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_res rr
      WHERE rr.deleted = 0
        AND rr.role_id = role.id
        AND rr.res_id = api.id
        AND rr.mount_id = 0
  );

-- =============================================================================
-- 校验统计
-- =============================================================================

SELECT COUNT(*) AS sys_res_count FROM sys_res WHERE deleted = 0;
SELECT COUNT(*) AS sys_res_group_count FROM sys_res_group WHERE deleted = 0;
SELECT COUNT(*) AS sys_res_mount_count FROM sys_res_mount WHERE deleted = 0;
SELECT role.code, role.name, COUNT(rr.id) AS res_count
FROM sys_role role
LEFT JOIN sys_role_res rr
  ON rr.deleted = 0
 AND rr.role_id = role.id
WHERE role.deleted = 0
GROUP BY role.id, role.code, role.name
ORDER BY role.id;

