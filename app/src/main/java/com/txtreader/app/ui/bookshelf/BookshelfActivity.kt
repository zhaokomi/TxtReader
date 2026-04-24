package com.txtreader.app.ui.bookshelf

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.txtreader.app.R
import com.txtreader.app.data.entity.Book
import com.txtreader.app.databinding.ActivityBookshelfBinding
import com.txtreader.app.ui.reader.ReaderActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BookshelfActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookshelfBinding
    private val viewModel: BookshelfViewModel by viewModels()
    private lateinit var adapter: BookAdapter

    // 文件选择器
    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBook(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookshelfBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        observeViewModel()
        setupFab()
    }

    private fun setupRecyclerView() {
        adapter = BookAdapter(
            onBookClick = { book -> openReader(book) },
            onBookLongClick = { book -> showDeleteDialog(book) }
        )
        binding.rvBooks.apply {
            layoutManager = GridLayoutManager(this@BookshelfActivity, 2)
            adapter = this@BookshelfActivity.adapter
        }
    }

    private fun observeViewModel() {
        // 书籍列表
        viewModel.books.observe(this) { books ->
            adapter.submitList(books)
            binding.layoutEmpty.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
            binding.rvBooks.visibility = if (books.isEmpty()) View.GONE else View.VISIBLE
        }

        // 导入状态
        lifecycleScope.launch {
            viewModel.importState.collectLatest { state ->
                when (state) {
                    is BookshelfViewModel.ImportState.Idle -> {
                        binding.layoutImporting.visibility = View.GONE
                    }
                    is BookshelfViewModel.ImportState.Parsing -> {
                        binding.layoutImporting.visibility = View.VISIBLE
                        binding.tvImportStatus.text = "正在解析..."
                    }
                    is BookshelfViewModel.ImportState.Success -> {
                        binding.layoutImporting.visibility = View.GONE
                        Toast.makeText(
                            this@BookshelfActivity,
                            "《${state.title}》导入成功，共${state.chapterCount}章",
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.resetImportState()
                    }
                    is BookshelfViewModel.ImportState.Error -> {
                        binding.layoutImporting.visibility = View.GONE
                        Toast.makeText(
                            this@BookshelfActivity,
                            "导入失败：${state.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetImportState()
                    }
                    is BookshelfViewModel.ImportState.AlreadyExists -> {
                        binding.layoutImporting.visibility = View.GONE
                        Toast.makeText(this@BookshelfActivity, "该书已在书架中", Toast.LENGTH_SHORT).show()
                        viewModel.resetImportState()
                    }
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabImport.setOnClickListener {
            pickFileLauncher.launch(arrayOf("text/plain", "*/*"))
        }
    }

    private fun openReader(book: Book) {
        val intent = Intent(this, ReaderActivity::class.java)
        intent.putExtra(ReaderActivity.EXTRA_BOOK_ID, book.id)
        startActivity(intent)
    }

    private fun showDeleteDialog(book: Book) {
        AlertDialog.Builder(this)
            .setTitle("删除书籍")
            .setMessage("确认从书架中删除《${book.title}》？\n（不会删除原文件）")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteBook(book)
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bookshelf, menu)
        return true
    }
}
