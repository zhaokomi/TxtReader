package com.txtreader.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.txtreader.app.data.dao.BookDao
import com.txtreader.app.data.dao.ChapterDao
import com.txtreader.app.data.entity.Book
import com.txtreader.app.data.entity.Chapter

@Database(
    entities = [Book::class, Chapter::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "txtreader.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
