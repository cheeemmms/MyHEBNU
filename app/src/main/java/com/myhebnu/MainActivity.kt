package com.myhebnu

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
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
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myhebnu.data.local.preferences.UserPreferences
import com.myhebnu.data.repository.AuthRepository
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
import com.myhebnu.data.repository.BibDetailLoader
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

    /**
     * 书目详情预取器：点击卡片的那一刻就把详情请求发出去。
     *
     * 详情页在转场第 0 帧才进入组合，如果那时才发请求，220ms 的容器变换期间
     * 页面只有题名/作者/封面，馆藏与简介会在落位后的一帧里「长」出来。
     * 预取把这段等待移到点击瞬间，且与详情页共用同一条请求（不增加服务器压力）。
     */
    @Inject
    lateinit var bibDetailLoader: BibDetailLoader

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
                        pendingNavigation = pendingNavigation,
                        onPrefetchBibDetail = { bibId -> bibDetailLoader.prefetch(bibId) }
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
    pendingNavigation: kotlinx.coroutines.channels.Channel<String>,
    /** 点开书目详情前预取该书的详情数据，见 [MainAppContent]。 */
    onPrefetchBibDetail: (String) -> Unit = {}
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
            MainAppContent(
                pendingNavigation = pendingNavigation,
                onPrefetchBibDetail = onPrefetchBibDetail
            )
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
    pendingNavigation: kotlinx.coroutines.channels.Channel<String>,
    /** 点击卡片时预取详情；由 Activity 注入的 `BibDetailLoader` 提供，纯逻辑不感知 DI。 */
    onPrefetchBibDetail: (String) -> Unit = {}
) {
    // 官方 Navigation：返回栈、页面级转场、每个 back stack entry 的可保存状态
    // （列表滚动位置、输入框文字）都由框架负责，不再手写 currentRoute/backStack。
    val navController = rememberNavController()

    // Handle widget deep-link navigation：清空整个返回栈后跳到目标页
    LaunchedEffect(Unit) {
        for (route in pendingNavigation) {
            navController.navigate(route) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // 前进/后退统一交给 NavController：系统返回手势（含 Android 14 预测性返回）由它接管。
    val navigateTo: (String) -> Unit = { target -> navController.navigate(target) }
    val goBack: () -> Unit = { navController.popBackStack() }

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
            // NavHost 内部同样用 AnimatedContent 做转场，且每个 back stack entry 自带
            // SaveableStateHolder，列表滚动位置/输入框文字由框架保存，无需手写 provider。
            NavHost(
                navController = navController,
                startDestination = TopLevelRoute.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                page(TopLevelRoute.Home.route) {
                    HomeScreen(
                        onNavigate = { target -> navigateTo(target) },
                        onNavigateToSettings = { navigateTo(TopLevelRoute.SettingsRoute.route) },
                        sharedScope = sharedScope,
                        animatedScope = this
                    )
                }

                page(TopLevelRoute.Schedule.route) {
                    ScheduleScreen(
                        onBack = goBack,
                        sharedScope = sharedScope,
                        animatedScope = this
                    )
                }

                page(TopLevelRoute.Grade.route) {
                    GradeScreen(
                        onBack = goBack,
                        sharedScope = sharedScope,
                        animatedScope = this
                    )
                }

                page(TopLevelRoute.EmptyRoom.route) {
                    RoomScreen(
                        onBack = goBack,
                        sharedScope = sharedScope,
                        animatedScope = this
                    )
                }

                page(TopLevelRoute.Exam.route) {
                    ExamScreen(
                        onBack = goBack,
                        sharedScope = sharedScope,
                        animatedScope = this
                    )
                }

                page(TopLevelRoute.SettingsRoute.route) {
                    SettingsScreen(
                        onBack = goBack,
                        onNavigateToAdvanced = { navigateTo("advanced_settings") },
                        onNavigateToAbout = { navigateTo("about") }
                    )
                }

                page("advanced_settings") {
                    AdvancedSettingsScreen(
                        onBack = goBack,
                        onNavigateToColorTheme = { navigateTo("color_theme") }
                    )
                }

                page("color_theme") {
                    ColorThemeScreen(onBack = goBack)
                }

                page("about") {
                    AboutScreen(
                        onBack = goBack,
                        onNavigateToSystemUpdate = { navigateTo("system_update") }
                    )
                }

                page("system_update") {
                    SystemUpdateScreen(onBack = goBack)
                }

                page("semester_setup") {
                    SemesterSetupScreen(onBack = goBack)
                }

                page(LibraryRoutes.HOME) {
                    LibraryHomeScreen(
                        onBack = goBack,
                        onNavigate = { target -> navigateTo(target) },
                        sharedScope = sharedScope,
                        animatedScope = this
                    )
                }

                page(LibraryRoutes.READER_CODE) {
                    ReaderCodeScreen(onBack = goBack)
                }

                page(LibraryRoutes.LOANS) {
                    LoansScreen(onBack = goBack)
                }

                page(LibraryRoutes.REQUESTS) {
                    RequestsScreen(onBack = goBack)
                }

                page(LibraryRoutes.FINES) {
                    FinesScreen(onBack = goBack)
                }

                page(LibraryRoutes.SEARCH) {
                    SearchScreen(
                        onBack = goBack,
                        onOpenBib = { bibId, isbn, title, cover ->
                            onPrefetchBibDetail(bibId)
                            navigateTo(LibraryRoutes.bibDetail(bibId, isbn, title, cover))
                        },
                        sharedScope = sharedScope,
                        animatedScope = this
                    )
                }

                page(LibraryRoutes.WISHLIST) {
                    WishlistScreen(
                        onBack = goBack,
                        onOpenBib = { bibId, isbn, title, cover ->
                            onPrefetchBibDetail(bibId)
                            navigateTo(LibraryRoutes.bibDetail(bibId, isbn, title, cover))
                        },
                        onGoSearch = { navigateTo(LibraryRoutes.SEARCH) },
                        sharedScope = sharedScope,
                        animatedScope = this
                    )
                }

                // 书目详情：参数走 query 参数，避免书名里的分隔符/斜杠破坏路由匹配。
                page(
                    route = LibraryRoutes.BIB_DETAIL_PATTERN,
                    arguments = bibDetailArguments
                ) { entry ->
                    val args = entry.arguments
                    BibDetailScreen(
                        bibId = args?.getString("bibId").orEmpty(),
                        isbn = args?.getString("isbn").orEmpty(),
                        fallbackTitle = args?.getString("title").orEmpty(),
                        fallbackCover = args?.getString("cover").orEmpty(),
                        onBack = goBack,
                        sharedScope = sharedScope,
                        animatedScope = this
                    )
                }
            }
        }
    }
}

