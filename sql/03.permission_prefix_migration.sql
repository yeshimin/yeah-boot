-- =============================================================================
-- 资源权限标识前缀规范化
--
-- 说明：
-- 1. 视图资源使用 view: 前缀，接口资源使用 api: 前缀。
-- 2. 前缀仅用于命名和维护，不参与鉴权算法或资源类型判断。
-- 3. sys_res、sys_role_res、sys_res_mount 的资源ID和关联关系均不变。
-- 4. 脚本可以重复执行，不会重复添加前缀。
-- 5. 必须与本次前后端代码同步使用；执行后刷新页面或重新登录。
-- =============================================================================

SET @now := NOW();
SET @operator := 'system';

-- 增加前缀后部分权限标识会超过旧字段长度，统一预留足够空间。
ALTER TABLE `sys_res`
    MODIFY COLUMN `permission` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '权限标识（全局唯一）';

START TRANSACTION;

-- =============================================================================
-- 1. 为历史视图按钮补齐独立的 view: 权限标识
-- =============================================================================

UPDATE `sys_res` button
INNER JOIN `sys_res` page ON page.`id` = button.`parent_id` AND page.`deleted` = 0
SET button.`permission` = CASE CONCAT(page.`path`, '|', button.`name`)
        WHEN '/system/user|查看用户' THEN 'view:admin:sysUser:detail'
        WHEN '/system/user|新增用户' THEN 'view:admin:sysUser:create'
        WHEN '/system/user|编辑用户' THEN 'view:admin:sysUser:update'
        WHEN '/system/user|删除用户' THEN 'view:admin:sysUser:delete'
        WHEN '/system/role|新增角色' THEN 'view:admin:sysRole:create'
        WHEN '/system/role|编辑角色' THEN 'view:admin:sysRole:update'
        WHEN '/system/role|删除角色' THEN 'view:admin:sysRole:delete'
        WHEN '/system/role|分配权限' THEN 'view:admin:sysRole:setResources'
        WHEN '/system/resource|新增资源' THEN 'view:admin:sysRes:create'
        WHEN '/system/resource|编辑资源' THEN 'view:admin:sysRes:update'
        WHEN '/system/resource|删除资源' THEN 'view:admin:sysRes:delete'
        WHEN '/system/resource|新增接口分组' THEN 'view:admin:sysResGroup:create'
        WHEN '/system/resource|编辑接口分组' THEN 'view:admin:sysResGroup:update'
        WHEN '/system/resource|删除接口分组' THEN 'view:admin:sysResGroup:delete'
        WHEN '/system/resource|挂载接口' THEN 'view:admin:sysResMount:save'
        WHEN '/system/org|新增组织' THEN 'view:admin:sysOrg:create'
        WHEN '/system/org|编辑组织' THEN 'view:admin:sysOrg:update'
        WHEN '/system/org|删除组织' THEN 'view:admin:sysOrg:delete'
        WHEN '/system/position|新增岗位' THEN 'view:admin:sysPost:create'
        WHEN '/system/position|编辑岗位' THEN 'view:admin:sysPost:update'
        WHEN '/system/position|删除岗位' THEN 'view:admin:sysPost:delete'
        WHEN '/system/dict|新增字典' THEN 'view:admin:sysDict:create'
        WHEN '/system/dict|编辑字典' THEN 'view:admin:sysDict:update'
        WHEN '/system/dict|删除字典' THEN 'view:admin:sysDict:delete'
        WHEN '/system/area|查看省份' THEN 'view:admin:areaProvince:crud:detail'
        WHEN '/system/area|新增省份' THEN 'view:admin:areaProvince:crud:create'
        WHEN '/system/area|编辑省份' THEN 'view:admin:areaProvince:crud:update'
        WHEN '/system/area|删除省份' THEN 'view:admin:areaProvince:crud:delete'
        WHEN '/system/area|查看城市' THEN 'view:admin:areaCity:crud:detail'
        WHEN '/system/area|新增城市' THEN 'view:admin:areaCity:crud:create'
        WHEN '/system/area|编辑城市' THEN 'view:admin:areaCity:crud:update'
        WHEN '/system/area|删除城市' THEN 'view:admin:areaCity:crud:delete'
        WHEN '/system/area|查看区县' THEN 'view:admin:areaDistrict:crud:detail'
        WHEN '/system/area|新增区县' THEN 'view:admin:areaDistrict:crud:create'
        WHEN '/system/area|编辑区县' THEN 'view:admin:areaDistrict:crud:update'
        WHEN '/system/area|删除区县' THEN 'view:admin:areaDistrict:crud:delete'
        WHEN '/system/file|查看文件' THEN 'view:basic:file:crud:detail'
        WHEN '/system/file|上传文件' THEN 'view:basic:file:upload'
        WHEN '/system/file|下载文件' THEN 'view:basic:file:download'
        WHEN '/system/file|删除文件' THEN 'view:basic:file:delete'
        WHEN '/system/storage|查看存储' THEN 'view:basic:storage:crud:detail'
        WHEN '/system/storage|上传存储' THEN 'view:basic:storage:upload'
        WHEN '/system/storage|下载存储' THEN 'view:basic:storage:download'
        WHEN '/system/storage|删除存储' THEN 'view:basic:storage:delete'
        ELSE button.`permission`
    END,
    button.`update_time` = @now,
    button.`update_by` = @operator
