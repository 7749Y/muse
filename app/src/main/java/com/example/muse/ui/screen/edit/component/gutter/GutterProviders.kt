package com.example.muse.ui.screen.edit.component.gutter

import com.example.muse.ui.screen.edit.component.gutter.GutterItem

/** 纯行号（视觉行从1开始） */
fun numberLineProvider(): (Int) -> GutterItem = { line -> GutterItem.Number(line + 1) }

/** 无序列表：所有行都用圆点 */
fun bulletListProvider(): (Int) -> GutterItem = { GutterItem.Bullet }

/** 有序列表：所有行自动编号 */
fun orderedListProvider(): (Int) -> GutterItem = { line -> GutterItem.OrderedNumber(line + 1) }

/** 引用：所有行显示竖线 */
fun quoteProvider(): (Int) -> GutterItem = { GutterItem.QuoteLine }

/** 无装饰 */
fun emptyGutter(): (Int) -> GutterItem = { GutterItem.None }
