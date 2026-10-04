# yeah-boot

> 本文档由 AI 辅助生成，发布前建议结合实际配置与功能状态再做一次人工校对。

> 基于 Java 21 / Spring Boot 3.5 的多模块后端快速开发框架，面向后台管理与 App 场景。

## 项目简介

`yeah-boot` 采用 Maven 聚合 + 多模块拆分的组织方式，将启动工程、基础框架、通用数据层和业务模块解耦。

当前项目已经内置以下常见后端能力：

- 管理后台与 App 同时支持单入口和双入口启动模式
- JWT 认证与基于资源编码的权限控制
- 统一返回结构与全局异常处理
- MyBatis-Plus 分页与基础 CRUD 抽象
- 文件存储抽象，支持本地 / MinIO / 七牛等扩展
- 短信通知、Redis 消息队列、WebSocket 推送
- 注解式接口限流

适合作为中小型后台系统、内部业务系统或自定义管理平台的基础脚手架。

## 核心特性

- `yeah-admin` 与 `yeah-app` 双启动工程，便于按终端拆分接口入口
- `yeah-framework` 抽离认证、安全、缓存、存储、MQ、通知、WebSocket 等基础能力
- `yeah-biz-module` 承载后台权限、基础业务、公共文件访问等模块
- `yeah-biz-common` 提供跨业务共享的数据与服务能力
- 基于 `Spring Security + JWT + Redis` 的无状态认证模型
- 基于 `MyBatis-Plus` 的实体、Repo、分页查询与基础 CRUD 能力
- 集成 `SpringDoc OpenAPI`，便于接口文档输出

## 模块结构

```text
yeah-boot
├── yeah-admin                 后台管理启动工程
├── yeah-app                   App 端启动工程
├── yeah-biz-common            跨业务通用模块
│   ├── yeah-biz-data          业务公共数据层
│   └── yeah-biz-service       业务公共服务层
├── yeah-biz-module            业务模块层
│   ├── yeah-basic             基础业务模块
│   ├── yeah-public            公共访问模块
│   └── yeah-upms              用户/角色/权限/组织模块
└── yeah-framework             基础框架层
    ├── yeah-auth              认证与鉴权
    ├── yeah-common-core       通用核心能力
    ├── yeah-common-data       通用数据层
    ├── yeah-flowcontrol       流控与限流
    ├── yeah-generator         代码生成相关
    ├── yeah-mq                消息队列抽象
    ├── yeah-notification      通知能力
    ├── yeah-storage           文件存储抽象
    └── yeah-websocket         WebSocket 能力
```

## 技术栈

- Java 21
- Spring Boot 3.5.16
- Spring MVC
- Spring Security
- MyBatis-Plus 3.5.17
- MySQL
- Redis
- JWT (`java-jwt`)
- SpringDoc OpenAPI 2
- Hutool / Fastjson2
- MinIO / 七牛云存储
- 阿里云短信
- WebSocket / Disruptor

## 典型能力

- 后台用户、角色、组织、资源管理
- App 用户短信验证码登录 / 密码登录
- 文件上传、下载、公开预览
- 地区数据查询与树形组装
- WebSocket 消息推送与广播
- Redis 消息发布与消费
- 注解式限流控制

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- MySQL
- Redis

### 数据库初始化

项目根目录的 `sql/` 下提供了初始化脚本压缩包，建议按以下顺序导入：

- `sql/00.yeah_boot_main.sql.zip`：主库初始化脚本，包含核心表结构与基础数据
- `sql/01.street_and_village_data.sql.zip`：街道与村庄数据，因数据量较大，按需导入

建议顺序：

1. 先导入 `00.yeah_boot_main`
2. 如业务需要更完整的地区数据，再导入 `01.street_and_village_data`

### 管理后台默认账号

- 用户名：`admin`
- 密码：`123456`

### 构建项目

```bash
mvn clean install
```

### 环境 Profile

`yeah-admin` 与 `yeah-app` 均提供以下配置文件：

- `application.yml`：共享配置，默认激活 `dev`
- `application-dev.yml`：本地开发环境配置
- `application-prod.yml`：生产环境配置，可由 Jar 同级 `config/application-prod.yml` 覆盖

本地开发默认使用 `dev`。启动生产环境时显式指定：

```bash
SPRING_PROFILES_ACTIVE=prod sh deploy.sh start
```

### 多 JDK 部署

部署脚本默认要求 JDK 21，可通过环境变量为不同服务选择独立运行时。`JAVA_BIN` 优先级最高；未设置时，脚本会优先使用 `JENV_JAVA_VERSION`，再尝试解析服务目录中的 `.java-version`，最后回退到系统 `java`。

使用服务目录的 `.java-version` 时，需将该文件与部署后的 `deploy.sh` 放在同一目录，例如文件内容为 `21`。

```bash
# 当前主线服务：默认要求 JDK 21
JENV_JAVA_VERSION=21 sh deploy.sh start

# JDK 8 维护线服务：显式指定运行时与最低版本
# 版本标识以 jenv versions 输出为准，例如 1.8 或 1.8.0.362
JENV_JAVA_VERSION=1.8 JAVA_MIN_VERSION=8 sh deploy.sh start

# 不使用 jenv 时，直接指定 Java 可执行文件
JAVA_BIN=/opt/jdk-21/bin/java sh deploy.sh start
```

