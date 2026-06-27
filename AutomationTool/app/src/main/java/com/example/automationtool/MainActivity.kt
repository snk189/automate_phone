package com.example.automationtool

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.automationtool.theme.AutomationToolTheme

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.automationtool.ui.screens.CreateAutomationScreen
import com.example.automationtool.ui.screens.HomeScreen
import com.example.automationtool.viewmodel.AutomationViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutomationToolTheme { 
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
                    MainNavigation() 
                } 
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val viewModel: AutomationViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToCreate = { id ->
                    if (id != null) {
                        navController.navigate("create/$id")
                    }
                }
            )
        }
        composable("create/{automationId}") { backStackEntry ->
            val idString = backStackEntry.arguments?.getString("automationId")
            val id = idString?.toLongOrNull()
            if (id != null) {
                CreateAutomationScreen(
                    automationId = id,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
