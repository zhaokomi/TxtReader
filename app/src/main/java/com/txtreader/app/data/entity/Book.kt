package com.txtreader.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 书籍实体 - 书架上每本书的信息
 */
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,           // 书名
    val filePath: String,        // 文件路径（URI 字符串）
    val fileSize: Long = 0,      // 文件大小（字节）
    val encoding: String = "UTF-8", // 文件编码
    val totalChapters: Int = 0,  // 总章节数
    val totalCharacters: Long = 0, // 总字数
    val coverColor: Int = 0,     // 封面颜色（随机生成）
    val addedTime: Long = System.currentTimeMillis(), // 添加时间
    val lastReadTime: Long = 0,  // 最后阅读时间
    // 阅读进度
    val lastChapterIndex: Int = 0,   // 最后阅读章节索引
    val lastScrollPosition: Int = 0, // 章节内滚动位置（像素）
    val readProgress: Float = 0f     // 总体阅读进度 0.0~1.0
)