// ============================================================
// Navigation：目的地注册与页面级转场
// ============================================================

/** 书目详情的导航参数：query 写法，全部可选、缺省空串（避免空路径段匹配不上）。 */
private val bibDetailArguments = listOf(
    navArgument("bibId") { type = NavType.StringType; defaultValue = "" },
    navArgument("isbn") { type = NavType.StringType; defaultValue = "" },
    navArgument("title") { type = NavType.StringType; defaultValue = "" },
    navArgument("cover") { type = NavType.StringType; defaultValue = "" }
)

/** 这次页面切换是否牵涉书目详情（牵涉时用「内容与封面同帧显形」的时序）。 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.touchesBibDetail(): Boolean =
    initialState.isBibDetailEntry() || targetState.isBibDetailEntry()

private fun NavBackStackEntry.isBibDetailEntry(): Boolean =
    destination.route?.startsWith(LibraryRoutes.BIB_DETAIL_PREFIX) == true

/** 其余路由的默认入场：Compose 的 fadeIn(220ms, delay 90ms) + scaleIn(0.92→1)。 */
private fun defaultPageEnterTransition(): EnterTransition =
    fadeIn(tween(220, delayMillis = 90)) +
        scaleIn(tween(220, delayMillis = 90), initialScale = 0.92f)

/**
 * 注册一个目的地，并套用本项目的页面级转场。
 *
 * 牵涉书目详情时：进入 200ms 无延迟淡入（内容从第 0 帧就和封面一起显形），
 * 离开 60ms 让位，且不做 scaleIn（缩放会和容器几何抢同一段位移）。
 * 其余路由保持 Compose 默认观感。
 */
private fun NavGraphBuilder.page(
    route: String,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        arguments = arguments,
        enterTransition = {
            if (touchesBibDetail()) fadeIn(tween(200)) else defaultPageEnterTransition()
        },
        exitTransition = {
            if (touchesBibDetail()) fadeOut(tween(60)) else fadeOut(tween(90))
        },
        popEnterTransition = {
            if (touchesBibDetail()) fadeIn(tween(200)) else defaultPageEnterTransition()
        },
        popExitTransition = {
            if (touchesBibDetail()) fadeOut(tween(60)) else fadeOut(tween(90))
        },
        content = content
    )
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
