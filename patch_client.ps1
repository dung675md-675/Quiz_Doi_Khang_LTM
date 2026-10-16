$file = "src\quiz\Client.java"
$content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)

# The CLOSED case has 4-space indent
$old3 = "matchId=null;`r`n    ToastManager.info(frame, Wire.field(a,1));`r`n    showLobby();"
$new3 = "matchId=null;`r`n    SoundManager.stopCountdown();`r`n    ToastManager.info(frame, Wire.field(a,1));`r`n    showLobby();"
if ($content.Contains($old3)) {
    $content = $content.Replace($old3, $new3)
    Write-Host "Patch 3 OK (stopCountdown in CLOSED)"
} else {
    Write-Host "Still not found. Check hex output above for exact pattern."
}

[System.IO.File]::WriteAllText($file, $content, [System.Text.Encoding]::UTF8)
Write-Host "Done."
