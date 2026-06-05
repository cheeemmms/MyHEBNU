package com.myhebnu.ui.auth

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.myhebnu.R

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
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
                // Loading state — checking stored session
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.loginUrl.isNotEmpty() -> {
                // Show WebView for CAS login — fullscreen, no chrome
                AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                // Enable JavaScript (CAS login page is JS-rendered)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowContentAccess = true
                                settings.allowFileAccess = false

                                // Critical: allow mixed content (HTTP resources on HTTP page)
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                                // Don't block any loads
                                settings.blockNetworkLoads = false
                                settings.blockNetworkImage = false

                                // Viewport & zoom
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true

                                // Modern mobile UA for proper page rendering
                                settings.userAgentString =
                                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                        android.util.Log.w("MyHEBNU", "WebView onPageStarted: $url")
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        // Track URL changes for login success detection
                                        request?.url?.toString()?.let { url ->
                                            android.util.Log.w("MyHEBNU", "WebView shouldOverrideUrlLoading: $url")
                                            viewModel.onWebViewUrlChanged(url)
                                        }
                                        return false // Let WebView handle the navigation
                                    }

                                    @Deprecated("Deprecated in Java")
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        url: String?
                                    ): Boolean {
                                        android.util.Log.w("MyHEBNU", "WebView shouldOverrideUrlLoading(deprecated): $url")
                                        url?.let { viewModel.onWebViewUrlChanged(it) }
                                        return false
                                    }

                                    override fun onPageFinished(
                                        view: WebView?,
                                        url: String?
                                    ) {
                                        android.util.Log.w("MyHEBNU", "WebView onPageFinished: $url")
                                        url?.let { viewModel.onWebViewUrlChanged(it) }
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: android.webkit.WebResourceError?
                                    ) {
                                        val desc = error?.description?.toString()
                                        val url = request?.url?.toString()
                                        android.util.Log.w("MyHEBNU", "WebView onReceivedError: desc=$desc url=$url")
                                        viewModel.onLoginError(desc)
                                    }
                                }

                                loadUrl(uiState.loginUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
            }
        }

        // Error snackbar
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
