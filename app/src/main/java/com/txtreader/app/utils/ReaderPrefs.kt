package com.txtreader.app.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 阅读设置持久化
 */
object ReaderPrefs {
    private const val PREFS_NAME = "reader_settings"
    private const val KEY_FONT_SIZE = "font_size"
    private const val KEY_THEME = "theme"   // 0:白 1:护眼绿 2:羊皮纸 3:深色 4:夜间
    private const val KEY_LINE_SPACING = "line_spacing"
    private const val KEY_PAGE_PADDING = "page_padding"
    private const val KEY_BRIGHTNESS = "brightness"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var fontSize: Float
        get() = _fontSize
        set(v) { _fontSize = v }
    var theme: Int
        get() = _theme
        set(v) { _theme = v }
    var lineSpacingMult: Float
        get() = _lineSpacing
        set(v) { _lineSpacing = v }

    private var _fontSize = 18f
    private var _theme = 0
    private var _lineSpacing = 1.8f

    fun load(context: Context) {
        val p = prefs(context)
        _fontSize = p.getFloat(KEY_FONT_SIZE, 18f)
        _theme = p.getInt(KEY_THEME, 0)
        _lineSpacing = p.getFloat(KEY_LINE_SPACING, 1.8f)
    }

    fun save(context: Context) {
        prefs(context).edit()
            .putFloat(KEY_FONT_SIZE, _fontSize)
            .putInt(KEY_THEME, _theme)
            .putFloat(KEY_LINE_SPACING, _lineSpacing)
            .apply()
    }

    // 主题配置
    data class ThemeConfig(
        val bgColor: Int,
        val textColor: Int,
        val name: String
    )

    val themes = listOf(
        ThemeConfig(0xFFFAFAFA.toInt(), 0xFF222222.toInt(), "白昼"),
        ThemeConfig(0xFFCCE8CF.toInt(), 0xFF1A3A1A.toInt(), "护眼"),
        ThemeConfig(0xFFF5E6C8.toInt(), 0xFF3D2B1F.toInt(), "羊皮纸"),
        ThemeConfig(0xFF2D2D2D.toInt(), 0xFFCCCCCC.toInt(), "深色"),
        ThemeConfig(0xFF0D0D0D.toInt(), 0xFF888888.toInt(), "夜间")
    )

    fun currentTheme(): ThemeConfig = themes[_theme.coerceIn(0, themes.size - 1)]
}
