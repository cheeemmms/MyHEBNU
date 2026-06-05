# 河北师范大学校徽图标转换脚本
$ErrorActionPreference = "Stop"

$sourceImage = "C:\Users\yongl\Downloads\20140925173501647.png"
$baseDir = "D:\Personal_file\VibeCoding\Program\My-University\app\src\main\res"

if (-not (Test-Path $sourceImage)) {
    Write-Host "ERROR: Source image not found: $sourceImage" -ForegroundColor Red
    exit 1
}

Add-Type -AssemblyName System.Drawing

$iconSizes = @{
    "mipmap-mdpi" = 48
    "mipmap-hdpi" = 72
    "mipmap-xhdpi" = 96
    "mipmap-xxhdpi" = 144
    "mipmap-xxxhdpi" = 192
}

Write-Host "Processing school badge..." -ForegroundColor Green

foreach ($folder in $iconSizes.Keys) {
    $size = $iconSizes[$folder]
    $targetPath = Join-Path $baseDir "$folder\ic_launcher.png"
    $targetRoundPath = Join-Path $baseDir "$folder\ic_launcher_round.png"

    try {
        $original = [System.Drawing.Image]::FromFile($sourceImage)
        $bitmap = New-Object System.Drawing.Bitmap($size, $size)
        $graphics = [System.Drawing.Graphics]::FromImage($bitmap)

        $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $graphics.Clear([System.Drawing.Color]::White)

        $width = $original.Width
        $height = $original.Height
        $scale = [Math]::Min($size / $width, $size / $height) * 0.9
        $newWidth = [int]($width * $scale)
        $newHeight = [int]($height * $scale)
        $x = ($size - $newWidth) / 2
        $y = ($size - $newHeight) / 2

        $graphics.DrawImage($original, $x, $y, $newWidth, $newHeight)
        $bitmap.Save($targetPath, [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Host "Created $folder/ic_launcher.png (${size}x${size})" -ForegroundColor Cyan

        $roundBitmap = New-Object System.Drawing.Bitmap($size, $size)
        $roundGraphics = [System.Drawing.Graphics]::FromImage($roundBitmap)
        $roundGraphics.Clear([System.Drawing.Color]::Transparent)
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddEllipse(0, 0, $size, $size)
        $roundGraphics.SetClip($path)
        $roundGraphics.DrawImage($original, $x, $y, $newWidth, $newHeight)
        $roundBitmap.Save($targetRoundPath, [System.Drawing.Imaging.ImageFormat]::Png)
        Write-Host "Created $folder/ic_launcher_round.png (${size}x${size})" -ForegroundColor Cyan

        $graphics.Dispose()
        $roundGraphics.Dispose()
        $bitmap.Dispose()
        $roundBitmap.Dispose()
        $original.Dispose()
    }
    catch {
        Write-Host "Error processing $folder: $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Done!" -ForegroundColor Green
