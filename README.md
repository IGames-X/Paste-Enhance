# Paste Enhance

Enhanced paste for Claude in Cursor terminal.

---

## Features

### 🖼️ Paste Screenshot

Take a screenshot and press the shortcut key — the image is automatically saved as a temporary file and its path is sent to the terminal. Claude can recognize it immediately without manually saving the file first.

### 📁 Paste Multiple Files

Copy files from Explorer and press the shortcut key — all file paths are sent to the terminal at once for Claude to process.

### 📋 Normal Text Paste

When the clipboard contains plain text, it is pasted normally.

---

## Usage

1. Open a terminal in Cursor and start Claude Code
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
- Designed for use with **Cursor** + **Claude Code**

---
---

## 功能

### 🖼️ 截图直接粘贴

截图后按快捷键，图片自动保存为临时文件并将路径发送到终端，Claude 可直接识别，无需手动保存。

### 📁 批量文件粘贴

在资源管理器中复制文件后按快捷键，所有文件路径一次性发送到终端供 Claude 处理。

### 📋 普通文本粘贴

剪贴板为普通文本时，正常粘贴文本内容。

---

## 使用方法

1. 在 Cursor 中打开终端并启动 Claude Code
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
- 需要配合 **Cursor** + **Claude Code** 使用
