package com.txtreader.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.txtreader.app.data.entity.Book

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY lastReadTime DESC, addedTime DESC")
    fun getAllBooks(): LiveData<List<Book>>

    @Query("SELECT * FROM books ORDER BY lastReadTime DESC, addedTime DESC")
    suspend fun getAllBooksSync(): List<Book>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): Book?

    @Query("SELECT * FROM books WHERE filePath = :path LIMIT 1")
    suspend fun getBookByPath(path: String): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    /** 更新阅读进度 */
    @Query("""
        UPDATE books 
        SET lastChapterIndex = :chapterIndex,
            lastScrollPosition = :scrollPos,
            readProgress = :progress,
            lastReadTime = :readTime
        WHERE id = :bookId
    """)
    suspend fun updateReadProgress(
        bookId: Long,
        chapterIndex: Int,
        scrollPos: Int,
        progress: Float,
        readTime: Long = System.currentTimeMillis()
    )
}
