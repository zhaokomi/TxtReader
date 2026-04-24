package com.txtreader.app.ui.reader

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.txtreader.app.R
import com.txtreader.app.databinding.ActivityReaderBinding
import com.txtreader.app.databinding.DialogChapterListBinding
import com.txtreader.app.databinding.DialogReaderSettingsBinding
import com.txtreader.app.utils.ReaderPrefs
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK_ID = "book_id"
    }

    private lateinit var binding: ActivityReaderBinding
    private val viewModel: ReaderViewModel by viewModels()

    private val autoSaveHandler = Handler(Looper.getMainLooper())
    private var lastScrollY = 0
    private var isMenuVisible = false

    private val autoSaveRunnable = Runnable {
        viewModel.saveProgress(binding.scrollView.scrollY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 沉浸式状态栏
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        ReaderPrefs.load(this)
        applyTheme()

        val bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)
        if (bookId == -1L) {
            finish()
            return
        }

        setupScrollView()
        setupButtons()
        observeViewModel()

        viewModel.loadBook(bookId)
    }

    private fun setupScrollView() {
        binding.scrollView.gestureListener = object : GestureScrollView.GestureListener {
            override fun onSingleTap(x: Float, y: Float) {
                val w = binding.scrollView.width.toFloat()
                when {
                    x < w * 0.3f -> viewModel.prevChapter()
                    x > w * 0.7f -> viewModel.nextChapter()
                    else -> toggleMenu()
                }
            }

            override fun onScrollChanged(scrollY: Int) {
                lastScrollY = scrollY
                // 防抖保存进度
                autoSaveHandler.removeCallbacks(autoSaveRunnable)
                autoSaveHandler.postDelayed(autoSaveRunnable, 1500)
            }
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnChapterList.setOnClickListener { showChapterList() }
        binding.btnSettings.setOnClickListener { showSettings() }
        binding.btnPrevChapter.setOnClickListener { viewModel.prevChapter() }
        binding.btnNextChapter.setOnClickListener { viewModel.nextChapter() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.readerState.collectLatest { state ->
                when (state) {
                    is ReaderViewModel.ReaderState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    is ReaderViewModel.ReaderState.Ready -> {
                        binding.progressBar.visibility = View.GONE
                        binding.tvBookTitle.text = state.book.title
                    }
                    is ReaderViewModel.ReaderState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@ReaderActivity, state.message, Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.chapterContent.collectLatest { content ->
                content ?: return@collectLatest

                // 设置章节标题和正文
                binding.tvChapterTitle.text = content.chapter.title
                binding.tvContent.text = content.content

                // 更新进度条
                val chapters = viewModel.chapterList.value
                val progress = if (chapters.size <= 1) 0f
                    else viewModel.currentChapterIndex.toFloat() / (chapters.size - 1)
                binding.progressReading.progress = progress
                binding.tvProgressText.text = buildString {
                    append("第${viewModel.currentChapterIndex + 1}章")
                    if (chapters.isNotEmpty()) append(" / ${chapters.size}章")
                    append("  ${"%.0f".format(progress * 100)}%")
                }

                // 恢复滚动位置
                val savedScroll = viewModel.currentBook?.lastScrollPosition ?: 0
                if (viewModel.currentChapterIndex == (viewModel.currentBook?.lastChapterIndex ?: 0) && savedScroll > 0) {
                    binding.scrollView.post {
                        binding.scrollView.scrollTo(0, savedScroll)
                    }
                } else {
                    binding.scrollView.scrollTo(0, 0)
                }

                // 上下章按钮状态
                binding.btnPrevChapter.isEnabled = content.hasPrev
                binding.btnNextChapter.isEnabled = content.hasNext
                binding.btnPrevChapter.alpha = if (content.hasPrev) 1f else 0.4f
                binding.btnNextChapter.alpha = if (content.hasNext) 1f else 0.4f
            }
        }
    }

    private fun toggleMenu() {
        isMenuVisible = !isMenuVisible
        val topBar = binding.layoutTopBar
        val bottomBar = binding.layoutBottomBar
        if (isMenuVisible) {
            topBar.visibility = View.VISIBLE
            bottomBar.visibility = View.VISIBLE
            topBar.animate().alpha(1f).setDuration(200).start()
            bottomBar.animate().alpha(1f).setDuration(200).start()
        } else {
            topBar.animate().alpha(0f).setDuration(200).withEndAction {
                topBar.visibility = View.GONE
            }.start()
            bottomBar.animate().alpha(0f).setDuration(200).withEndAction {
                bottomBar.visibility = View.GONE
            }.start()
        }
    }

    private fun showChapterList() {
        val dialog = BottomSheetDialog(this)
        val dlgBinding = DialogChapterListBinding.inflate(layoutInflater)
        dialog.setContentView(dlgBinding.root)

        val chapters = viewModel.chapterList.value
        val chapterAdapter = ChapterListAdapter(chapters, viewModel.currentChapterIndex) { index ->
            viewModel.loadChapter(index)
            dialog.dismiss()
        }
        dlgBinding.rvChapters.layoutManager = LinearLayoutManager(this)
        dlgBinding.rvChapters.adapter = chapterAdapter
        dlgBinding.rvChapters.scrollToPosition(viewModel.currentChapterIndex)

        dlgBinding.tvTitle.text = "目录（${chapters.size}章）"
        dialog.show()
    }

    private fun showSettings() {
        val dialog = BottomSheetDialog(this)
        val dlgBinding = DialogReaderSettingsBinding.inflate(layoutInflater)
        dialog.setContentView(dlgBinding.root)

        // 字体大小
        dlgBinding.seekFontSize.progress = ((ReaderPrefs.fontSize - 12f) / (32f - 12f) * 100).toInt()
        dlgBinding.tvFontSizeValue.text = "${ReaderPrefs.fontSize.toInt()}sp"
        dlgBinding.seekFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                ReaderPrefs.fontSize = 12f + progress / 100f * 20f
                dlgBinding.tvFontSizeValue.text = "${ReaderPrefs.fontSize.toInt()}sp"
                binding.tvContent.textSize = ReaderPrefs.fontSize
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) { ReaderPrefs.save(this@ReaderActivity) }
        })

        // 行间距
        dlgBinding.seekLineSpacing.progress = ((ReaderPrefs.lineSpacingMult - 1.2f) / (2.5f - 1.2f) * 100).toInt()
        dlgBinding.tvLineSpacingValue.text = "${"%.1f".format(ReaderPrefs.lineSpacingMult)}倍"
        dlgBinding.seekLineSpacing.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                ReaderPrefs.lineSpacingMult = 1.2f + progress / 100f * 1.3f
                dlgBinding.tvLineSpacingValue.text = "${"%.1f".format(ReaderPrefs.lineSpacingMult)}倍"
                binding.tvContent.setLineSpacing(0f, ReaderPrefs.lineSpacingMult)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) { ReaderPrefs.save(this@ReaderActivity) }
        })

        // 主题选择
        val themeButtons = listOf(
            dlgBinding.btnTheme0, dlgBinding.btnTheme1, dlgBinding.btnTheme2,
            dlgBinding.btnTheme3, dlgBinding.btnTheme4
        )
        themeButtons.forEachIndexed { i, btn ->
            btn.setOnClickListener {
                ReaderPrefs.theme = i
                ReaderPrefs.save(this)
                applyTheme()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun applyTheme() {
        val theme = ReaderPrefs.currentTheme()
        binding.layoutReader.setBackgroundColor(theme.bgColor)
        binding.tvChapterTitle.setTextColor(theme.textColor)
        binding.tvContent.apply {
            setTextColor(theme.textColor)
            textSize = ReaderPrefs.fontSize
            setLineSpacing(0f, ReaderPrefs.lineSpacingMult)
        }
    }

    override fun onPause() {
        super.onPause()
        autoSaveHandler.removeCallbacks(autoSaveRunnable)
        viewModel.saveProgress(binding.scrollView.scrollY)
    }

    override fun onDestroy() {
        super.onDestroy()
        autoSaveHandler.removeCallbacks(autoSaveRunnable)
    }
}
