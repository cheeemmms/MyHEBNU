package com.myhebnu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.AuthRepository
import com.myhebnu.ui.components.ContainerEnterTransition
import com.myhebnu.ui.components.ContainerExitTransition
import com.myhebnu.ui.about.AboutScreen
import com.myhebnu.ui.about.SystemUpdateScreen
import com.myhebnu.ui.auth.LoginScreen
import com.myhebnu.ui.exam.ExamScreen
import com.myhebnu.ui.grade.GradeScreen
import com.myhebnu.ui.home.HomeScreen
import com.myhebnu.ui.library.BibDetailScreen
import com.myhebnu.ui.library.FinesScreen
import com.myhebnu.ui.library.LibraryHomeScreen
import com.myhebnu.ui.library.LibraryRoutes
import com.myhebnu.ui.library.LoansScreen
import com.myhebnu.ui.library.ReaderCodeScreen
import com.myhebnu.ui.library.RequestsScreen
import com.myhebnu.ui.library.SearchScreen
import com.myhebnu.ui.library.WishlistScreen
import com.myhebnu.ui.navigation.TopLevelRoute
import com.myhebnu.ui.room.RoomScreen
import com.myhebnu.ui.schedule.ScheduleScreen
import com.myhebnu.ui.settings.AdvancedSettingsScreen
import com.myhebnu.ui.settings.ColorThemeScreen
import com.myhebnu.ui.settings.SettingsScreen
import com.myhebnu.ui.settings.SemesterSetupScreen
import com.myhebnu.ui.theme.MyHEBNUTheme
import com.myhebnu.ui.theme.findPresetById
import com.myhebnu.ui.welcome.WelcomeScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
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
                // Opaque backdrop prevents the system window background from
                // bleeding through during the loading/welcome/login phases.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    MyHEBNUApp(
                        authRepository = authRepository,
                        preferences = preferences,
                        pendingNavigation = pendingNavigation
                    )
                }
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
    var showSsoMigrationNotice by remember { mutableStateOf(false) }

    // Startup decision is LOCAL ONLY — no network call before the first frame.
    // Session validity is checked lazily by AuthInterceptor on the first API call,
    // and autoLogin() recovers silently if needed.
    LaunchedEffect(Unit) {
        val (isFirstLaunch, isLoggedIn) = combine(
            preferences.isFirstLaunch,
            preferences.isLoggedIn
        ) { firstLaunch, loggedIn -> firstLaunch to loggedIn }.first()

        currentScreen = when {
            isFirstLaunch -> AppScreen.Welcome
            isLoggedIn && authRepository.hasLocalCookieSession() -> AppScreen.Main
            else -> AppScreen.Login
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

    // 登录方式已从「教务自建登录页」切到「学校统一身份认证」，两边密码可能不同，
    // 所以对老用户提示一次（只提示一次，装完首次进入时展示）。
    LaunchedEffect(currentScreen) {
        if (currentScreen == null || currentScreen == AppScreen.Welcome) return@LaunchedEffect
        if (!preferences.ssoMigrationNoticeShown.first()) {
            showSsoMigrationNotice = true
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

    if (showSsoMigrationNotice) {
        val noticeScope = rememberCoroutineScope()
        val dismissNotice: () -> Unit = {
            showSsoMigrationNotice = false
            noticeScope.launch { preferences.setSsoMigrationNoticeShown(true) }
        }
        AlertDialog(
            onDismissRequest = dismissNotice,
            title = { Text("登录方式已升级") },
            text = {
                Text(
                    "因加入图书馆相关服务，本程序已把教务与图书馆统一到同一个身份认证入口（CAS）。" +
                        "本次更新后请使用「统一身份认证」的密码重新登录一次——" +
                        "它可能与你在教务系统旧登录页用过的密码不同。\n\n" +
                        "登录一次之后，教务与图书馆会自动共用同一个会话，不需要分别登录。\n\n" +
                        "如果你现在保持在登录状态，请清除应用数据后重新登录。"
                )
            },
            confirmButton = {
                TextButton(onClick = dismissNotice) { Text("知道了") }
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
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
            // 只避让横向的刘海/打孔（横屏时有用），纵向交给各页面自己处理：
            // 图书馆书籍详情要把封面铺到状态栏下面，这里统一加顶部内边距会抢先占掉那块空间。
            // 其余页面用的是 M3 TopAppBar，它自带状态栏 inset，不受影响。
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
    ) {
        // 首页卡片 → 目标页：共享元素容器变换（卡片直接展开成整页）
        SharedTransitionLayout {
            val sharedScope = this
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    if (isBibDetailRoute(initialState) || isBibDetailRoute(targetState)) {
                        // 书籍详情这条线只做 fade-through，不缩放。
                        // 页面级的 scaleIn(0.92) 会和共享元素的容器变换抢同一个位移：
                        // 去程目标是整页共享节点，看不到它；回程目标是列表页，看得到。
                        // 一进一出于是成了两套动画——这里把这条线钉成纯淡入淡出。
                        ContainerEnterTransition togetherWith ContainerExitTransition
                    } else {
                        // 其余路由沿用 Compose 默认 spec（AnimatedContent 自带的同一组参数）。
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                            scaleIn(
                                animationSpec = tween(220, delayMillis = 90),
                                initialScale = 0.92f
                            )) togetherWith fadeOut(animationSpec = tween(90))
                    }
                }
            ) { route ->
                val animatedScope: AnimatedVisibilityScope = this
                when (route) {
                TopLevelRoute.Home.route -> HomeScreen(
                    onNavigate = { target -> navigateTo(target) },
                    onNavigateToSettings = { navigateTo(TopLevelRoute.SettingsRoute.route) },
                    sharedScope = sharedScope,
                    animatedScope = animatedScope
                )

                TopLevelRoute.Schedule.route -> ScheduleScreen(
                    onBack = { goBack() },
                    sharedScope = sharedScope,
                    animatedScope = animatedScope
                )

                TopLevelRoute.Grade.route -> GradeScreen(
                    onBack = { goBack() },
                    sharedScope = sharedScope,
                    animatedScope = animatedScope
                )

                TopLevelRoute.EmptyRoom.route -> RoomScreen(
                    onBack = { goBack() },
                    sharedScope = sharedScope,
                    animatedScope = animatedScope
                )

                TopLevelRoute.Exam.route -> ExamScreen(
                    onBack = { goBack() },
                    sharedScope = sharedScope,
                    animatedScope = animatedScope
                )

                TopLevelRoute.SettingsRoute.route -> SettingsScreen(
                    onBack = { goBack() },
                    onNavigateToAdvanced = { navigateTo("advanced_settings") },
                    onNavigateToAbout = { navigateTo("about") }
                )

                "advanced_settings" -> AdvancedSettingsScreen(
                    onBack = { goBack() },
                    onNavigateToColorTheme = { navigateTo("color_theme") }
                )

                "color_theme" -> ColorThemeScreen(
                    onBack = { goBack() }
                )

                "about" -> AboutScreen(
                    onBack = { goBack() },
                    onNavigateToSystemUpdate = { navigateTo("system_update") }
                )

                "system_update" -> SystemUpdateScreen(
                    onBack = { goBack() }
                )

                "semester_setup" -> SemesterSetupScreen(
                    onBack = { goBack() }
                )

                LibraryRoutes.HOME -> LibraryHomeScreen(
                    onBack = { goBack() },
                    onNavigate = { target -> navigateTo(target) },
                    sharedScope = sharedScope,
                    animatedScope = animatedScope
                )

                LibraryRoutes.READER_CODE -> ReaderCodeScreen(
                    onBack = { goBack() }
                )

                LibraryRoutes.LOANS -> LoansScreen(
                    onBack = { goBack() }
                )

                LibraryRoutes.REQUESTS -> RequestsScreen(
                    onBack = { goBack() }
                )

                LibraryRoutes.FINES -> FinesScreen(
                    onBack = { goBack() }
                )

                LibraryRoutes.SEARCH -> SearchScreen(
                    onBack = { goBack() },
                    onOpenBib = { bibId, isbn, title, cover ->
                        navigateTo(LibraryRoutes.bibDetail(bibId, isbn, title, cover))
                    },
                    sharedScope = sharedScope,
                    animatedScope = animatedScope
                )

                LibraryRoutes.WISHLIST -> WishlistScreen(
                    onBack = { goBack() },
                    onOpenBib = { bibId, isbn, title, cover ->
                        navigateTo(LibraryRoutes.bibDetail(bibId, isbn, title, cover))
                    },
                    onGoSearch = { navigateTo(LibraryRoutes.SEARCH) },
                    sharedScope = sharedScope,
                    animatedScope = animatedScope
                )

                else -> {
                    val detailArgs = LibraryRoutes.parseBibDetail(route)
                    if (detailArgs != null) {
                        BibDetailScreen(
                            bibId = detailArgs.bibId,
                            isbn = detailArgs.isbn,
                            fallbackTitle = detailArgs.title,
                            fallbackCover = detailArgs.cover,
                            onBack = { goBack() },
                            sharedScope = sharedScope,
                            animatedScope = animatedScope
                        )
                    }
                }
                }
            }
        }
    }
}

// ============================================================
// Helper: 书籍详情路由判定
// ============================================================

/**
 * 是否是书籍详情页路由。
 *
 * 详情页与它的两个入口（检索结果、想看列表）之间走共享元素容器变换，
 * 页面级转场必须让位给容器变换，见 `MainAppContent` 里的 `transitionSpec`。
 */
private fun isBibDetailRoute(route: String): Boolean =
    LibraryRoutes.parseBibDetail(route) != null

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
