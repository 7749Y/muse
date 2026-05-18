package com.example.muse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.muse.data.local.DocumentPreview
import com.example.muse.data.local.DocumentRepository
import com.example.muse.data.local.MuseDatabase
import com.example.muse.data.local.PrimaryTagEntity
import com.example.muse.data.local.SecondaryTagEntity
import com.example.muse.ui.screen.browse.BrowseScreen
import com.example.muse.ui.screen.edit.EditScreen
import com.example.muse.ui.screen.edit.SavedModule
import com.example.muse.ui.screen.home.HomeScreen
import com.example.muse.ui.theme.MuseTheme
import kotlinx.coroutines.launch

private sealed class Screen {
    data object Home : Screen()
    data class Browse(val tagId: Long, val tagName: String) : Screen()
    data object Edit : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuseTheme(darkTheme = true) {
                val db = remember { MuseDatabase.getInstance(this@MainActivity) }
                val repository = remember { DocumentRepository(db) }
                val scope = rememberCoroutineScope()
                var primaryTags by remember { mutableStateOf(emptyList<PrimaryTagEntity>()) }
                var loaded by remember { mutableStateOf(false) }
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
                var previousScreen by remember { mutableStateOf<Screen>(Screen.Home) }
                var articles by remember { mutableStateOf(emptyList<DocumentPreview>()) }

                // Edit screen data
                var editDocumentId by remember { mutableLongStateOf(0L) }
                var editModules by remember { mutableStateOf(emptyList<SavedModule>()) }
                var editTitle by remember { mutableStateOf("") }
                var selPrimaryTagId by remember { mutableStateOf<Long?>(null) }
                var selSecondaryTagIds by remember { mutableStateOf(emptySet<Long>()) }

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
                    when (currentScreen) {
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
                            title = (currentScreen as Screen.Browse).tagName,
                            articles = articles,
                            onBackClick = {
                                currentScreen = Screen.Home
                                articles = emptyList()
                            },
                            onArticleClick = { docId ->
                                val fromScreen = currentScreen
                                scope.launch {
                                    val doc = repository.loadDocument(docId) ?: return@launch
                                    editModules = doc.modules
                                    editTitle = doc.document.name
                                    editDocumentId = doc.document.id
                                    selPrimaryTagId = doc.document.primaryTagId
                                    selSecondaryTagIds = doc.secondaryTags.map { it.id }.toSet()
                                    previousScreen = fromScreen
                                    currentScreen = Screen.Edit
                                }
                            },
                        )

                        is Screen.Edit -> EditScreen(
                            modifier = Modifier,
                            moduleSpacing = 22.dp,
                            initialModules = editModules,
                            initialTitle = editTitle,
                            primaryTags = primaryTags,
                            selectedPrimaryTagId = selPrimaryTagId,
                            selectedSecondaryTagIds = selSecondaryTagIds,
                            onBackClick = {
                                val prev = previousScreen
                                currentScreen = prev
                                if (prev is Screen.Browse) {
                                    scope.launch {
                                        articles = repository.getDocumentPreviewsByPrimaryTag(prev.tagId)
                                    }
                                }
                            },
                            onSaveClick = { modules, title ->
                                scope.launch {
                                    repository.saveDocument(
                                        name = title,
                                        modules = modules,
                                        documentId = editDocumentId,
                                        primaryTagId = selPrimaryTagId,
                                    )
                                    repository.saveDocumentSecondaryTags(
                                        editDocumentId, selSecondaryTagIds.toList()
                                    )
                                }
                            },
                            onTagsChanged = { pId, sIds ->
                                selPrimaryTagId = pId
                                selSecondaryTagIds = sIds.toSet()
                            },
                            onCreateSecondaryTag = { name ->
                                val newId = db.tagDao().insertSecondaryTag(
                                    SecondaryTagEntity(name = name)
                                )
                                newId
                            },
                        )
                    }
                }
            }
        }
    }
}
