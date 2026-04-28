# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires device/emulator)
./gradlew connectedDebugAndroidTest

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.example.muse.ExampleUnitTest"

# Run lint
./gradlew lintDebug

# Build and install debug app
./gradlew installDebug
```

## Tech Stack

- **Language:** Kotlin 2.2.10
- **UI:** Jetpack Compose with Material3
- **Minimum SDK:** 32, Target SDK: 36
- **Architecture:** Single-module Android app
- **Build:** Gradle Kotlin DSL with version catalog (`gradle/libs.versions.toml`)

## Project Structure

```
app/src/
├── androidTest/         # Instrumented tests (Espresso + Compose)
│   └── java/com/example/muse/
├── main/
│   ├── java/com/example/muse/
│   │   ├── MainActivity.kt          # Single Activity entry point
│   │   └── ui/theme/                # Theme definitions
│   │       ├── Color.kt
│   │       ├── Theme.kt
│   │       └── Type.kt
│   ├── res/                          # Android resources
│   └── AndroidManifest.xml
└── test/                 # Unit tests (JUnit 4)
    └── java/com/example/muse/
```

## Architecture Notes

- Composable content is set directly in `MainActivity.kt` — no Navigation component or ViewModel setup yet
- Theme uses Material3 dynamic color on Android 12+ with fallback to custom purple scheme
- Edge-to-edge display is enabled (`enableEdgeToEdge()`)
- Version catalog at `gradle/libs.versions.toml` manages all dependency versions in one place
- All Gradle repositories include Aliyun mirrors as primary source for network stability in China

## Product Vision

Muse 是一个面向移动端的笔记应用，兼容 Markdown，以"模块化编辑"为核心交互方式。

### 编辑方式

每个条目由多个**模块**组成（段落、列表、表格、代码块、图片、引用等），通过编辑页面的 **+ 菜单按钮**选择模块类型添加。编辑某个模块时将其**最大化**全屏编辑，仅保留该模块编辑框 + 输入法 + 顶部小型标题提示。所有编辑框**必须自动换行**，禁止水平滚动条。

### Markdown 文件结构

标签名即文件名，关键字在软件内用于检索，在 .md 文件中写在条目下方。

```
# 标签名

## 条目标题

### 关键字1|关键字2|...

    [内容]

<sup>*yy-mm-dd*</sup>

    [追加内容]

<sup>*yy-mm-dd*</sup>

---
```

- 条目按日期**倒序排列**
- 已保存的条目再次编辑时，若日期不同则在原条目下**追加新内容和对应日期**
- 日期可手动编辑更改

### 数据结构

每个条目包含：

- **ID**：唯一不重复
- **标签**：唯一（对应 .md 文件名）
- **关键字**：多个，用于检索
- **内容**：模块化结构
- **日期**：自动记录，可编辑
- **收藏**：是否收藏

### 屏幕规划

- `HomeScreen` — 首页（词云 + 搜索 + 底部标签栏）
- `BrowseScreen` — 按标签浏览（卡片列表，每卡片含标题+描述+标签）
- `EditScreen` — 条目编辑（标题输入 + 标签 + 模块菜单 + 模块最大化编辑）
