package com.igamesx.pasteenhance

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO

object ClipboardHelper {

    private val clipDir: File by lazy {
        val dir = File(System.getProperty("user.home"), ".claude/clipTemp")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    /**
     * Priority 1: Files copied from Explorer.
     * Returns space-separated absolute paths, or null if clipboard has no file drop.
     */
    fun getFilePaths(): String? {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val transferable = clipboard.getContents(null) ?: return null
            if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return null
            @Suppress("UNCHECKED_CAST")
            val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
            if (files.isNullOrEmpty()) return null
            files.joinToString(" ") { it.absolutePath }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Priority 2: Bitmap image (screenshot).
     * Saves PNG to clipTemp dir and returns the file path, or null if no image in clipboard.
     * Keeps at most [maxImages] files in the dir (deletes oldest first).
     */
    fun saveImageToTemp(maxImages: Int = 20): String? {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val transferable = clipboard.getContents(null) ?: return null
            if (!transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) return null

            val image = transferable.getTransferData(DataFlavor.imageFlavor) ?: return null

            // Convert to BufferedImage (ARGB) then encode as PNG bytes
            val src = image as? java.awt.Image ?: return null
            val bmp = BufferedImage(src.getWidth(null), src.getHeight(null), BufferedImage.TYPE_INT_ARGB)
            val g = bmp.createGraphics()
            g.drawImage(src, 0, 0, null)
            g.dispose()

            val baos = ByteArrayOutputStream()
            ImageIO.write(bmp, "png", baos)
            val bytes = baos.toByteArray()

            // MD5 hash first 12 hex chars — matches daemon.ps1 naming convention
            val md5 = MessageDigest.getInstance("MD5")
            val hashBytes = md5.digest(bytes)
            val hash = hashBytes.joinToString("") { "%02x".format(it) }.substring(0, 12)

            val outFile = File(clipDir, "claude_img_$hash.png")
            if (!outFile.exists()) {
                outFile.writeBytes(bytes)
                // Evict oldest files if over limit
                val existing = clipDir.listFiles { f -> f.name.matches(Regex("claude_img_[0-9a-f]{12}\\.png")) }
                    ?.sortedBy { it.lastModified() } ?: emptyList()
                if (existing.size > maxImages) {
                    existing.take(existing.size - maxImages).forEach { it.delete() }
                }
            }
            outFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns clipboard plain text, or null if clipboard has no text.
     */
    fun getText(): String? {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val transferable = clipboard.getContents(null) ?: return null
            if (!transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) return null
            transferable.getTransferData(DataFlavor.stringFlavor) as? String
        } catch (_: Exception) {
            null
        }
    }
}
