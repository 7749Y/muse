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
import com.example.muse.data.local.PrimaryTagEntity
import com.example.muse.data.local.SecondaryTagEntity
import com.example.muse.ui.screen.edit.EditScreen
import com.example.muse.ui.screen.home.HomeScreen
import com.example.muse.ui.theme.MuseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuseTheme(darkTheme = true) {
                val db = remember { MuseDatabase.getInstance(this@MainActivity) }
                val scope = rememberCoroutineScope()
                var primaryTags by remember { mutableStateOf(emptyList<PrimaryTagEntity>()) }
                var loaded by remember { mutableStateOf(false) }

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
                    HomeScreen(primaryTags = primaryTags)
                }
            }
        }
    }
}


