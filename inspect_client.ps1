$file = "src\quiz\Client.java"
$content = [System.IO.File]::ReadAllText($file, [System.Text.Encoding]::UTF8)

# Find matchId=null in context of CLOSED case
$idx = $content.IndexOf('matchId=null;')
Write-Host "matchId=null at: $idx"
$snippet = $content.Substring($idx, 150)
$bytes = [System.Text.Encoding]::UTF8.GetBytes($snippet)
$hex = ($bytes | ForEach-Object { $_.ToString("X2") }) -join " "
Write-Host "HEX: $hex"
