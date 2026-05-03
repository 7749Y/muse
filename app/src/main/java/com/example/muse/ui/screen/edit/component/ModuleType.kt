package com.example.muse.ui.screen.edit.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

enum class ModuleType(
    val displayName: String,
    val gutterProvider: (Int) -> GutterItem,
    val fontSize: TextUnit,
    val fontWeight: FontWeight,
    val textColor: Color = Color.White,
) {
    List("列表", { GutterItem.Bullet }, 20.sp, FontWeight.SemiBold),
    Code("代码块", { GutterItem.Number(it + 1) }, 16.sp, FontWeight.Normal),
    Quote("引用", { GutterItem.QuoteLine }, 24.sp, FontWeight.SemiBold),
    Image("图片", { GutterItem.None }, 14.sp, FontWeight.Normal),
    Table("表格", { GutterItem.None }, 14.sp, FontWeight.Normal),
    SubHeading("子标题", { GutterItem.None }, 18.sp, FontWeight.Bold),
}
