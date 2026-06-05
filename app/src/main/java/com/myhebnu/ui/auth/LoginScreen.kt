package com.myhebnu.ui.auth

import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.myhebnu.R

/**
 * SSO login screen using Chrome Custom Tabs or system browser.
 *
 * Strategy: open the CAS login page in the system browser (which fully supports
 * JS/CSS), then capture cookies via CookieManager when returning to the app.
 *
 * This avoids WebView rendering issues on Android 16+ / Xiaomi devices.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Navigate away when logged in
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.loginUrl.isNotEmpty() -> {
                // Show a UI explaining the login flow
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.login_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = {
                            openCasInBrowser(context, uiState.loginUrl)
                            // After returning, check cookies
                            viewModel.checkBrowserLogin()
                        }
                    ) {
                        Text("打开统一认证登录")
                    }

                    Spacer(Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                            openCasInBrowser(context, uiState.loginUrl)
                            viewModel.checkBrowserLogin()
                        }
                    ) {
                        Text("重新打开登录页面")
                    }
                }
            }
        }

        // Error
        uiState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = viewModel::clearError) {
                        Text(stringResource(R.string.retry))
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

private fun openCasInBrowser(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("MyHEBNU", "Failed to open browser: ${e.message}")
    }
}
