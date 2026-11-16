param(
    [string]$Source = "C:\CuteDroid\UraliconMya.png",
    [string]$Prefix = "ic_launcher"
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

Add-Type -TypeDefinition @"
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

public static class IconMasker
{
    public static Bitmap CreateMonochrome(Bitmap source, Color background, int lowThreshold, int highThreshold)
    {
        int width = source.Width;
        int height = source.Height;
        Bitmap result = new Bitmap(width, height, PixelFormat.Format32bppArgb);
        Rectangle rect = new Rectangle(0, 0, width, height);

        BitmapData srcData = source.LockBits(rect, ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        BitmapData dstData = result.LockBits(rect, ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);

        int srcStride = srcData.Stride;
        int dstStride = dstData.Stride;
        byte[] src = new byte[srcStride * height];
        byte[] dst = new byte[dstStride * height];
        Marshal.Copy(srcData.Scan0, src, 0, src.Length);

        int range = Math.Max(1, highThreshold - lowThreshold);

        for (int y = 0; y < height; y++)
        {
            int srcRow = y * srcStride;
            int dstRow = y * dstStride;
            for (int x = 0; x < width; x++)
            {
                int si = srcRow + x * 4;
                int di = dstRow + x * 4;

                byte b = src[si];
                byte g = src[si + 1];
                byte r = src[si + 2];
                byte a = src[si + 3];

                int dr = r - background.R;
                int dg = g - background.G;
                int db = b - background.B;
                double distance = Math.Sqrt(dr * dr + dg * dg + db * db);

                double alpha = (distance - lowThreshold) / (double)range;
                if (alpha < 0) alpha = 0;
                if (alpha > 1) alpha = 1;
                alpha *= a / 255.0;

                dst[di] = 255;
                dst[di + 1] = 255;
                dst[di + 2] = 255;
                dst[di + 3] = (byte)Math.Round(alpha * 255);
            }
        }

        Marshal.Copy(dst, 0, dstData.Scan0, dst.Length);
        source.UnlockBits(srcData);
        result.UnlockBits(dstData);
        return result;
    }
}
"@ -ReferencedAssemblies System.Drawing

if (-not (Test-Path -LiteralPath $Source)) {
    throw "Icon source not found: $Source"
}

$resDir = Join-Path $PSScriptRoot "..\app\src\main\res"
$densities = @{
    "mdpi" = 48
    "hdpi" = 72
    "xhdpi" = 96
    "xxhdpi" = 144
    "xxxhdpi" = 192
}

$sourceImage = [System.Drawing.Image]::FromFile((Resolve-Path -LiteralPath $Source))
$sourceBitmap = New-Object System.Drawing.Bitmap $sourceImage.Width, $sourceImage.Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$sourceGraphics = [System.Drawing.Graphics]::FromImage($sourceBitmap)
$sourceGraphics.DrawImage($sourceImage, 0, 0, $sourceImage.Width, $sourceImage.Height)
$sourceGraphics.Dispose()

$background = $sourceBitmap.GetPixel(0, 0)
$monochrome = [IconMasker]::CreateMonochrome($sourceBitmap, $background, 35, 95)

function Save-Square {
    param(
        [System.Drawing.Image]$Image,
        [int]$Size,
        [string]$Path,
        [double]$ContentScale = 1.0,
        [bool]$Transparent = $false
    )

    $bitmap = New-Object System.Drawing.Bitmap $Size, $Size
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

    if ($Transparent) {
        $graphics.Clear([System.Drawing.Color]::Transparent)
    } else {
        $graphics.Clear([System.Drawing.Color]::White)
    }

    $content = [int][Math]::Round($Size * $ContentScale)
    $offset = [int][Math]::Round(($Size - $content) / 2.0)
    $graphics.DrawImage($Image, $offset, $offset, $content, $content)
    $graphics.Dispose()
    $bitmap.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

try {
    foreach ($entry in $densities.GetEnumerator()) {
        $density = $entry.Key
        $size = $entry.Value
        $dir = Join-Path $resDir "mipmap-$density"
        New-Item -ItemType Directory -Path $dir -Force | Out-Null

        Save-Square -Image $sourceImage -Size $size -Path (Join-Path $dir "$Prefix.png")
        Save-Square -Image $sourceImage -Size $size -Path (Join-Path $dir "${Prefix}_round.png")

        $foregroundSize = [int][Math]::Round($size * 108 / 48)
        Save-Square -Image $sourceImage -Size $foregroundSize -Path (Join-Path $dir "${Prefix}_foreground.png") -ContentScale (72 / 108) -Transparent $true
        Save-Square -Image $monochrome -Size $foregroundSize -Path (Join-Path $dir "${Prefix}_monochrome.png") -ContentScale (72 / 108) -Transparent $true
    }

    Write-Output "Icons generated: $Prefix from $Source (background #$($background.R.ToString('X2'))$($background.G.ToString('X2'))$($background.B.ToString('X2')))"
} finally {
    $monochrome.Dispose()
    $sourceBitmap.Dispose()
    $sourceImage.Dispose()
}
