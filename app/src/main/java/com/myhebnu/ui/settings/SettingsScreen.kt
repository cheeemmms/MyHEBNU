package com.myhebnu.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myhebnu.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToAdvanced: () -> Unit,
    onNavigateToWebViewLogin: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var themeDropdownExpanded by remember { mutableStateOf(false) }
    val themeOptions = listOf(
        "system" to "跟随系统",
        "light" to "浅色模式",
        "dark" to "深色模式"
    )
    val currentLabel = themeOptions.find { it.first == uiState.themeMode }?.second ?: "跟随系统"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ═══ Appearance ═══
            SettingsSectionHeader(title = stringResource(R.string.settings_appearance))
            SettingsCard {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = themeDropdownExpanded,
                        onExpandedChange = { themeDropdownExpanded = it }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .clickable { themeDropdownExpanded = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "主题模式",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        ExposedDropdownMenu(
                            expanded = themeDropdownExpanded,
                            onDismissRequest = { themeDropdownExpanded = false }
                        ) {
                            themeOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setThemeMode(value)
                                        themeDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ═══ Academics ═══
            SettingsSectionHeader(title = stringResource(R.string.settings_academics))
            SettingsCard {
                SettingsInfoItem(
                    title = stringResource(R.string.current_week),
                    value = "第 ${uiState.currentWeek} 周"
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                SettingsInfoItem(
                    title = "当前学期",
                    value = "${uiState.semesterYear}-${uiState.semesterTerm}"
                )
            }

            // ═══ Advanced ═══
            SettingsSectionHeader(title = stringResource(R.string.settings_advanced))
            SettingsCard {
                SettingsNavigateItem(
                    title = stringResource(R.string.settings_advanced),
                    onClick = onNavigateToAdvanced
                )
            }

            // ═══ Account ═══
            SettingsSectionHeader(title = "账号")
            SettingsCard {
                SettingsNavigateItem(
                    title = "浏览器登录",
                    onClick = onNavigateToWebViewLogin
                )
            }

            // ═══ About ═══
            SettingsSectionHeader(title = stringResource(R.string.settings_about))
            SettingsCard {
                SettingsNavigateItem(
                    title = stringResource(R.string.about_title),
                    onClick = onNavigateToAbout
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

}

// ═══════════════════════════════════════════════════════════════
// Reusable components
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsInfoItem(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (value.isNotEmpty()) {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsNavigateItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
