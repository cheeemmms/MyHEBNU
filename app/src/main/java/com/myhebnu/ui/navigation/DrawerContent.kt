package com.myhebnu.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.myhebnu.R

@Composable
fun DrawerContent(
    currentRoute: String?,
    onRouteSelected: (TopLevelRoute) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(modifier = modifier) {
        // App branding header
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))
        Spacer(Modifier.height(8.dp))

        // Navigation items
        topLevelRoutes.forEach { route ->
            val selected = currentRoute == route.route
            NavigationDrawerItem(
                icon = {
                    Icon(
                        imageVector = if (selected) route.selectedIcon else route.unselectedIcon,
                        contentDescription = stringResource(route.titleResId)
                    )
                },
                label = { Text(stringResource(route.titleResId)) },
                selected = selected,
                onClick = { onRouteSelected(route) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Spacer(Modifier.weight(1f))

        // Settings entry at bottom
        HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp))
        NavigationDrawerItem(
            icon = {
                Icon(
                    imageVector = TopLevelRoute.SettingsRoute.selectedIcon,
                    contentDescription = stringResource(R.string.settings)
                )
            },
            label = { Text(stringResource(R.string.settings)) },
            selected = currentRoute == TopLevelRoute.SettingsRoute.route,
            onClick = onSettingsClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
