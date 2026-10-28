package com.myhebnu.ui.library

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.oned.Code128Writer
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * 借书码渲染。
 *
 * 学校「我的图书馆」的借书码是一串 14 位的动态口令（`EC` + 12 位大写字母数字），
 * 条形码与二维码承载**同一个**字符串——所以两者必须同源渲染，任何一侧不同步都会导致扫码失败。
 *
 * 生成后按 1 模块 = 1 像素的位图交给 Compose，由 [FilterQuality.None] 放大，
 * 这样边缘不会被插值糊掉（扫码识别率的关键）。
 */

@Composable
fun BarcodeImage(
    content: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(content) { runCatching { encodeBarcode(content) }.getOrNull() }
    if (bitmap == null) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "借书条形码",
            contentScale = ContentScale.FillWidth,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3.2f)
        )
    }
}

@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(content) { runCatching { encodeQrCode(content) }.getOrNull() }
    if (bitmap == null) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(8.dp)
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "借书二维码",
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.None,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ---------------------------------------------------------------------------
// 编码
// ---------------------------------------------------------------------------

private fun encodeBarcode(content: String): ImageBitmap {
    val hints = mapOf(
        EncodeHintType.MARGIN to 0,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    // 高度只是像素密度，实际显示由 Compose 缩放
    val matrix = Code128Writer().encode(
        content,
        BarcodeFormat.CODE_128,
        360,
        110,
        hints
    )
    return matrix.toImageBitmap()
}

private fun encodeQrCode(content: String): ImageBitmap {
    val hints = mapOf(
        EncodeHintType.MARGIN to 0,
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 480, 480, hints)
    return matrix.toImageBitmap()
}

private fun BitMatrix.toImageBitmap(): ImageBitmap {
    val w = width
    val h = height
    val pixels = IntArray(w * h)
    for (y in 0 until h) {
        val offset = y * w
        for (x in 0 until w) {
            pixels[offset + x] = if (get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }
    val bitmap = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    return bitmap.asImageBitmap()
}
