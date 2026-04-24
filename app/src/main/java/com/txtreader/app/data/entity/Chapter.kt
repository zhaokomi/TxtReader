package com.txtreader.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 章节实体
 */
@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = Book::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("bookId")]
)
data class Chapter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,           // 所属书籍ID
    val index: Int,             // 章节序号（0-based）
    val title: String,          // 章节标题
    val startOffset: Long,      // 在文件中的字节偏移（起始）
    val endOffset: Long,        // 在文件中的字节偏移（结束）
    val charCount: Int = 0      // 字符数
)
