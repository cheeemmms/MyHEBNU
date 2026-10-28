package com.myhebnu.ui.library

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myhebnu.ui.design.InlineNotice
import com.myhebnu.ui.design.LoadingBlock
import com.myhebnu.ui.design.ScreenScaffold
import com.myhebnu.ui.design.Spacing
import com.myhebnu.ui.design.StateBlock

/**
 * 动态借书码。
 *
 * 这一页刻意做成「一张票」：白底、居中、大留白，二维码是绝对主体。
 * 白底不是装饰——扫码器需要真实的白底黑码对比度，深色主题下若跟随背景会直接扫不出来。
 * 因此票面内所有颜色都写死（白纸 + 近黑码），并在这页临时把屏幕亮度拉满、保持常亮。
 */
@Composable
fun ReaderCodeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderCodeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    KeepScreenBrightAndOn()

    ScreenScaffold(
        title = "借书码",
        modifier = modifier,
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(Spacing.lg))

            when {
                uiState.isLoading && uiState.code.isBlank() -> LoadingBlock()

                uiState.code.isBlank() -> StateBlock(
                    title = uiState.error ?: "借书码获取失败",
                    description = "借书码由图书馆系统实时下发，需要联网才能取到。",
                    actionLabel = "重试",
                    onAction = viewModel::refresh
                )

                else -> {
                    TicketSurface(
                        code = uiState.code,
                        name = uiState.name,
                        studentId = uiState.studentId
                    )

                    Spacer(Modifier.height(Spacing.lg))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${uiState.secondsLeft} 秒后自动换码",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = viewModel::refresh) {
                            Text("立即刷新")
                        }
                    }

                    if (uiState.error != null) {
                        InlineNotice(
                            text = uiState.error.orEmpty(),
                            actionLabel = "重试",
                            onAction = viewModel::refresh
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xxxl))
        }
    }
}

@Composable
private fun TicketSurface(
    code: String,
    name: String,
    studentId: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name.ifBlank { "读者" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TicketInk
            )
            if (studentId.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    text = studentId,
                    style = MaterialTheme.typography.bodySmall,
                    color = TicketInkMuted
                )
            }

            Spacer(Modifier.height(Spacing.xl))

            QrCodeImage(
                content = code,
                modifier = Modifier.size(220.dp)
            )

            Spacer(Modifier.height(Spacing.xl))

            BarcodeImage(
                content = code,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Spacing.md))

            Text(
                text = code.chunked(4).joinToString(" "),
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                color = TicketInk
            )
        }
    }
}

/** 票面内的近黑与灰，不随主题变化——扫码需要稳定对比度。 */
private val TicketInk = Color(0xFF1A1A1A)
private val TicketInkMuted = Color(0xFF6B6B6B)

/**
 * 借书码页期间保持屏幕常亮，并把亮度拉到最亮；离开时恢复系统原状。
 *
 * 扫码失败最常见的原因就是屏幕太暗，所以这里不做「用户可调」的开关，直接用最稳的状态。
 */
@Composable
private fun KeepScreenBrightAndOn() {
    val view = LocalView.current

    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val previousBrightness = window?.attributes?.screenBrightness ?: 0f

        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window?.let { w ->
            val params = w.attributes
            params.screenBrightness = 1f
            w.attributes = params
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window?.let { w ->
                val params = w.attributes
                params.screenBrightness = previousBrightness
                w.attributes = params
            }
        }
    }
}
