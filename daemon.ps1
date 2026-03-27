param(
    [string]$Dir = "",
    [int]$MaxImages = 20
)

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::Out.Flush()

if (-not $Dir) {
    $Dir = "$env:USERPROFILE\.claude\clipTemp"
}
if (-not (Test-Path $Dir)) {
    New-Item -ItemType Directory -Path $Dir | Out-Null
}

Write-Host "READY"
[Console]::Out.Flush()

while ($true) {
    $line = [Console]::In.ReadLine()
    if ($null -eq $line -or $line -eq "exit") { break }
    if ($line -ne "save") { continue }

    $obj = [System.Windows.Forms.Clipboard]::GetDataObject()

    # Priority 1: FileDropList (files copied from Explorer)
    if ($obj.GetDataPresent([System.Windows.Forms.DataFormats]::FileDrop)) {
        $files = $obj.GetData([System.Windows.Forms.DataFormats]::FileDrop)
        $result = ($files | ForEach-Object { $_ }) -join " "
        Write-Host $result
        [Console]::Out.Flush()
        continue
    }

    # Priority 2: Text in clipboard -> treat as text copy, let extension handle it
    $hasText = $obj.GetDataPresent([System.Windows.Forms.DataFormats]::UnicodeText)
    if (-not $hasText) {
        $hasText = $obj.GetDataPresent([System.Windows.Forms.DataFormats]::Text)
    }
    if ($hasText) {
        Write-Host "NONE"
        [Console]::Out.Flush()
        continue
    }

    # Priority 3: Raw Bitmap (screenshot, no text, no file)
    $img = $null
    if ($obj.GetDataPresent("System.Drawing.Bitmap")) {
        $img = $obj.GetData("System.Drawing.Bitmap")
    } elseif ($obj.GetDataPresent("Bitmap")) {
        $img = $obj.GetData("Bitmap")
    }

    if ($null -eq $img) {
        Write-Host "NONE"
        [Console]::Out.Flush()
        continue
    }

    $bmp = New-Object System.Drawing.Bitmap($img.Width, $img.Height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.DrawImage($img, 0, 0, $img.Width, $img.Height)
    $g.Dispose()

    $ms = New-Object System.IO.MemoryStream
    $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $bytes = $ms.ToArray()
    $ms.Dispose()

    $hashBytes = [System.Security.Cryptography.MD5]::Create().ComputeHash($bytes)
    $hash = [System.BitConverter]::ToString($hashBytes).Replace("-", "").ToLower().Substring(0, 12)
    $imgPath = "$Dir\claude_img_$hash.png"

    if (-not (Test-Path $imgPath)) {
        [System.IO.File]::WriteAllBytes($imgPath, $bytes)

        $existingFiles = Get-ChildItem $Dir -Filter "claude_img_*.png" | Sort-Object LastWriteTime
        if ($existingFiles.Count -gt $MaxImages) {
            $existingFiles | Select-Object -First ($existingFiles.Count - $MaxImages) | Remove-Item -Force
        }
    }

    Write-Host $imgPath
    [Console]::Out.Flush()
}
