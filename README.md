# Paste Enhance

Enhanced paste for Cursor terminal — works with any terminal session, including Claude Code.

---

## Features

### 🖼️ Paste Screenshot

Take a screenshot and press the shortcut key — the image is automatically saved as a temporary file and its path is sent to the terminal, ready to use without manually saving the file first.

### 📁 Paste Multiple Files

Copy files from Explorer and press the shortcut key — all file paths are sent to the terminal at once.

### 📋 Normal Text Paste

When the clipboard contains plain text, it is pasted normally.

---

## Usage

1. Open a terminal in Cursor
2. Take a screenshot or copy files in Explorer
3. Press `Ctrl+Alt+V` in the terminal

---

## Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `pasteEnhance.tempImageDir` | `%USERPROFILE%\.claude\clipTemp` | Directory for temporary image files |
| `pasteEnhance.maxCachedImages` | `20` | Max number of cached images |

---

## Requirements

- **Windows only** — relies on PowerShell and Windows clipboard APIs
- Designed for use with **Cursor** — works with any terminal session, including **Claude Code**

---

## Changelog

### 0.0.6
- Fixed: Minor bug fixes.

### 0.0.5
- Fixed: `activeTerminal` API returning `null` in Cursor even when a terminal is focused — the plugin now falls back to the first available terminal, preventing silent no-op on shortcut press.
- Fixed: PowerShell detection result being permanently cached on failure — when the system is busy at startup and `pwsh` detection times out, the bad result was cached and caused all subsequent attempts to fail. Detection is now only cached on success.
- Fixed: Detection now tries `pwsh` then `powershell` sequentially (5 s timeout each) and caches only the first working one, compatible with PS5-only, PS7-only, or both environments.
- Fixed: Daemon stdout pipe potentially corrupted when Cursor's integrated terminal reinitializes during startup. Spawn options now explicitly set `stdio: ['pipe', 'pipe', 'pipe']` to prevent terminal handle inheritance.
- Fixed: On daemon spawn failure (e.g. ENOENT), the cached PS executable is cleared so the next shortcut press re-detects correctly instead of failing permanently.
- Fixed: Added `proc.on('error')` handler — spawn errors are now surfaced to the user instead of silently hanging.
- Fixed: Daemon startup timeout extended from 8 s to 25 s — `Add-Type -AssemblyName System.Windows.Forms/Drawing` can take over 10 s on first load under high system load. On timeout the process is killed, all state is reset, and the user is prompted to retry.
- Fixed: After a timeout the dangling daemon process was not killed, causing a second shortcut press to spawn a second PowerShell instance and corrupt global state.
- Improved: Status bar shows a spinner ("Paste Enhance 启动中...") while waiting for the daemon.

### 0.0.4
- Performance: Changed activation event to `onStartupFinished` — daemon pre-warms at startup, eliminating first-paste delay.
- Performance: PowerShell version detection is now asynchronous, no longer blocking the main thread.

### 0.0.3
- Added: `CHANGELOG.md` to display version history on Open VSX Registry.

### 0.0.2
- Fixed: Plugin did not work on machines where PowerShell 5 execution policy was `Restricted` (default on new Windows installations). The daemon now starts with `-ExecutionPolicy Bypass`, requiring no manual configuration from the user.
- Fixed: Hardcoded PowerShell 5 path replaced with dynamic detection — the plugin now prefers PowerShell 7 (`pwsh`) if available, falling back to PowerShell 5 (`powershell`).

### 0.0.1
- Initial release

---
---

## 功能

### 🖼️ 截图直接粘贴

截图后按快捷键，图片自动保存为临时文件并将路径发送到终端，无需手动保存文件。

### 📁 批量文件粘贴

在资源管理器中复制文件后按快捷键，所有文件路径一次性发送到终端。

### 📋 普通文本粘贴

剪贴板为普通文本时，正常粘贴文本内容。

---

## 使用方法

1. 在 Cursor 中打开终端
2. 截图或在资源管理器中复制文件
3. 在终端中按 `Ctrl+Alt+V`

---

## 设置

| 设置项 | 默认值 | 说明 |
|--------|--------|------|
| `pasteEnhance.tempImageDir` | `%USERPROFILE%\.claude\clipTemp` | 临时图片存放目录 |
| `pasteEnhance.maxCachedImages` | `20` | 最多保留的图片数量 |

---

## 环境要求

- 仅支持 **Windows** 平台
- 需要配合 **Cursor** 使用，适用于任意终端会话，包括 **Claude Code**

---

## 更新日志

### 0.0.6
- 修复：修复一些错误。

### 0.0.5
- 修复：在 Cursor 中终端获焦时 `activeTerminal` API 仍返回 `null` 的问题——现在回退到第一个可用终端，避免快捷键静默失效。
- 修复：PowerShell 检测结果在失败时被永久缓存的问题——系统繁忙时 `pwsh` 检测超时，错误结果被缓存后所有后续尝试均失败。现在仅在检测成功时才缓存结果。
- 修复：检测逻辑改为依次尝试 `pwsh` → `powershell`（每个超时 5 秒），仅缓存第一个可用项，兼容只有 PS5、只有 PS7 或两者并存的环境。
- 修复：Cursor 集成终端在启动期间重新初始化时，可能导致 daemon 的 stdout pipe 被破坏。spawn 选项现在显式设置 `stdio: ['pipe', 'pipe', 'pipe']`，防止继承终端句柄。
- 修复：daemon spawn 失败（如 ENOENT）时，现在清除已缓存的 PS 可执行文件路径，下次按快捷键能重新检测，而非永久失效。
- 修复：新增 `proc.on('error')` 处理器——spawn 错误现在会明确提示用户，而不再静默挂起。
- 修复：daemon 启动超时时间从 8 秒调整为 25 秒——系统负载高时首次加载 `System.Windows.Forms/Drawing` 程序集可能超过 10 秒。超时后自动 kill 进程、重置状态，并提示用户再按一次重试。
- 修复：超时后未 kill 残留的 daemon 进程，导致第二次按快捷键会再启动一个 PowerShell 实例，两进程竞争写入全局状态变量造成混乱。
- 优化：等待 daemon 启动时状态栏显示旋转图标（"Paste Enhance 启动中..."），用户可明确感知插件正在工作。

### 0.0.4
- 性能优化：激活事件改为 `onStartupFinished`，Daemon 在启动时预热，消除首次粘贴延迟。
- 性能优化：PowerShell 版本检测改为异步，不再阻塞主线程。

### 0.0.3
- 新增：`CHANGELOG.md` 文件，以支持在 Open VSX Registry 上展示版本更新历史。

### 0.0.2
- 修复：在 PowerShell 5 执行策略为 `Restricted` 的新电脑上（Windows 默认设置），插件无法正常工作的问题。现在启动 daemon 时自动附加 `-ExecutionPolicy Bypass`，用户无需任何手动配置。
- 修复：移除硬编码的 PowerShell 5 路径，改为动态检测——优先使用 PowerShell 7（`pwsh`），不存在时回退到 PowerShell 5（`powershell`）。

### 0.0.1
- 初始版本发布
