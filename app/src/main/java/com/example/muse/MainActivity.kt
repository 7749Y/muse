package com.example.muse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.muse.ui.screen.browse.BrowseScreen
import com.example.muse.ui.screen.edit.EditScreen
import com.example.muse.ui.screen.edit.MaximizedEditScreen
import com.example.muse.ui.screen.edit.UndoRedoTestScreen
import com.example.muse.ui.screen.home.HomeScreen
import com.example.muse.ui.theme.MuseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuseTheme(darkTheme = true) {
                MaximizedEditScreen()
            }
        }
    }
}