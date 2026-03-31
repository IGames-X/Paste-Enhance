# Changelog

## 0.0.4

- Performance: Changed activation event from `onCommand` to `onStartupFinished` — the PowerShell daemon now pre-warms in the background when VS Code starts, eliminating the 1–2 second delay on first paste.
- Performance: `findPowerShell()` is now asynchronous (uses `exec` instead of `execSync`), so PowerShell version detection no longer blocks the main thread.

- 性能优化：将激活事件从 `onCommand` 改为 `onStartupFinished`——PowerShell Daemon 在 VS Code 启动后即在后台预热，首次粘贴不再有 1–2 秒延迟。
- 性能优化：`findPowerShell()` 改为异步实现（使用 `exec` 替代 `execSync`），PowerShell 版本检测不再阻塞主线程。

## 0.0.3

- Added: `CHANGELOG.md` to display version history on Open VSX Registry.
- 新增：`CHANGELOG.md` 文件，以支持在 Open VSX Registry 上展示版本更新历史。

## 0.0.2

- Fixed: Plugin did not work on machines where PowerShell 5 execution policy was `Restricted` (default on new Windows installations). The daemon now starts with `-ExecutionPolicy Bypass`, requiring no manual configuration from the user.
- Fixed: Hardcoded PowerShell 5 path replaced with dynamic detection — the plugin now prefers PowerShell 7 (`pwsh`) if available, falling back to PowerShell 5 (`powershell`).

- 修复：在 PowerShell 5 执行策略为 `Restricted` 的新电脑上（Windows 默认设置），插件无法正常工作的问题。现在启动 daemon 时自动附加 `-ExecutionPolicy Bypass`，用户无需任何手动配置。
- 修复：移除硬编码的 PowerShell 5 路径，改为动态检测——优先使用 PowerShell 7（`pwsh`），不存在时回退到 PowerShell 5（`powershell`）。

## 0.0.1

- Initial release
- 初始版本发布
