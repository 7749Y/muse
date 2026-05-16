package com.example.muse.ui.screen.edit.component.heading

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 标题等级下拉选择器——点击按钮弹出 h2~h6 下拉菜单。
 */
@Composable
fun H2_6Button(
    level: Int,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minLevel: Int = 2,
    maxLevel: Int = 6,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "h$level",
                fontSize = 14.sp,
                color = Color.White,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            (minLevel..maxLevel).forEach { lvl ->
                val selected = lvl == level
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "h$lvl",
                            fontSize = 14.sp,
                            color = if (selected) Color(0xFFFFD700) else Color.White,
                        )
                    },
                    onClick = {
                        onLevelChange(lvl)
                        expanded = false
                    },
                )
            }
        }
    }
}
