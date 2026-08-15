package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.example.data.preferences.ThemeMode
import com.example.ui.navigation.SafeBoxNavGraph
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val app = application as SafeBoxApplication

    setContent {
      val prefState by app.appPreferences.state.collectAsState()
      val systemDark = isSystemInDarkTheme()
      val isDark = when (prefState.themeMode) {
        com.example.data.preferences.ThemeMode.SYSTEM -> systemDark
        com.example.data.preferences.ThemeMode.DARK -> true
        com.example.data.preferences.ThemeMode.LIGHT -> false
      }

      MyApplicationTheme(darkTheme = isDark) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val navController = rememberNavController()
          SafeBoxNavGraph(
            navController = navController,
            repository = app.repository,
            securityManager = app.securityManager,
            appPreferences = app.appPreferences
          )
        }
      }
    }
  }
}

