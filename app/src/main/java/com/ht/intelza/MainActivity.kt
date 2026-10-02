package com.ht.intelza

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ht.intelza.ui.IntelzaApp
import com.ht.intelza.ui.theme.IntelzaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntelzaTheme {
                IntelzaApp()
            }
        }
    }
}
