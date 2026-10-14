# Git Hooks

启用项目提供的轻量提交检查：

```sh
git config core.hooksPath .githooks
```

默认 Hook 只检查暂存区空白错误。需要在每次提交前编译后端时：

```sh
YEAH_BOOT_RUN_COMPILE_ON_COMMIT=1 git commit -m "..."
```

完整构建、依赖漏洞扫描和 SonarQube 应在 CI 或交付安全流程中执行，不应默认阻塞日常提交。
