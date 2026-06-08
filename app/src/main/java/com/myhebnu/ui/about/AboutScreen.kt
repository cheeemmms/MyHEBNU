package com.myhebnu.ui.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myhebnu.BuildConfig
import com.myhebnu.R

private const val GITHUB_REPO_URL = "https://github.com/cheeemmms/MyHEBNU"
private const val LICENSE_URL = "https://www.gnu.org/licenses/agpl-3.0.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onNavigateToSystemUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLicensesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // App icon
            Image(
                painter = painterResource(R.drawable.app_icon),
                contentDescription = "MyHEBNU",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(16.dp))

            // App name
            Text(
                text = "MyHEBNU",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            // Version
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(32.dp))

            // Card: Community & Source
            AboutCard {
                val qqNumber = stringResource(R.string.qq_placeholder)
                val emailAddr = stringResource(R.string.version_placeholder)

                AboutCardItem(
                    headline = stringResource(R.string.about_view_source),
                    supporting = stringResource(R.string.about_view_source_desc),
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL)))
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                AboutCardItem(
                    headline = stringResource(R.string.about_join_group),
                    supporting = qqNumber,
                    icon = Icons.Filled.Group,
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("QQ群号", qqNumber))
                        Toast.makeText(context, R.string.qq_group_copied, Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                AboutCardItem(
                    headline = stringResource(R.string.about_contact_email),
                    supporting = emailAddr,
                    icon = Icons.Filled.Email,
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$emailAddr")
                            })
                        } catch (_: Exception) {}
                    }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Card: Licenses
            AboutCard {
                AboutCardItem(
                    headline = stringResource(R.string.about_license),
                    supporting = stringResource(R.string.about_license_desc),
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(LICENSE_URL)))
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                AboutCardItem(
                    headline = stringResource(R.string.about_third_party_licenses),
                    onClick = { showLicensesDialog = true }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Card: System & Updates
            AboutCard {
                AboutCardItem(
                    headline = stringResource(R.string.about_system_updates),
                    supporting = stringResource(R.string.about_system_updates_desc),
                    icon = Icons.Filled.Settings,
                    onClick = onNavigateToSystemUpdate
                )
            }

            Spacer(Modifier.height(32.dp))
        }

        // Licenses dialog
        if (showLicensesDialog) {
            AlertDialog(
                onDismissRequest = { showLicensesDialog = false },
                title = { Text(stringResource(R.string.about_third_party_licenses)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(LICENSES_TEXT, style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLicensesDialog = false }) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// Licenses text
// ═══════════════════════════════════════════════════════════════

private val LICENSES_TEXT = """
    Jetpack Compose — Apache 2.0
    https://developer.android.com/jetpack/compose

    Material Design 3 — Apache 2.0
    https://m3.material.io

    Hilt / Dagger — Apache 2.0
    https://dagger.dev/hilt

    Retrofit — Apache 2.0
    https://square.github.io/retrofit

    OkHttp — Apache 2.0
    https://square.github.io/okhttp

    Room — Apache 2.0
    https://developer.android.com/training/data-storage/room

    Kotlin Coroutines — Apache 2.0
    https://github.com/Kotlin/kotlinx.coroutines

    Coil — Apache 2.0
    https://coil-kt.github.io/coil

    Jsoup — MIT
    https://jsoup.org

    Glance — Apache 2.0
    https://developer.android.com/jetpack/compose/glance

    DataStore — Apache 2.0
    https://developer.android.com/topic/libraries/architecture/datastore

    WorkManager — Apache 2.0
    https://developer.android.com/topic/libraries/architecture/workmanager

    EncryptedSharedPreferences — Apache 2.0
    https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences
""".trimIndent()

// ═══════════════════════════════════════════════════════════════
// Reusable components
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AboutCard(content: @Composable ColumnScope.() -> Unit) {
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
private fun AboutCardItem(
    headline: String,
    supporting: String = "",
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp)
    } else {
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
    }

    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(headline, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (supporting.isNotEmpty()) {
                Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onClick != null) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(20.dp))
        }
    }
}
