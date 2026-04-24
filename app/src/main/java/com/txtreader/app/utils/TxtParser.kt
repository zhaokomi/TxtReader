package com.txtreader.app.utils

import android.content.Context
import android.net.Uri
import com.txtreader.app.data.entity.Chapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset

/**
 * TXT 文件解析工具
 * - 自动检测编码（UTF-8 / GBK / GB2312）
 * - 章节分割（正则匹配常见章节标题格式）
 */
object TxtParser {

    // 章节标题匹配正则（覆盖常见网文格式）
    private val CHAPTER_PATTERNS = listOf(
        Regex("""^第[零一二三四五六七八九十百千万\d]+[章节回卷集部][^\n]{0,30}$"""),
        Regex("""^Chapter\s+\d+[^\n]{0,30}$""", RegexOption.IGNORE_CASE),
        Regex("""^\d+[、\.\s][^\n]{2,30}$"""),
        Regex("""^【[^\n]{1,30}】$"""),
        Regex("""^[（(][^\n]{1,20}[）)]$""")
    )

    /**
     * 检测文件编码
     */
    fun detectEncoding(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = ByteArray(4096)
                val read = stream.read(bytes)
                if (read < 3) return "UTF-8"
                // BOM 检测
                when {
                    bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
                        "UTF-8"
                    bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> "UTF-16LE"
                    bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> "UTF-16BE"
                    else -> detectByContent(bytes, read)
                }
            } ?: "UTF-8"
        } catch (e: Exception) {
            "UTF-8"
        }
    }

    private fun detectByContent(bytes: ByteArray, length: Int): String {
        // 简单的 UTF-8 合法性检测
        var i = 0
        var invalidUtf8 = false
        while (i < length) {
            val b = bytes[i].toInt() and 0xFF
            when {
                b <= 0x7F -> i++
                b in 0xC2..0xDF -> {
                    if (i + 1 >= length || (bytes[i + 1].toInt() and 0xC0) != 0x80) {
                        invalidUtf8 = true; break
                    }
                    i += 2
                }
                b in 0xE0..0xEF -> {
                    if (i + 2 >= length) { i++; continue }
                    if ((bytes[i + 1].toInt() and 0xC0) != 0x80 ||
                        (bytes[i + 2].toInt() and 0xC0) != 0x80) {
                        invalidUtf8 = true; break
                    }
                    i += 3
                }
                else -> { invalidUtf8 = true; break }
            }
        }
        return if (invalidUtf8) "GBK" else "UTF-8"
    }

    /**
     * 读取章节内容（通过偏移量）
     */
    suspend fun readChapterContent(
        context: Context,
        uri: Uri,
        startOffset: Long,
        endOffset: Long,
        encoding: String
    ): String = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.skip(startOffset)
                val length = (endOffset - startOffset).toInt().coerceAtLeast(0)
                val bytes = ByteArray(length)
                var totalRead = 0
                while (totalRead < length) {
                    val read = stream.read(bytes, totalRead, length - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                String(bytes, 0, totalRead, Charset.forName(encoding))
            } ?: ""
        } catch (e: Exception) {
            "读取失败：${e.message}"
        }
    }

    /**
     * 解析书籍：提取章节列表
     * @return Pair<编码, 章节列表>
     */
    suspend fun parseBook(
        context: Context,
        uri: Uri,
        bookId: Long
    ): ParseResult = withContext(Dispatchers.IO) {

        val encoding = detectEncoding(context, uri)
        val chapters = mutableListOf<Chapter>()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(
                    InputStreamReader(inputStream, Charset.forName(encoding)),
                    8 * 1024
                )

                val titleLines = mutableListOf<Pair<String, Long>>() // title -> byteOffset
                var byteOffset = 0L
                var lineBuffer = StringBuilder()
                var totalChars = 0L
                var charBuffer = CharArray(1024)
                var read: Int

                // 第一遍：扫描章节标题及其字节偏移
                val rawStream = context.contentResolver.openInputStream(uri)!!
                // 跳过 BOM
                val bom = rawStream.read()
                var bomSize = 0L
                if (bom != -1) {
                    val b = bom.toByte()
                    if (b == 0xEF.toByte()) {
                        rawStream.read(); rawStream.read()
                        bomSize = 3
                    }
                }
                rawStream.close()

                // 使用字节流按行扫描
                scanLines(context, uri, encoding, bomSize) { lineText, startByte, endByte ->
                    totalChars += lineText.length
                    if (isChapterTitle(lineText.trim())) {
                        titleLines.add(Pair(lineText.trim(), startByte))
                    }
                }

                // 获取文件总大小
                val fileSize = getFileSize(context, uri)

                if (titleLines.isEmpty()) {
                    // 没有识别到章节，整本书作为一章
                    chapters.add(
                        Chapter(
                            bookId = bookId,
                            index = 0,
                            title = "正文",
                            startOffset = bomSize,
                            endOffset = fileSize,
                            charCount = totalChars.toInt()
                        )
                    )
                } else {
                    // 构建章节列表
                    titleLines.forEachIndexed { i, (title, start) ->
                        val end = if (i + 1 < titleLines.size) titleLines[i + 1].second else fileSize
                        chapters.add(
                            Chapter(
                                bookId = bookId,
                                index = i,
                                title = title,
                                startOffset = start,
                                endOffset = end,
                                charCount = ((end - start) / 2).toInt() // 估算
                            )
                        )
                    }
                }

                ParseResult(
                    encoding = encoding,
                    chapters = chapters,
                    totalCharacters = totalChars
                )
            } ?: ParseResult(encoding, emptyList(), 0)
        } catch (e: Exception) {
            ParseResult(encoding, emptyList(), 0, error = e.message)
        }
    }

    private fun scanLines(
        context: Context,
        uri: Uri,
        encoding: String,
        skipBytes: Long,
        onLine: (text: String, startByte: Long, endByte: Long) -> Unit
    ) {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            if (skipBytes > 0) stream.skip(skipBytes)

            val charset = Charset.forName(encoding)
            val isMultiByte = encoding.equals("GBK", true) || encoding.equals("GB2312", true)

            val bufSize = 65536
            val buf = ByteArray(bufSize)
            var bytePos = skipBytes
            var lineStart = skipBytes
            val lineBuf = StringBuilder()

            // 读取整个文件，按行切分并记录字节偏移
            while (true) {
                val n = stream.read(buf)
                if (n == -1) break

                // 将 buf[0..n] 解码后处理
                val text = String(buf, 0, n, charset)
                var charIdx = 0
                // 注意：字节到字符的映射不是线性的，这里用近似处理
                for (ch in text) {
                    val chBytes = ch.toString().toByteArray(charset).size.toLong()
                    if (ch == '\n') {
                        onLine(lineBuf.toString(), lineStart, bytePos + chBytes)
                        lineStart = bytePos + chBytes
                        lineBuf.clear()
                    } else {
                        lineBuf.append(ch)
                    }
                    bytePos += chBytes
                }
            }
            if (lineBuf.isNotEmpty()) {
                onLine(lineBuf.toString(), lineStart, bytePos)
            }
        }
    }

    private fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun isChapterTitle(line: String): Boolean {
        if (line.isBlank() || line.length > 50) return false
        return CHAPTER_PATTERNS.any { it.matches(line) }
    }

    data class ParseResult(
        val encoding: String,
        val chapters: List<Chapter>,
        val totalCharacters: Long,
        val error: String? = null
    )
}
