package com.txtreader.app.ui.reader

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.txtreader.app.R
import com.txtreader.app.data.entity.Chapter
import com.txtreader.app.databinding.ItemChapterBinding

class ChapterListAdapter(
    private val chapters: List<Chapter>,
    private val currentIndex: Int,
    private val onChapterClick: (Int) -> Unit
) : RecyclerView.Adapter<ChapterListAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemChapterBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChapterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chapter = chapters[position]
        holder.binding.apply {
            tvChapterIndex.text = "${position + 1}"
            tvChapterTitle.text = chapter.title
            val isCurrent = position == currentIndex
            val color = if (isCurrent) {
                ContextCompat.getColor(root.context, R.color.accent)
            } else {
                ContextCompat.getColor(root.context, android.R.color.primary_text_light)
            }
            tvChapterTitle.setTextColor(color)
            tvChapterIndex.setTextColor(color)
            root.setOnClickListener { onChapterClick(position) }
        }
    }

    override fun getItemCount() = chapters.size
}
