package xyz.koleno.internetradioplayer.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import xyz.koleno.internetradioplayer.MainActivity
import xyz.koleno.internetradioplayer.ui.main.MainScreen
import xyz.koleno.internetradioplayer.ui.main.MainScreenViewModel
import xyz.koleno.internetradioplayer.ui.settings.SettingsScreen
import xyz.koleno.internetradioplayer.ui.settings.SettingsScreenViewModel
import xyz.koleno.internetradioplayer.ui.theme.InternetRadioPlayerTheme

enum class NavDestinations {
    Main,
    Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    InternetRadioPlayerTheme {
        val navHostController = rememberNavController()
        val topBarState = rememberSaveable { (mutableStateOf(false)) }

        NavHost(
            navController = navHostController,
            startDestination = NavDestinations.Main.name
        ) {
            composable(NavDestinations.Main.name) {
                topBarState.value = false
                val viewModel: MainScreenViewModel =
                    hiltViewModel(LocalActivity.current as MainActivity)
                MainScreen(viewModel.state, {
                    viewModel.setEvent(it)
                }) {
                    navHostController.navigate(NavDestinations.Settings.name)
                }
            }
            composable(NavDestinations.Settings.name) {
                val viewModel: SettingsScreenViewModel = hiltViewModel()
                topBarState.value = true
                SettingsScreen(viewModel.state, viewModel.effect, onEvent = {
                    viewModel.setEvent(it)
                }) {
                    navHostController.popBackStack()
                }
            }
        }
    }
}