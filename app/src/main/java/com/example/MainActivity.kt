package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.PantryRepository
import com.example.ui.MainAppNavigation
import com.example.ui.PantryViewModel
import com.example.ui.PantryViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PantryRepository(database)
        val viewModelFactory = PantryViewModelFactory(repository)

        setContent {
            val viewModel: PantryViewModel = viewModel(factory = viewModelFactory)
            val selectedTheme by viewModel.selectedTheme.collectAsState()
            
            MyApplicationTheme(themePreset = selectedTheme) {
                MainAppNavigation(viewModel)
            }
        }
    }
}
