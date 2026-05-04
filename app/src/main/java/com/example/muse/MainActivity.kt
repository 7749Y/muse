package com.example.muse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import com.example.muse.ui.screen.browse.BrowseScreen
import com.example.muse.ui.screen.edit.EditScreen
import com.example.muse.ui.screen.edit.GeneralEditScreen
import com.example.muse.ui.screen.edit.component.GeneralText
import com.example.muse.ui.screen.edit.component.ModuleType
import com.example.muse.ui.screen.edit.component.RadialMenuTestScreen
import com.example.muse.ui.screen.home.HomeScreen
import com.example.muse.ui.theme.MuseTheme
//休假
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuseTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {
                    GeneralText(
                        text = "这是一串文本啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊\n再来一串\n再来一串\n再来一串\n再来一串\n再来一串",
                        type = ModuleType.List,
                    )
                }
            }
        }
    }
}