同一台服务器上的每个服务应使用独立目录、Jar、端口、`config/`、日志和 PID 文件。

### 启动模式

当前支持两种启动方式：

- 单入口模式：只启动 `yeah-admin`，同时承载后台端与 App 端接口
- 双入口模式：分别启动 `yeah-admin` 和 `yeah-app`

接口区分方式：

- 后台接口统一以 `/admin` 为前缀
- App 接口统一以 `/app` 为前缀

### 单入口模式

推荐优先使用单入口模式，当前开箱即用：

```bash
mvn -pl yeah-admin -am spring-boot:run
```

说明：

- 该模式下由 `yeah-admin` 统一承载 admin 和 app 两套接口
- 访问时通过 `/admin` 和 `/app` 路径前缀区分不同终端接口

### 双入口模式

后台端可单独启动：

```bash
mvn -pl yeah-admin -am spring-boot:run
```

App 端可单独启动：

```bash
mvn -pl yeah-app -am spring-boot:run
```

说明：

- `yeah-app` 已提供与 `yeah-admin` 一致的 `dev` / `prod` Profile 配置结构
- 独立启动前需确保目标 Profile 的数据库、Redis、JWT 等基础设施配置完整
- 单入口与双入口可按部署拓扑选择

### 启动前准备

- 在本地准备 Spring Boot 运行配置
- 根据实际环境补充数据源、Redis、JWT、对象存储、短信等参数
- 若只需要部分能力，可以按模块依赖进行裁剪

### 配置示例

以下为部分配置示例，可按实际需要放入本地配置文件中：

```properties
# 日志级别
logging.level.com.yeshimin.yeahboot=debug

# 通知-阿里云短信配置
yeah-boot.notification.aliyun.sms.access-key-id=your-access-key-id
yeah-boot.notification.aliyun.sms.access-key-secret=your-access-key-secret
yeah-boot.notification.aliyun.sms.sign-name=your-sign-name
yeah-boot.notification.aliyun.sms.template-code=your-template-code

# 接口权限校验，true 表示不启用
yeah-boot.safe-mode=true

# 存储-七牛配置
yeah-boot.storage.impl.qiniu.access-key=your-qiniu-access-key
yeah-boot.storage.impl.qiniu.bucket=your-qiniu-bucket
yeah-boot.storage.impl.qiniu.domain=your-qiniu-domain
yeah-boot.storage.impl.qiniu.public-domain=your-qiniu-public-domain
yeah-boot.storage.impl.qiniu.secret-key=your-qiniu-secret-key
```

说明：

- `yeah-boot.safe-mode=true` 表示关闭接口权限校验，适合本地联调或初始化阶段使用
- 阿里云短信与七牛云存储相关配置按需启用，不使用时可不配置
- 仓库中的 JWT 相关 key/secret 仅用于测试或开发环境，正式部署前务必替换为你自己的安全配置
- 除上述自定义配置外，仍需补充 `spring.datasource.*`、`spring.data.redis.*` 等标准 Spring Boot 配置

### 登录页公告与自注册

管理后台登录页公告和自注册能力通过系统参数动态控制。参数变更后会自动刷新缓存，前端登录页会通过公开参数接口获取当前状态，无需根据域名判断环境。

| 参数键 | 类型 | 默认值 | 说明 |
|---|---|---:|---|
| `auth.login.notice.enabled` | BOOLEAN | `false` | 是否显示登录页公告 |
| `auth.login.notice.title` | STRING | 空 | 公告标题 |
| `auth.login.notice.content` | STRING | 空 | 公告正文，按纯文本显示并保留换行 |
| `auth.login.register.enabled` | BOOLEAN | `false` | 是否开放管理后台自注册 |
| `auth.login.register.default-role-code` | STRING | 空 | 自注册用户自动绑定的默认角色编码 |

登录页公开参数需同时满足“启用”和“公开访问”两个条件。公开接口为 `GET /admin/sysConfig/publicConfig`，支持按 `groupCode` 查询多个参数，或按 `configKey` 精确查询一个参数，响应统一为 `NameValueVo` 列表。`auth.login.register.default-role-code` 不应开启公开访问。

开启自注册前，必须先创建并启用受限角色，再将其角色编码填写到 `auth.login.register.default-role-code`。注册接口只接收用户名、密码和验证码，组织、岗位、角色、状态等字段由服务端控制。

## 开发建议

- 业务通用能力优先放入 `yeah-framework` 或 `yeah-biz-common`
- 面向后台的接口优先放入 `yeah-admin` / `yeah-upms`
- 面向 App 的接口优先放入 `yeah-app`
- 业务模块尽量复用现有通用返回、异常、Repo 与存储抽象

## Roadmap

完整的待处理、部分完成和暂缓事项见 [项目路线图](./docs/ROADMAP.md)。

- 支持更完善的全局限流模式
- 补充更完整的配置示例与部署说明
- 提升模块级测试覆盖率

## Contributing

欢迎通过 Issue 或 PR 参与讨论和改进。
