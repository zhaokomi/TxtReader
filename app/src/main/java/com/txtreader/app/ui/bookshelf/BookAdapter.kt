package com.txtreader.app.ui.bookshelf

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.txtreader.app.data.entity.Book
import com.txtreader.app.databinding.ItemBookBinding
import com.txtreader.app.utils.FileUtils

class BookAdapter(
    private val onBookClick: (Book) -> Unit,
    private val onBookLongClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.apply {
                // 封面颜色和首字
                viewCover.setBackgroundColor(book.coverColor)
                tvCoverChar.text = book.title.firstOrNull()?.toString() ?: "书"

                tvTitle.text = book.title
                tvMeta.text = buildString {
                    if (book.totalChapters > 0) append("${book.totalChapters}章 · ")
                    if (book.totalCharacters > 0) append(FileUtils.formatCharCount(book.totalCharacters))
                }
                tvLastRead.text = if (book.lastReadTime > 0) {
                    "上次：${FileUtils.formatTime(book.lastReadTime)}"
                } else {
                    "未读"
                }

                // 进度条
                progressRead.progress = (book.readProgress * 100).toFloat()
                tvProgress.text = if (book.readProgress > 0) {
                    "${"%.0f".format(book.readProgress * 100)}%"
                } else ""

                root.setOnClickListener { onBookClick(book) }
                root.setOnLongClickListener {
                    onBookLongClick(book)
                    true
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Book, newItem: Book) = oldItem == newItem
    }
}
