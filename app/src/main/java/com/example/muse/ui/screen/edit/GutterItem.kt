package com.example.muse.ui.screen.edit

sealed class GutterItem {
    /** 不绘制任何内容 */
    object None : GutterItem()

    /** 数字（行号） */
    data class Number(val number: Int) : GutterItem()

    /** 无序列表符号（圆点） */
    object Bullet : GutterItem()

    /** 有序列表前缀，如 "1." */
    data class OrderedNumber(val number: Int) : GutterItem()

    /** 引用竖线 */
    object QuoteLine : GutterItem()
}