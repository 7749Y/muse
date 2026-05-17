package com.example.muse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.muse.data.local.DocumentPreview
import com.example.muse.data.local.MuseDatabase
import com.example.muse.data.local.PrimaryTagEntity
import com.example.muse.ui.screen.browse.BrowseScreen
import com.example.muse.ui.screen.home.HomeScreen
import com.example.muse.ui.theme.MuseTheme
import kotlinx.coroutines.launch

private sealed class Screen {
    data object Home : Screen()
    data class Browse(val tagId: Long, val tagName: String) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuseTheme(darkTheme = true) {
                val db = remember { MuseDatabase.getInstance(this@MainActivity) }
                val repository = remember { com.example.muse.data.local.DocumentRepository(db) }
                val scope = rememberCoroutineScope()
                var primaryTags by remember { mutableStateOf(emptyList<PrimaryTagEntity>()) }
                var loaded by remember { mutableStateOf(false) }
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
                var articles by remember { mutableStateOf(emptyList<DocumentPreview>()) }

                LaunchedEffect(Unit) {
                    if (db.tagDao().getPrimaryTagCount() == 0) {
                        db.tagDao().insertPrimaryTags(listOf(
                            PrimaryTagEntity(name = "编程"),
                            PrimaryTagEntity(name = "美术"),
                            PrimaryTagEntity(name = "音乐"),
                        ))
                    }
                    primaryTags = db.tagDao().getAllPrimaryTagsOnce()
                    loaded = true
                }

                if (loaded) {
                    when (val screen = currentScreen) {
                        is Screen.Home -> HomeScreen(
                            primaryTags = primaryTags,
                            onTagClick = { tagId, tagName ->
                                currentScreen = Screen.Browse(tagId, tagName)
                                scope.launch {
                                    articles = repository.getDocumentPreviewsByPrimaryTag(tagId)
                                }
                            },
                        )

                        is Screen.Browse -> BrowseScreen(
                            title = screen.tagName,
                            articles = articles,
                            onBackClick = {
                                currentScreen = Screen.Home
                                articles = emptyList()
                            },
                        )
                    }
                }
            }
        }
    }
}
