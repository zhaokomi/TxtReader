package com.txtreader.app.ui.reader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.txtreader.app.data.db.AppDatabase
import com.txtreader.app.data.entity.Book
import com.txtreader.app.data.entity.Chapter
import com.txtreader.app.utils.TxtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val bookDao = db.bookDao()
    private val chapterDao = db.chapterDao()

    private val _readerState = MutableStateFlow<ReaderState>(ReaderState.Loading)
    val readerState: StateFlow<ReaderState> = _readerState

    private val _chapterContent = MutableStateFlow<ChapterContent?>(null)
    val chapterContent: StateFlow<ChapterContent?> = _chapterContent

    private val _chapterList = MutableStateFlow<List<Chapter>>(emptyList())
    val chapterList: StateFlow<List<Chapter>> = _chapterList

    var currentBook: Book? = null
        private set
    var currentChapterIndex: Int = 0
        private set

    sealed class ReaderState {
        object Loading : ReaderState()
        data class Ready(val book: Book) : ReaderState()
        data class Error(val message: String) : ReaderState()
    }

    data class ChapterContent(
        val chapter: Chapter,
        val content: String,
        val hasPrev: Boolean,
        val hasNext: Boolean
    )

    /**
     * 加载书籍
     */
    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            try {
                val book = withContext(Dispatchers.IO) { bookDao.getBookById(bookId) }
                if (book == null) {
                    _readerState.value = ReaderState.Error("找不到该书籍")
                    return@launch
                }
                currentBook = book
                currentChapterIndex = book.lastChapterIndex

                // 加载章节列表
                val chapters = withContext(Dispatchers.IO) {
                    chapterDao.getChaptersByBook(bookId)
                }
                _chapterList.value = chapters

                _readerState.value = ReaderState.Ready(book)

                // 加载当前章节内容
                loadChapter(currentChapterIndex, book.lastScrollPosition)
            } catch (e: Exception) {
                _readerState.value = ReaderState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 跳转到指定章节
     */
    fun loadChapter(index: Int, scrollPos: Int = 0) {
        viewModelScope.launch {
            val book = currentBook ?: return@launch
            val chapters = _chapterList.value
            if (chapters.isEmpty()) return@launch

            val safeIndex = index.coerceIn(0, chapters.size - 1)
            currentChapterIndex = safeIndex
            val chapter = chapters[safeIndex]

            val content = TxtParser.readChapterContent(
                context = getApplication(),
                uri = Uri.parse(book.filePath),
                startOffset = chapter.startOffset,
                endOffset = chapter.endOffset,
                encoding = book.encoding
            )

            _chapterContent.value = ChapterContent(
                chapter = chapter,
                content = content.trim(),
                hasPrev = safeIndex > 0,
                hasNext = safeIndex < chapters.size - 1
            )
        }
    }

    fun prevChapter() {
        if (currentChapterIndex > 0) loadChapter(currentChapterIndex - 1)
    }

    fun nextChapter() {
        val chapters = _chapterList.value
        if (currentChapterIndex < chapters.size - 1) loadChapter(currentChapterIndex + 1)
    }

    /**
     * 保存阅读进度
     */
    fun saveProgress(scrollPosition: Int) {
        val book = currentBook ?: return
        val chapters = _chapterList.value
        if (chapters.isEmpty()) return

        val progress = if (chapters.size <= 1) {
            0f
        } else {
            currentChapterIndex.toFloat() / (chapters.size - 1).toFloat()
        }

        viewModelScope.launch(Dispatchers.IO) {
            bookDao.updateReadProgress(
                bookId = book.id,
                chapterIndex = currentChapterIndex,
                scrollPos = scrollPosition,
                progress = progress
            )
        }
    }
}
