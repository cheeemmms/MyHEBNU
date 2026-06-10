package com.myhebnu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.AuthRepository
import com.myhebnu.ui.about.AboutScreen
import com.myhebnu.ui.about.SystemUpdateScreen
import com.myhebnu.ui.auth.LoginScreen
import com.myhebnu.ui.auth.LoginViewModel
import com.myhebnu.ui.auth.WebViewFallbackScreen
import com.myhebnu.ui.exam.ExamScreen
import com.myhebnu.ui.grade.GradeScreen
import com.myhebnu.ui.home.HomeScreen
import com.myhebnu.ui.navigation.TopLevelRoute
import com.myhebnu.ui.room.RoomScreen
import com.myhebnu.ui.schedule.ScheduleScreen
import com.myhebnu.ui.settings.AdvancedSettingsScreen
import com.myhebnu.ui.settings.ColorThemeScreen
import com.myhebnu.ui.settings.SettingsScreen
import com.myhebnu.ui.theme.MyHEBNUTheme
import com.myhebnu.ui.theme.findPresetById
import com.myhebnu.ui.welcome.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var preferences: UserPreferences

    /** Channel for widget deep-link navigation requests */
    val pendingNavigation = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Handle widget deep link from initial launch intent
        handleDeepLink(intent)
        setContent {
            // 持续观察 DataStore Flow，选色后即时生效
            val themeMode by preferences.themeMode.collectAsState(initial = "system")
            val useCustomColorsPref by preferences.useCustomColors.collectAsState(initial = false)
            val activePresetId by preferences.activePresetId.collectAsState(initial = null)
            val customPresetsJson by preferences.customPresetsJson.collectAsState(initial = "[]")

            val customPresets = remember(customPresetsJson) { parsePresetsJson(customPresetsJson) }
            val activePreset = remember(activePresetId, customPresets) {
                if (activePresetId != null) findPresetById(activePresetId!!, customPresets) else null
            }
            val seedHue = activePreset?.seedHue
            val useCustomColors = useCustomColorsPref && seedHue != null

            MyHEBNUTheme(
                themeMode = themeMode,
                useCustomColors = useCustomColors,
                seedHue = seedHue
            ) {
                MyHEBNUApp(
                    authRepository = authRepository,
                    preferences = preferences,
                    pendingNavigation = pendingNavigation
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "myhebnu" && uri.host == "navigate") {
                val route = uri.lastPathSegment ?: "home"
                pendingNavigation.trySend(route)
            }
        }
    }
}

/** App state: welcome (first launch) → login → main content */
enum class AppScreen { Welcome, Login, Main }

@Composable
fun MyHEBNUApp(
    authRepository: AuthRepository,
    preferences: UserPreferences,
    pendingNavigation: kotlinx.coroutines.channels.Channel<String>
) {
    var currentScreen by remember { mutableStateOf<AppScreen?>(null) }
    var sessionExpiredMessage by remember { mutableStateOf<String?>(null) }
    val loginViewModel: LoginViewModel = hiltViewModel()

    // Check initial state: first launch? valid session?
    LaunchedEffect(Unit) {
        val isFirstLaunch = preferences.isFirstLaunch.first()
        if (isFirstLaunch) {
            currentScreen = AppScreen.Welcome
        } else {
            val hasSession = authRepository.hasValidSession()
            currentScreen = if (hasSession) AppScreen.Main else AppScreen.Login
        }
    }

    // Observe session expiry (302 detected by AuthInterceptor)
    val sessionExpired by authRepository.sessionExpired.collectAsState()
    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            android.util.Log.w("MyHEBNU", "检测到 session 过期，尝试自动登录...")
            val success = authRepository.autoLogin()
            if (success) {
                android.util.Log.w("MyHEBNU", "自动登录成功，用户无感恢复")
                // Session is already reset in autoLogin()
            } else {
                android.util.Log.w("MyHEBNU", "自动登录失败，跳转登录页")
                sessionExpiredMessage = "会话已过期，请重新登录"
                currentScreen = AppScreen.Login
            }
        }
    }

    // Listen for login success from LoginViewModel
    val loginState by loginViewModel.uiState.collectAsState()
    LaunchedEffect(loginState.isLoggedIn) {
        if (loginState.isLoggedIn) {
            currentScreen = AppScreen.Main
        }
    }

    when (currentScreen) {
        null -> {
            // Loading
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        AppScreen.Welcome -> {
            val scope = rememberCoroutineScope()
            WelcomeScreen(
                onGetStarted = {
                    scope.launch {
                        preferences.setFirstLaunchComplete()
                    }
                    currentScreen = AppScreen.Login
                }
            )
        }

        AppScreen.Login -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = { currentScreen = AppScreen.Main }
                )

                // Session expiry snackbar
                sessionExpiredMessage?.let { msg ->
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { sessionExpiredMessage = null }) {
                                Text("知道了")
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Text(msg)
                    }
                }
            }
        }

        AppScreen.Main -> {
            MainAppContent(pendingNavigation = pendingNavigation)
        }
    }
}

