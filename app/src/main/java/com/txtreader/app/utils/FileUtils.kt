package com.txtreader.app.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object FileUtils {

    /**
     * 从 URI 获取文件名
     */
    fun getFileName(context: Context, uri: Uri): String {
        var name = "未知书名"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx)
                }
            }
        } catch (e: Exception) {
            name = uri.lastPathSegment ?: "未知书名"
        }
        // 去掉扩展名
        return name.removeSuffix(".txt").removeSuffix(".TXT")
    }

    /**
     * 从 URI 获取文件大小
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0) size = cursor.getLong(idx)
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return size
    }

    /**
     * 格式化文件大小显示
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)}KB"
            else -> "${"%.1f".format(bytes / 1024.0 / 1024.0)}MB"
        }
    }

    /**
     * 格式化字数显示
     */
    fun formatCharCount(count: Long): String {
        return when {
            count < 10000 -> "${count}字"
            else -> "${"%.1f".format(count / 10000.0)}万字"
        }
    }

    /**
     * 格式化时间显示
     */
    fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return "未读"
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "刚刚"
            diff < 3600_000 -> "${diff / 60_000}分钟前"
            diff < 86400_000 -> "${diff / 3600_000}小时前"
            diff < 2 * 86400_000 -> "昨天"
            diff < 7 * 86400_000 -> "${diff / 86400_000}天前"
            else -> {
                val sdf = java.text.SimpleDateFormat("MM-dd", java.util.Locale.CHINA)
                sdf.format(java.util.Date(timestamp))
            }
        }
    }
}
