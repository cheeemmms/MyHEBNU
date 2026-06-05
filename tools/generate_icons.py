"""Generate Android launcher icons from source PNG for MyHEBNU.
Creates legacy mipmap PNGs and adaptive icon foreground PNGs.
"""
import os
from PIL import Image

SRC = r"C:\Users\yongl\Downloads\20140925173501647.png"
DST = r"D:\Personal_file\VibeCoding\Program\My-University\app\src\main\res"

# Android launcher icon presets
# Adaptive icon foreground: 108dp base, safe zone is 72dp center
ADAPTIVE_SIZES = {
    "mdpi":    (108, "mipmap-mdpi"),
    "hdpi":    (162, "mipmap-hdpi"),   # 108 * 1.5
    "xhdpi":   (216, "mipmap-xhdpi"),  # 108 * 2
    "xxhdpi":  (324, "mipmap-xxhdpi"), # 108 * 3
    "xxxhdpi": (432, "mipmap-xxxhdpi"),# 108 * 4
}

# Legacy icon: 48dp base
LEGACY_SIZES = {
    "mdpi":    48,
    "hdpi":    72,
    "xhdpi":   96,
    "xxhdpi":  144,
    "xxxhdpi": 192,
}

img = Image.open(SRC).convert("RGBA")
print(f"Source: {img.size} {img.mode}")

# ---- Adaptive icon foregrounds (ic_launcher_foreground.png) ----
for density, (size_px, dir_name) in ADAPTIVE_SIZES.items():
    out_dir = os.path.join(DST, dir_name)
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "ic_launcher_foreground.png")
    # Resize to fill 108dp with content in the safe zone (72dp = 2/3 of 108)
    safe_ratio = 72.0 / 108.0
    safe_px = int(size_px * safe_ratio)
    # Scale image so content fits within safe zone, then pad to full 108dp
    img_resized = img.resize((safe_px, safe_px), Image.LANCZOS)
    canvas = Image.new("RGBA", (size_px, size_px), (0, 0, 0, 0))
    offset_x = (size_px - safe_px) // 2
    offset_y = (size_px - safe_px) // 2
    canvas.paste(img_resized, (offset_x, offset_y))
    canvas.save(out_path, "PNG")
    print(f"  Adaptive foreground {density}: {size_px}x{size_px} → {out_path}")

# ---- Legacy icons (ic_launcher.png) ----
for density, size_px in LEGACY_SIZES.items():
    dir_name = f"mipmap-{density}"
    out_dir = os.path.join(DST, dir_name)
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "ic_launcher.png")
    img_resized = img.resize((size_px, size_px), Image.LANCZOS)
    img_resized.save(out_path, "PNG")
    print(f"  Legacy {density}: {size_px}x{size_px} → {out_path}")

print("Done. Now update XML drawables to reference the PNG.")
