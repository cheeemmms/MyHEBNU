package com.myhebnu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myhebnu.data.repository.AuthRepository
import com.myhebnu.ui.auth.LoginScreen
import com.myhebnu.ui.auth.LoginViewModel
import com.myhebnu.ui.exam.ExamScreen
import com.myhebnu.ui.grade.GradeScreen
import com.myhebnu.ui.navigation.DrawerContent
import com.myhebnu.ui.navigation.TopLevelRoute
import com.myhebnu.ui.navigation.topLevelRoutes
import com.myhebnu.ui.room.RoomScreen
import com.myhebnu.ui.schedule.ScheduleScreen
import com.myhebnu.ui.theme.MyHEBNUTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyHEBNUTheme {
                MyHEBNUApp(authRepository = authRepository)
            }
        }
    }
}

@Composable
fun MyHEBNUApp(
    authRepository: AuthRepository
) {
    var isLoggedIn by remember { mutableStateOf<Boolean?>(null) }
    val loginViewModel: LoginViewModel = hiltViewModel()

    // Check auth state on launch
    LaunchedEffect(Unit) {
        android.util.Log.w("MyHEBNU", "=== 开始检查登录状态 ===")
        val result = authRepository.hasValidSession()
        android.util.Log.w("MyHEBNU", "hasValidSession = $result")
        isLoggedIn = result
        android.util.Log.w("MyHEBNU", "=== 检查登录状态完成 ===")
    }

    // Listen for login success from LoginViewModel
    val loginState by loginViewModel.uiState.collectAsState()
    LaunchedEffect(loginState.isLoggedIn) {
        if (loginState.isLoggedIn) {
            android.util.Log.w("MyHEBNU", "LoginViewModel reports logged in")
            isLoggedIn = true
        }
    }

    android.util.Log.w("MyHEBNU", "Composing MyHEBNUApp, isLoggedIn = $isLoggedIn")
    when (isLoggedIn) {
        null -> {
            android.util.Log.w("MyHEBNU", "Showing loading spinner")
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        false -> {
            android.util.Log.w("MyHEBNU", "Showing LoginScreen")
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { isLoggedIn = true }
            )
        }
        true -> {
            android.util.Log.w("MyHEBNU", "Showing MainAppContent")
            MainAppContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent() {
    var currentRoute by remember { mutableStateOf(TopLevelRoute.Schedule.route) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                currentRoute = currentRoute,
                onRouteSelected = { route ->
                    scope.launch { drawerState.close() }
                    currentRoute = route.route
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val titleRes = topLevelRoutes.find { it.route == currentRoute }
                            ?.titleResId ?: R.string.app_name
                        Text(text = stringResource(titleRes))
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.menu)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            AnimatedContent(
                targetState = currentRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) { route ->
                when (route) {
                    TopLevelRoute.Schedule.route -> ScheduleScreen()
                    TopLevelRoute.Grade.route -> GradeScreen()
                    TopLevelRoute.EmptyRoom.route -> RoomScreen()
                    TopLevelRoute.Exam.route -> ExamScreen()
                    else -> ScheduleScreen()
                }
            }
        }
    }
}
