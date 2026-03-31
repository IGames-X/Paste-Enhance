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
