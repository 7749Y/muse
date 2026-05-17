package com.example.muse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.muse.data.local.DocumentRepository
import com.example.muse.data.local.MuseDatabase
import com.example.muse.ui.screen.edit.EditScreen
import com.example.muse.ui.theme.MuseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuseTheme(darkTheme = true) {
                val db = remember { MuseDatabase.getInstance(this@MainActivity) }
                val repository = remember { DocumentRepository(db) }
                val scope = rememberCoroutineScope()

                var modules by remember { mutableStateOf(emptyList<com.example.muse.ui.screen.edit.SavedModule>()) }
                var documentId by remember { mutableLongStateOf(0L) }
                var loaded by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val doc = repository.getOrCreateDefaultDocument()
                    modules = doc.modules
                    documentId = doc.document.id
                    loaded = true
                }

                if (loaded) {
                    EditScreen(
                        moduleSpacing = 22.dp,
                        initialModules = modules,
                        onSaveClick = { currentModules ->
                            scope.launch {
                                repository.saveDocument(
                                    name = "default",
                                    modules = currentModules,
                                    documentId = documentId,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
