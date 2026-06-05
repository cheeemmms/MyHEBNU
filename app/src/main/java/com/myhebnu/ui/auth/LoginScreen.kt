package com.myhebnu.ui.auth

import android.annotation.SuppressLint
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
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
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.allowContentAccess = true
                            settings.allowFileAccess = true
                            settings.allowUniversalAccessFromFileURLs = true
                            settings.allowFileAccessFromFileURLs = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.blockNetworkLoads = false
                            settings.blockNetworkImage = false

                            @Suppress("DEPRECATION")
                            settings.safeBrowsingEnabled = false
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.setGeolocationEnabled(false)
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.textZoom = 100

                            settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                            // Capture JS console logs for debugging
                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                                    android.util.Log.e(
                                        "MyHEBNU",
                                        "JS [${msg.messageLevel()}] ${msg.sourceId()}:${msg.lineNumber()} — ${msg.message()}"
                                    )
                                    return true
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?, url: String?,
                                    favicon: android.graphics.Bitmap?
                                ) {
                                    android.util.Log.w("MyHEBNU", "onPageStarted: $url")
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    request?.url?.toString()?.let { url ->
                                        android.util.Log.w("MyHEBNU", "shouldOverrideUrlLoading: $url")
                                        viewModel.onWebViewUrlChanged(url)
                                    }
                                    return false
                                }

                                @Deprecated("Deprecated in Java")
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?, url: String?
                                ): Boolean {
                                    android.util.Log.w("MyHEBNU", "shouldOverrideUrlLoading(dep): $url")
                                    url?.let { viewModel.onWebViewUrlChanged(it) }
                                    return false
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    android.util.Log.w("MyHEBNU", "onPageFinished: $url")
                                    url?.let { viewModel.onWebViewUrlChanged(it) }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: android.webkit.WebResourceError?
                                ) {
                                    val desc = error?.description?.toString()
                                    val errUrl = request?.url?.toString()
                                    android.util.Log.e("MyHEBNU", "onReceivedError: $desc url=$errUrl")
                                    viewModel.onLoginError(desc)
                                }
                            }

                            android.util.Log.w("MyHEBNU", "Loading URL: ${uiState.loginUrl}")
                            loadUrl(uiState.loginUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

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
