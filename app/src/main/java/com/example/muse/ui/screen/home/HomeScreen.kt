package com.example.muse.ui.screen.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.muse.R
import com.example.muse.data.local.PrimaryTagEntity
import com.example.muse.ui.theme.MuseTheme

@Composable
fun HomeScreen(
    primaryTags: List<PrimaryTagEntity> = emptyList(),
    onTagClick: (tagId: Long, tagName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Title
            Text(
                text = "Μουσαι",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp),
                textAlign = TextAlign.Center,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-2).sp
            )

            // Word cloud (stretches to fill available space)
            WordCloudSection(modifier = Modifier.weight(1f))

            // Search bar
            SearchBarSection(modifier = Modifier.padding(vertical = 20.dp))

            // Bottom frame
            BottomFrameSection(
                primaryTags = primaryTags,
                onTagClick = onTagClick,
            )
        }

        // Settings icon
        IconButton(
            onClick = { /* TODO */ },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 16.dp)
                .size(36.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "Settings",
                tint = Color.White
            )
        }

        // Edit button (floating)
        IconButton(
            onClick = { /* TODO */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 37.dp, bottom = 94.dp)
                .size(48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = "Edit",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun WordCloudSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(12.dp)
    ) {
        Text(
            text = "云图（待办）",
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            color = Color.White
        )
    }
}

@Composable
private fun SearchBarSection(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp)
            .border(1.dp, Color(0xFF444444), CircleShape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "=￣ω￣=",
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = "Search",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun BottomFrameSection(
    primaryTags: List<PrimaryTagEntity>,
    onTagClick: (tagId: Long, tagName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(start = 36.dp, end = 62.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "GitHub同款\ncontributions\n（待办）",
                fontFamily = FontFamily.Monospace,
                fontSize = 15.sp,
                color = Color.White,
                lineHeight = 20.sp
            )
        }

        // Right tag column
        Column(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            primaryTags.forEach { tag ->
                TagButton(
                    text = tag.name,
                    onClick = { onTagClick(tag.id, tag.name) },
                )
            }
            AddTagButton()
        }
    }
}

@Composable
private fun TagButton(text: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color(0xFFB3B3B3)
        )
    }
}

@Composable
private fun AddTagButton() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add),
            contentDescription = "Add tag",
            tint = Color(0xFFB3B3B3)
        )
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF1E1E1E
)
@Composable
private fun HomeScreenPreview() {
    MuseTheme(darkTheme = true) {
        HomeScreen(
            primaryTags = listOf(
                PrimaryTagEntity(id = 1, name = "编程"),
                PrimaryTagEntity(id = 2, name = "美术"),
                PrimaryTagEntity(id = 3, name = "音乐"),
            ),
        )
    }
}
