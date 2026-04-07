package com.igamesx.pasteenhance

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger

class PasteAction : AnAction() {

    private val log = thisLogger()

    private val imageExts = setOf(
        "png", "jpg", "jpeg", "gif", "bmp", "webp", "tiff", "tif", "heic", "heif", "avif"
    )

    private fun isImagePath(path: String): Boolean =
        path.substringAfterLast('.', "").lowercase() in imageExts

    override fun actionPerformed(e: AnActionEvent) {
        e.project ?: return

        val textToSend: String? =
            ClipboardHelper.getFilePaths()
                ?: ClipboardHelper.saveImageToTemp()

        val terminalView = resolveTerminalView(e)

        if (textToSend != null) {
            if (terminalView == null) {
                log.warn("PasteEnhance: no terminal view found")
                return
            }

            val paths = textToSend.split(" ")
            val hasImage = paths.any { isImagePath(it) }

            if (paths.size > 1 && hasImage) {
                // Multiple paths containing images: send sequentially with 300ms delay
                // around image paths so Claude Code can recognize each as [Image #N].
                // invokeAndWait ensures ordering; sleep happens on background thread.
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
        } else {
            // Clipboard is plain text — send it directly to the terminal via sendText,
            // same as VSCode does. Fall back to ACTION_PASTE only if no terminal is focused.
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

    /**
     * Rider 2026.1 uses com.intellij.terminal.frontend.view.TerminalView (new block terminal).
     * The active view is available via its DATA_KEY from the action's DataContext.
     */
    private fun resolveTerminalView(e: AnActionEvent): Any? {
        return try {
            val terminalViewClass = Class.forName("com.intellij.terminal.frontend.view.TerminalView")
            val companion = terminalViewClass.getField("Companion").get(null)
            @Suppress("UNCHECKED_CAST")
            val dataKey = companion.javaClass.getMethod("getDATA_KEY").invoke(companion)
                as? com.intellij.openapi.actionSystem.DataKey<Any> ?: return null
            e.dataContext.getData(dataKey)
        } catch (ex: Exception) {
            log.warn("PasteEnhance: resolveTerminalView failed", ex)
            null
        }
    }

    /** Calls TerminalView.sendText(String) — types text into prompt without executing. */
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