WHERE button.`deleted` = 0
  AND button.`type` = 3
  AND page.`path` IN (
      '/system/user', '/system/role', '/system/resource', '/system/org', '/system/position',
      '/system/dict', '/system/area', '/system/file', '/system/storage'
  );

-- =============================================================================
-- 2. 规范其他已有视图资源标识
-- =============================================================================

UPDATE `sys_res`
SET `permission` = CASE
        WHEN `permission` LIKE 'api:%' THEN CONCAT('view:', SUBSTRING(`permission`, 5))
        ELSE CONCAT('view:', `permission`)
    END,
    `update_time` = @now,
    `update_by` = @operator
WHERE `deleted` = 0
  AND `type` IN (1, 2, 3, 5)
  AND `permission` <> ''
  AND `permission` <> '*:*:*'
  AND `permission` NOT LIKE 'view:%';

-- =============================================================================
-- 3. 规范全部接口资源标识
-- =============================================================================

UPDATE `sys_res`
SET `permission` = CASE
        WHEN `permission` LIKE 'view:%' THEN CONCAT('api:', SUBSTRING(`permission`, 6))
        ELSE CONCAT('api:', `permission`)
    END,
    `update_time` = @now,
    `update_by` = @operator
WHERE `deleted` = 0
  AND `type` = 4
  AND `permission` <> ''
  AND `permission` <> '*:*:*'
  AND `permission` NOT LIKE 'api:%';

COMMIT;

-- =============================================================================
-- 4. 执行结果核对
-- =============================================================================

-- 正常结果应为0行：非空视图标识必须以 view: 开头，接口标识必须以 api: 开头。
SELECT `id`, `type`, `name`, `permission`
FROM `sys_res`
WHERE `deleted` = 0
  AND `permission` <> ''
  AND `permission` <> '*:*:*'
  AND (
      (`type` = 4 AND `permission` NOT LIKE 'api:%')
      OR (`type` IN (1, 2, 3, 5) AND `permission` NOT LIKE 'view:%')
  )
ORDER BY `type`, `id`;

-- 正常结果应为0行：检查有效资源中是否存在重复权限标识。
SELECT `permission`, COUNT(*) AS `permission_count`
FROM `sys_res`
WHERE `deleted` = 0
  AND `permission` <> ''
GROUP BY `permission`
HAVING COUNT(*) > 1;

-- 汇总迁移后的权限标识数量。
SELECT
    SUM(CASE WHEN `permission` LIKE 'view:%' THEN 1 ELSE 0 END) AS `view_permission_count`,
    SUM(CASE WHEN `permission` LIKE 'api:%' THEN 1 ELSE 0 END) AS `api_permission_count`
FROM `sys_res`
WHERE `deleted` = 0;
