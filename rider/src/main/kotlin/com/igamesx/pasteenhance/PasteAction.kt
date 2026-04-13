package com.igamesx.pasteenhance

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.Component
import java.awt.Container
import java.awt.KeyboardFocusManager

class PasteAction : AnAction() {

    private val log = thisLogger()

    private val imageExts = setOf(
        "png", "jpg", "jpeg", "gif", "bmp", "webp", "tiff", "tif", "heic", "heif", "avif"
    )

    private fun isImagePath(path: String): Boolean =
        path.substringAfterLast('.', "").lowercase() in imageExts

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Priority 0: Files selected in IDE Project Explorer.
        // When focus is outside the terminal, activate the terminal window so we can
        // resolve TerminalView from the newly focused component — no warm-up needed.
        val ideSelectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (!ideSelectedFiles.isNullOrEmpty()) {
            val paths = ideSelectedFiles.joinToString(" ") { it.path }

            val directView = resolveTerminalView(e)
            if (directView != null) {
                sendPathsToTerminal(directView, paths)
            } else {
                // Activate terminal window; in the callback focus is inside the terminal,
                // so DataManager can resolve TerminalView via DATA_KEY.
                val toolWindow = ToolWindowManager.getInstance(project)
                    .getToolWindow("Terminal") ?: run {
                    log.warn("PasteEnhance: Terminal tool window not found"); return
                }
                toolWindow.activate {
                    val view = resolveTerminalViewFromFocus()
                        ?: resolveTerminalFromToolWindow(project)
                    if (view != null) {
                        sendPathsToTerminal(view, paths)
                    } else {
                        log.warn("PasteEnhance: could not resolve TerminalView after activation")
                    }
                }
            }
            return
        }

        // Priority 1: Files copied from OS Explorer (javaFileListFlavor).
        // Priority 2: Screenshot bitmap → save PNG → return path.
        val textToSend: String? =
            ClipboardHelper.getFilePaths() ?: ClipboardHelper.saveImageToTemp()

        val terminalView = resolveTerminalView(e)

        if (textToSend != null) {
            if (terminalView == null) {
                log.warn("PasteEnhance: no terminal view found"); return
            }
            sendPathsToTerminal(terminalView, textToSend)
        } else {
            val clipText = ClipboardHelper.getText()
            if (clipText != null && terminalView != null) {
                invokeTerminalSendText(terminalView, clipText)
            } else {
                ActionManager.getInstance()
                    .getAction(IdeActions.ACTION_PASTE)
                    ?.actionPerformed(e)
            }
        }
    }

    /** Handles single and multi-path sends, inserting 300ms gaps around image paths. */
    private fun sendPathsToTerminal(terminalView: Any, textToSend: String) {
        val paths = textToSend.split(" ")
        val hasImage = paths.any { isImagePath(it) }
        if (paths.size > 1 && hasImage) {
            Thread {
                for (i in paths.indices) {
                    val chunk = (if (i == 0) "" else " ") + paths[i]
                    ApplicationManager.getApplication().invokeAndWait {
                        invokeTerminalSendText(terminalView, chunk)
                    }
                    if (i < paths.size - 1 &&
                        (isImagePath(paths[i]) || isImagePath(paths[i + 1]))) {
                        Thread.sleep(300)
                    }
                }
            }.apply { isDaemon = true }.start()
        } else {
            invokeTerminalSendText(terminalView, textToSend)
        }
    }

    /**
     * Resolves TerminalView from the action's DataContext.
     * Succeeds only when focus is currently inside the terminal panel.
     */
    private fun resolveTerminalView(e: AnActionEvent): Any? {
        return try {
            getTerminalDataKey()?.let { e.dataContext.getData(it) }
        } catch (ex: Exception) {
            log.warn("PasteEnhance: resolveTerminalView failed", ex)
            null
        }
    }

    /**
     * Resolves TerminalView from the currently focused AWT component.
     * Called inside toolWindow.activate{} callback, where the terminal has just been
     * focused — DataManager can populate DATA_KEY from the focused component.
     */
    private fun resolveTerminalViewFromFocus(): Any? {
        return try {
            val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
                ?: return null
            val dataKey = getTerminalDataKey() ?: return null
            DataManager.getInstance().getDataContext(focusOwner).getData(dataKey)
        } catch (ex: Exception) {
            log.warn("PasteEnhance: resolveTerminalViewFromFocus failed", ex)
            null
        }
    }

    /**
     * Last-resort: traverse the Terminal tool window's Swing component tree.
     * Works only if TerminalView extends JComponent (not guaranteed).
     */
    private fun resolveTerminalFromToolWindow(project: com.intellij.openapi.project.Project): Any? {
        return try {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Terminal")
                ?: return null
            val content = toolWindow.contentManager.selectedContent ?: return null
            val terminalViewClass = Class.forName("com.intellij.terminal.frontend.view.TerminalView")
            findComponentByClass(content.component, terminalViewClass)
        } catch (ex: Exception) {
            log.warn("PasteEnhance: resolveTerminalFromToolWindow failed", ex)
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun getTerminalDataKey(): com.intellij.openapi.actionSystem.DataKey<Any>? {
        return try {
            val cls = Class.forName("com.intellij.terminal.frontend.view.TerminalView")
            val companion = cls.getField("Companion").get(null)
            companion.javaClass.getMethod("getDATA_KEY").invoke(companion)
                as? com.intellij.openapi.actionSystem.DataKey<Any>
        } catch (ex: Exception) {
            null
        }
    }

    private fun findComponentByClass(component: Component, targetClass: Class<*>): Any? {
        if (targetClass.isInstance(component)) return component
        if (component is Container) {
            for (child in component.components) {
                val found = findComponentByClass(child, targetClass)
                if (found != null) return found
            }
        }
        return null
    }

    private fun invokeTerminalSendText(terminalView: Any, text: String) {
        try {
            terminalView.javaClass.getMethod("sendText", String::class.java)
                .invoke(terminalView, text)
        } catch (ex: Exception) {
            log.warn("PasteEnhance: sendText failed", ex)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }
}