@Composable
fun MainAppContent(
    pendingNavigation: kotlinx.coroutines.channels.Channel<String>
) {
    var currentRoute by remember { mutableStateOf(TopLevelRoute.Home.route) }
    val backStack = remember { mutableStateListOf<String>() }

    // Handle widget deep-link navigation
    LaunchedEffect(Unit) {
        for (route in pendingNavigation) {
            backStack.clear()
            currentRoute = route
        }
    }

    // 前进导航：压栈后跳转
    val navigateTo: (String) -> Unit = { target ->
        backStack.add(currentRoute)
        currentRoute = target
    }

    // 后退导航：弹栈；栈空则回首页
    val goBack: () -> Unit = {
        currentRoute = if (backStack.isNotEmpty()) {
            backStack.removeAt(backStack.lastIndex)
        } else {
            TopLevelRoute.Home.route
        }
    }

    // 修复 #6d：非首页时拦截系统返回手势
    BackHandler(enabled = currentRoute != TopLevelRoute.Home.route) {
        goBack()
    }

    // #6e: Opaque backdrop prevents white window background from
    // bleeding through during AnimatedContent crossfade in dark mode.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(targetState = currentRoute) { route ->
            when (route) {
                TopLevelRoute.Home.route -> HomeScreen(
                    onNavigate = { target -> navigateTo(target) },
                    onNavigateToSettings = { navigateTo(TopLevelRoute.SettingsRoute.route) }
                )

                TopLevelRoute.Schedule.route -> ScheduleScreen(
                    onBack = { goBack() }
                )

                TopLevelRoute.Grade.route -> GradeScreen(
                    onBack = { goBack() }
                )

                TopLevelRoute.EmptyRoom.route -> RoomScreen(
                    onBack = { goBack() }
                )

                TopLevelRoute.Exam.route -> ExamScreen(
                    onBack = { goBack() }
                )

                TopLevelRoute.SettingsRoute.route -> SettingsScreen(
                    onBack = { goBack() },
                    onNavigateToAdvanced = { navigateTo("advanced_settings") },
                    onNavigateToWebViewLogin = { navigateTo("webview_login") },
                    onNavigateToAbout = { navigateTo("about") }
                )

                "advanced_settings" -> AdvancedSettingsScreen(
                    onBack = { goBack() },
                    onNavigateToColorTheme = { navigateTo("color_theme") }
                )

                "color_theme" -> ColorThemeScreen(
                    onBack = { goBack() }
                )

                "webview_login" -> {
                    val loginVm: LoginViewModel = hiltViewModel()
                    val webViewLoginState by loginVm.uiState.collectAsState()

                    LaunchedEffect(Unit) {
                        loginVm.setupWebViewLogin()
                    }

                    // When WebView login succeeds, clear stack and go home
                    LaunchedEffect(webViewLoginState.isLoggedIn) {
                        if (webViewLoginState.isLoggedIn) {
                            backStack.clear()
                            currentRoute = TopLevelRoute.Home.route
                        }
                    }

                    // WebView toolbar back arrow → hideWebViewFallback() → observe and goBack()
                    var webViewReady by remember { mutableStateOf(false) }
                    LaunchedEffect(webViewLoginState.showWebViewFallback) {
                        if (webViewReady && !webViewLoginState.showWebViewFallback) {
                            goBack()
                        }
                        if (webViewLoginState.showWebViewFallback) {
                            webViewReady = true
                        }
                    }

                    WebViewFallbackScreen(
                        loginUrl = webViewLoginState.loginUrl,
                        viewModel = loginVm
                    )
                }

                "about" -> AboutScreen(
                    onBack = { goBack() },
                    onNavigateToSystemUpdate = { navigateTo("system_update") }
                )

                "system_update" -> SystemUpdateScreen(
                    onBack = { goBack() }
                )
            }
        }
    }
}

// ============================================================
// Helper: parse custom presets JSON for seed-hue lookup
// ============================================================

private fun parsePresetsJson(json: String): List<com.myhebnu.ui.theme.ColorPreset> {
    return try {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            com.myhebnu.ui.theme.ColorPreset(
                id = obj.getString("id"),
                name = obj.getString("name"),
                seedHue = obj.getDouble("seedHue").toFloat(),
                isBuiltIn = false
            )
        }
    } catch (e: Exception) { emptyList() }
}
