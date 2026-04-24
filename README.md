# TXT小说阅读器 (TxtReader)

> 原生 Android 应用，Kotlin + Room + MVVM 架构

---

## 📱 功能特性

### 书架管理
- 通过系统文件选择器导入任意 TXT 文件（支持 SAF 持久化权限）
- 网格卡片展示书籍，彩色封面 + 首字标识
- 显示章节数、总字数、最后阅读时间
- 封面底部进度条直观显示阅读进度百分比
- 长按书籍卡片弹出删除确认对话框（不删除原文件）

### TXT 解析
- **自动检测编码**：UTF-8 / GBK / GB2312（含 BOM 检测）
- **智能章节分割**：正则匹配"第X章/节/回"、"Chapter N"、数字序号等常见格式
- 无章节时整本书作为单章处理

### 阅读器
- 沉浸式全屏阅读
- **点击左/右 30% 区域**：上/下翻章节
- **点击中间**：显示/隐藏工具栏
- 顶栏：返回 · 书名 · 目录 · 设置
- 底栏：章节进度 · 上/下章按钮

### 阅读设置
- 字体大小：12sp ~ 32sp（滑动调节）
- 行间距：1.2 ~ 2.5 倍（滑动调节）
- 五种背景主题：白昼 / 护眼绿 / 羊皮纸 / 深色 / 夜间
- 设置自动持久化（SharedPreferences）

### 进度记忆
- **自动保存**：停止滑动 1.5 秒后自动保存
- **退到后台**：`onPause` 时立即保存
- 记录：章节索引 + 章节内滚动位置 + 总进度百分比
- 重新打开书籍时精确恢复到上次位置

---

## 🏗️ 项目结构

```
TxtReader/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/txtreader/app/
│       │   ├── data/
│       │   │   ├── entity/
│       │   │   │   ├── Book.kt          # 书籍实体（含进度字段）
│       │   │   │   └── Chapter.kt       # 章节实体
│       │   │   ├── dao/
│       │   │   │   ├── BookDao.kt       # 书籍 CRUD + 进度更新
│       │   │   │   └── ChapterDao.kt    # 章节查询
│       │   │   └── db/
│       │   │       └── AppDatabase.kt   # Room 数据库单例
│       │   ├── ui/
│       │   │   ├── bookshelf/
│       │   │   │   ├── BookshelfActivity.kt  # 书架主界面
│       │   │   │   ├── BookshelfViewModel.kt # 导入逻辑
│       │   │   │   └── BookAdapter.kt        # 书架 RecyclerView 适配器
│       │   │   └── reader/
│       │   │       ├── ReaderActivity.kt     # 阅读器主界面
│       │   │       ├── ReaderViewModel.kt    # 章节加载 + 进度保存
│       │   │       ├── ChapterListAdapter.kt # 目录列表适配器
│       │   │       ├── GestureScrollView.kt  # 手势检测 ScrollView
│       │   │       └── ReadingProgressBar.kt # 自定义进度条 View
│       │   └── utils/
│       │       ├── TxtParser.kt   # 编码检测 + 章节解析
│       │       ├── FileUtils.kt   # 文件信息工具
│       │       └── ReaderPrefs.kt # 阅读设置持久化
│       └── res/
│           ├── layout/
│           │   ├── activity_bookshelf.xml
│           │   ├── activity_reader.xml
│           │   ├── item_book.xml
│           │   ├── item_chapter.xml
│           │   ├── dialog_chapter_list.xml
│           │   └── dialog_reader_settings.xml
│           ├── values/
│           │   ├── strings.xml
│           │   ├── colors.xml
│           │   └── themes.xml
│           └── menu/
│               └── menu_bookshelf.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高
- Android SDK 34
- Kotlin 1.9.22
- Gradle 8.4

### 构建步骤
1. 用 Android Studio 打开 `TxtReader/` 目录
2. 等待 Gradle 同步完成
3. 连接安卓设备或启动模拟器（API 24+）
4. 点击 Run ▶ 即可

### 使用说明
1. 打开 App，书架默认为空
2. 点击右下角 **+** 按钮，通过系统文件选择器选择 TXT 文件
3. 解析完成后书籍出现在书架，点击即可阅读
4. 阅读时：
   - 点击屏幕中央 → 弹出工具栏
   - 点击左边 → 上一章（或手指向右滑）
   - 点击右边 → 下一章
   - 工具栏右上角 → 目录 / 设置

---

## 🔧 技术栈

| 组件 | 技术 |
|------|------|
| UI | Android View System + ViewBinding |
| 架构 | MVVM + Repository |
| 数据库 | Room 2.6 |
| 异步 | Kotlin Coroutines + StateFlow |
| 文件访问 | Storage Access Framework (SAF) |
| 编码检测 | 自实现 BOM + 字节序列检测 |
| 章节解析 | 正则表达式匹配 |
| 设置存储 | SharedPreferences |

---

## 📋 后续可扩展功能

- [ ] 书签功能（记录多个位置）
- [ ] 全文搜索
- [ ] 自动滚动（定速朗读模式）
- [ ] 字体选择（明体/黑体/等宽）
- [ ] 屏幕亮度调节
- [ ] 本地备份/恢复阅读进度
- [ ] 批量导入目录
- [ ] 书籍排序（按名称/时间/进度）
