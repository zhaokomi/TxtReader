package com.txtreader.app.ui.bookshelf

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.txtreader.app.data.db.AppDatabase
import com.txtreader.app.data.entity.Book
import com.txtreader.app.utils.FileUtils
import com.txtreader.app.utils.TxtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookshelfViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val bookDao = db.bookDao()
    private val chapterDao = db.chapterDao()

    val books: LiveData<List<Book>> = bookDao.getAllBooks()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState

    sealed class ImportState {
        object Idle : ImportState()
        object Parsing : ImportState()
        data class Success(val title: String, val chapterCount: Int) : ImportState()
        data class Error(val message: String) : ImportState()
        object AlreadyExists : ImportState()
    }

    /**
     * 导入 TXT 文件
     */
    fun importBook(uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportState.Parsing
            try {
                val context = getApplication<Application>()

                // 持久化权限（SAF）
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) { /* ignore */ }

                val uriStr = uri.toString()

                // 检查是否已存在
                val existing = withContext(Dispatchers.IO) { bookDao.getBookByPath(uriStr) }
                if (existing != null) {
                    _importState.value = ImportState.AlreadyExists
                    return@launch
                }

                val title = FileUtils.getFileName(context, uri)
                val fileSize = FileUtils.getFileSize(context, uri)

                // 先插入书籍（获取 ID）
                val randomColors = listOf(
                    0xFF6B4C9A.toInt(), 0xFF2E86AB.toInt(), 0xFFA23B72.toInt(),
                    0xFF3BB273.toInt(), 0xFFE94F37.toInt(), 0xFF393E41.toInt(),
                    0xFFF7B731.toInt(), 0xFF3867D6.toInt()
                )
                val coverColor = randomColors.random()

                val book = Book(
                    title = title,
                    filePath = uriStr,
                    fileSize = fileSize,
                    coverColor = coverColor
                )

                val bookId = withContext(Dispatchers.IO) { bookDao.insertBook(book) }

                // 解析章节
                val result = TxtParser.parseBook(context, uri, bookId)

                // 保存章节
                withContext(Dispatchers.IO) {
                    chapterDao.insertChapters(result.chapters)
                    // 更新书籍元数据
                    bookDao.updateBook(
                        book.copy(
                            id = bookId,
                            encoding = result.encoding,
                            totalChapters = result.chapters.size,
                            totalCharacters = result.totalCharacters
                        )
                    )
                }

                _importState.value = ImportState.Success(title, result.chapters.size)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "导入失败")
            }
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            bookDao.deleteBook(book)
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }
}
