# Changelog

## 0.0.8

- Fixed: Screenshot images could not be pasted after upgrading to PowerShell 7.6.0. The daemon was using `GetDataObject().GetData("System.Drawing.Bitmap")` which returns `null` in PowerShell 7.6 due to a .NET WinForms clipboard implementation change. Replaced with `Clipboard.GetImage()`, the dedicated static API that works consistently across PowerShell 5 and all PowerShell 7 versions.

- 修复：升级 PowerShell 至 7.6.0 后截图无法粘贴的问题。Daemon 原先使用 `GetDataObject().GetData("System.Drawing.Bitmap")` 获取剪贴板图片，但该调用在 PowerShell 7.6 的 .NET WinForms 剪贴板实现变更后始终返回 null。现已改用专用静态 API `Clipboard.GetImage()`，在 PowerShell 5 及所有 PowerShell 7 版本上行为一致。

## 0.0.7

- Improved: Removed IDE-specific references from all descriptions — the plugin works with any VS Code-compatible terminal.
- 优化：去除所有描述中对特定 IDE 的限定，插件适用于任意兼容 VS Code 的终端环境。

## 0.0.6

- Fixed: Minor bug fixes.
- 修复：修复一些错误。

## 0.0.5

- Fixed: `activeTerminal` API returning `null` even when a terminal is focused — the plugin now falls back to the first available terminal, preventing silent no-op on shortcut press.
- Fixed: PowerShell detection result being permanently cached on failure — when the system is busy at startup and `pwsh` detection times out, the bad result was cached and caused all subsequent attempts to fail. Detection is now only cached on success.
- Fixed: Detection now tries `pwsh` then `powershell` sequentially (with a 5 s timeout each) and caches only the first working one, making the plugin compatible with environments that have only PowerShell 5, only PowerShell 7, or both.
- Fixed: Daemon stdout pipe potentially corrupted when the integrated terminal reinitializes during startup. Spawn options now explicitly set `stdio: ['pipe', 'pipe', 'pipe']` to prevent terminal handle inheritance.
- Fixed: On daemon spawn failure (e.g. ENOENT), the cached PS executable is now cleared so the next shortcut press re-detects the correct executable instead of failing permanently.
- Fixed: Added `proc.on('error')` handler — spawn errors are now surfaced to the user instead of silently hanging.
- Fixed: Daemon startup timeout (previously 8 s, now 25 s) — `Add-Type -AssemblyName System.Windows.Forms/Drawing` can take over 10 s on first load after boot when the system is under load. On timeout the daemon process is killed, all state is reset, and the user is prompted to retry.
- Fixed: After a timeout the dangling daemon process was not killed, causing a second shortcut press to spawn a second PowerShell instance; global state variables were then corrupted by whichever process replied first.
- Improved: Status bar shows a spinner ("Paste Enhance 启动中...") while waiting for the daemon, so users are aware the plugin is working rather than assuming the shortcut is broken.

- 修复：终端获焦时 `activeTerminal` API 仍返回 `null` 的问题——现在回退到第一个可用终端，避免快捷键静默失效。
- 修复：PowerShell 检测结果在失败时被永久缓存的问题——启动时系统繁忙导致 `pwsh` 检测超时，错误结果被缓存后所有后续尝试均失败。现在仅在检测成功时才缓存结果。
- 修复：检测逻辑改为依次尝试 `pwsh` → `powershell`（每个超时 5 秒），仅缓存第一个可用项，兼容只有 PS5、只有 PS7 或两者并存的环境。
- 修复：集成终端在启动期间重新初始化时，可能导致 daemon 的 stdout pipe 被破坏。spawn 选项现在显式设置 `stdio: ['pipe', 'pipe', 'pipe']`，防止继承终端句柄。
- 修复：daemon spawn 失败（如 ENOENT）时，现在清除已缓存的 PS 可执行文件路径，使下次按快捷键能重新检测，而非永久失效。
- 修复：新增 `proc.on('error')` 处理器——spawn 错误现在会明确提示用户，而不再静默挂起。
- 修复：daemon 启动超时时间从 8 秒调整为 25 秒——系统负载高时首次加载 `System.Windows.Forms/Drawing` 程序集可能超过 10 秒。超时后自动 kill 进程、重置全部状态，并提示用户再按一次重试。
- 修复：超时后未 kill 残留的 daemon 进程，导致第二次按快捷键会再启动一个 PowerShell 实例，两个进程竞争写入全局状态变量造成混乱。
- 优化：等待 daemon 启动时状态栏显示旋转图标（"Paste Enhance 启动中..."），用户可明确感知插件正在工作，而非误以为快捷键失效。

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
