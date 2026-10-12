package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Doctor
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminExcelImportScreen
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.DoctorDetailsScreen
import com.example.ui.screens.DoctorEditDialog
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.DalilikAlTibbiTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.HomeViewModel

enum class AppScreen {
    HOME,
    DOCTOR_DETAILS,
    FAVORITES,
    ABOUT,
    ADMIN_LOGIN,
    ADMIN_DASHBOARD,
    ADMIN_IMPORT
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DalilikAlTibbiTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        DalilikApp()
                    }
                }
            }
        }
    }
}

@Composable
fun DalilikApp(
    homeViewModel: HomeViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var selectedDoctor by remember { mutableStateOf<Doctor?>(null) }
    val isAdminLoggedIn by adminViewModel.isLoggedIn.collectAsState()

    var doctorToEdit by remember { mutableStateOf<Doctor?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    when (currentScreen) {
        AppScreen.HOME -> {
            HomeScreen(
                viewModel = homeViewModel,
                onDoctorClick = { doctor ->
                    selectedDoctor = doctor
                    currentScreen = AppScreen.DOCTOR_DETAILS
                },
                onNavigateToFavorites = { currentScreen = AppScreen.FAVORITES },
                onNavigateToAbout = { currentScreen = AppScreen.ABOUT },
                onNavigateToAdmin = {
                    currentScreen = if (isAdminLoggedIn) AppScreen.ADMIN_DASHBOARD else AppScreen.ADMIN_LOGIN
                }
            )
        }

        AppScreen.DOCTOR_DETAILS -> {
            BackHandler {
                currentScreen = AppScreen.HOME
            }
            selectedDoctor?.let { doc ->
                DoctorDetailsScreen(
                    doctor = doc,
                    onBack = { currentScreen = AppScreen.HOME },
                    onToggleFavorite = {
                        homeViewModel.toggleFavorite(doc)
                        selectedDoctor = doc.copy(isFavorite = !doc.isFavorite)
                    }
                )
            } ?: run {
                currentScreen = AppScreen.HOME
            }
        }

        AppScreen.FAVORITES -> {
            BackHandler {
                currentScreen = AppScreen.HOME
            }
            FavoritesScreen(
                viewModel = homeViewModel,
                onDoctorClick = { doctor ->
                    selectedDoctor = doctor
                    currentScreen = AppScreen.DOCTOR_DETAILS
                },
                onBack = { currentScreen = AppScreen.HOME }
            )
        }

        AppScreen.ABOUT -> {
            BackHandler {
                currentScreen = AppScreen.HOME
            }
            AboutScreen(
                viewModel = homeViewModel,
                onBack = { currentScreen = AppScreen.HOME }
            )
        }

        AppScreen.ADMIN_LOGIN -> {
            BackHandler {
                currentScreen = AppScreen.HOME
            }
            if (isAdminLoggedIn) {
                currentScreen = AppScreen.ADMIN_DASHBOARD
            } else {
                AdminLoginScreen(
                    viewModel = adminViewModel,
                    onBack = { currentScreen = AppScreen.HOME }
                )
            }
        }

        AppScreen.ADMIN_DASHBOARD -> {
            BackHandler {
                currentScreen = AppScreen.HOME
            }
            if (!isAdminLoggedIn) {
                currentScreen = AppScreen.ADMIN_LOGIN
            } else {
                AdminDashboardScreen(
                    adminViewModel = adminViewModel,
                    homeViewModel = homeViewModel,
                    onNavigateToImport = { currentScreen = AppScreen.ADMIN_IMPORT },
                    onBack = { currentScreen = AppScreen.HOME },
                    onEditDoctor = { doctor ->
                        doctorToEdit = doctor
                        showEditDialog = true
                    },
                    onAddNewDoctor = {
                        doctorToEdit = null
                        showEditDialog = true
                    }
                )
            }
        }

        AppScreen.ADMIN_IMPORT -> {
            BackHandler {
                currentScreen = AppScreen.ADMIN_DASHBOARD
            }
            if (!isAdminLoggedIn) {
                currentScreen = AppScreen.ADMIN_LOGIN
            } else {
                AdminExcelImportScreen(
                    viewModel = adminViewModel,
                    onBack = { currentScreen = AppScreen.ADMIN_DASHBOARD }
                )
            }
        }
    }

    if (showEditDialog) {
        DoctorEditDialog(
            initialDoctor = doctorToEdit,
            onDismiss = {
                showEditDialog = false
                doctorToEdit = null
            },
            onSave = { updatedDoctor ->
                adminViewModel.saveDoctorManually(updatedDoctor) {
                    showEditDialog = false
                    doctorToEdit = null
                }
            }
        )
    }
}
