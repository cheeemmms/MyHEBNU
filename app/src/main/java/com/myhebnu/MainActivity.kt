package com.myhebnu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.AuthRepository
import com.myhebnu.ui.auth.LoginScreen
import com.myhebnu.ui.auth.LoginViewModel
import com.myhebnu.ui.exam.ExamScreen
import com.myhebnu.ui.grade.GradeScreen
import com.myhebnu.ui.home.HomeScreen
import com.myhebnu.ui.navigation.DrawerContent
import com.myhebnu.ui.navigation.TopLevelRoute
import com.myhebnu.ui.room.RoomScreen
import com.myhebnu.ui.schedule.ScheduleScreen
import com.myhebnu.ui.settings.AdvancedSettingsScreen
import com.myhebnu.ui.settings.SettingsScreen
import com.myhebnu.ui.theme.MyHEBNUTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var preferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Read persisted theme mode
            var themeMode by remember { mutableStateOf("system") }
            LaunchedEffect(Unit) {
                themeMode = preferences.themeMode.first()
            }

            MyHEBNUTheme(themeMode = themeMode) {
                MyHEBNUApp(authRepository = authRepository)
            }
        }
    }
}

@Composable
fun MyHEBNUApp(authRepository: AuthRepository) {
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    val loginViewModel: LoginViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        val result = authRepository.hasValidSession()
        isLoggedIn = result
    }

    val loginState by loginViewModel.uiState.collectAsState()
    LaunchedEffect(loginState.isLoggedIn) {
        if (loginState.isLoggedIn) isLoggedIn = true
    }

    when (isLoggedIn) {
        null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        false -> {
            LoginScreen(viewModel = loginViewModel, onLoginSuccess = { isLoggedIn = true })
        }
        true -> {
            MainAppContent()
        }
    }
}

@Composable
fun MainAppContent() {
    var currentRoute by remember { mutableStateOf(TopLevelRoute.Home.route) }
    var previousRoute by remember { mutableStateOf(TopLevelRoute.Home.route) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    previousRoute = currentRoute
                    currentRoute = TopLevelRoute.SettingsRoute.route
                }
            )
        }
    ) {
        // No global TopAppBar — each screen manages its own
        AnimatedContent(targetState = currentRoute) { route ->
            when (route) {
                TopLevelRoute.Home.route -> HomeScreen(
                    onNavigate = { target ->
                        previousRoute = currentRoute
                        currentRoute = target
                    },
                    onOpenDrawer = {
                        scope.launch { drawerState.open() }
                    }
                )
                TopLevelRoute.Schedule.route -> ScheduleScreen(
                    onBack = { currentRoute = TopLevelRoute.Home.route }
                )
                TopLevelRoute.Grade.route -> GradeScreen(
                    onBack = { currentRoute = TopLevelRoute.Home.route }
                )
                TopLevelRoute.EmptyRoom.route -> RoomScreen(
                    onBack = { currentRoute = TopLevelRoute.Home.route }
                )
                TopLevelRoute.Exam.route -> ExamScreen(
                    onBack = { currentRoute = TopLevelRoute.Home.route }
                )
                TopLevelRoute.SettingsRoute.route -> SettingsScreen(
                    onBack = { currentRoute = TopLevelRoute.Home.route },
                    onNavigateToAdvanced = {
                        previousRoute = currentRoute
                        currentRoute = "advanced_settings"
                    }
                )
                "advanced_settings" -> AdvancedSettingsScreen(
                    onBack = { currentRoute = previousRoute }
                )
            }
        }
    }
}
