package com.myhebnu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyHEBNUTheme {
                MyHEBNUApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyHEBNUApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                currentRoute = currentRoute,
                onRouteSelected = { route ->
                    scope.launch { drawerState.close() }
                    navController.navigate(route.route) {
                        popUpTo(TopLevelRoute.Schedule.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
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
