package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.jaboard.JaBoardApp
import com.example.jaboard.core.datastore.UserPreferences
import com.example.jaboard.core.design.JaBoardTheme
import com.example.jaboard.navigation.JaBoardAppShell

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    com.example.jaboard.core.system.configureAppCheck(this, intent)
    val container = (application as JaBoardApp).container
    setContent {
      val prefs by
        container.userPreferencesRepository.preferences.collectAsStateWithLifecycle(
          initialValue = UserPreferences()
        )
      JaBoardTheme(
        themeMode = prefs.themeMode,
        useDynamicColor = prefs.useDynamicColor,
      ) {
        JaBoardAppShell(container = container)
      }
    }
  }
}
