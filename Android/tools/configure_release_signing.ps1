[CmdletBinding()]
param(
    [string]$PrimaryDirectory = (Join-Path $env:USERPROFILE "AVORA-Signing"),
    [string]$BackupDirectory = "D:\AVORA-Signing-Backup",
    [string]$KeytoolPath = "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
)

$ErrorActionPreference = "Stop"

$alias = "avora-app-signing"
$primaryKey = Join-Path $PrimaryDirectory "avora-app-signing.jks"
$backupKey = Join-Path $BackupDirectory "avora-app-signing.jks"
$propertiesFile = Join-Path $PrimaryDirectory "keystore.properties"
$certificateFile = Join-Path $PrimaryDirectory "avora-app-signing-certificate.pem"
$backupCertificate = Join-Path $BackupDirectory "avora-app-signing-certificate.pem"
$checksumFile = Join-Path $BackupDirectory "avora-app-signing.jks.sha256"

if (-not (Test-Path -LiteralPath $KeytoolPath -PathType Leaf)) {
    throw "keytool bulunamadi: $KeytoolPath"
}

$protectedTargets = @($primaryKey, $backupKey, $propertiesFile)
$existingTargets = @($protectedTargets | Where-Object { Test-Path -LiteralPath $_ })
if ($existingTargets.Count -gt 0) {
    throw "Mevcut imza dosyalarinin uzerine yazilmayacak: $($existingTargets -join ', ')"
}

New-Item -ItemType Directory -Path $PrimaryDirectory -Force | Out-Null
New-Item -ItemType Directory -Path $BackupDirectory -Force | Out-Null

$passwordBytes = [byte[]]::new(32)
[Security.Cryptography.RandomNumberGenerator]::Fill($passwordBytes)
$password = [Convert]::ToBase64String($passwordBytes).TrimEnd("=").Replace("+", "A").Replace("/", "B")

$env:AVORA_SIGN_STORE_PASS = $password
$env:AVORA_SIGN_KEY_PASS = $password
try {
    & $KeytoolPath -genkeypair -v `
        -keystore $primaryKey `
        -storetype JKS `
        -storepass:env AVORA_SIGN_STORE_PASS `
        -keypass:env AVORA_SIGN_KEY_PASS `
        -alias $alias `
        -keyalg RSA `
        -keysize 4096 `
        -sigalg SHA256withRSA `
        -validity 10000 `
        -dname "CN=AVORA, OU=Mobile, O=AVORA, L=Istanbul, ST=Istanbul, C=TR"
    if ($LASTEXITCODE -ne 0) {
        throw "AVORA imza anahtari olusturulamadi. Cikis kodu: $LASTEXITCODE"
    }

    & $KeytoolPath -exportcert -rfc `
        -keystore $primaryKey `
        -storepass:env AVORA_SIGN_STORE_PASS `
        -alias $alias `
        -file $certificateFile
    if ($LASTEXITCODE -ne 0) {
        throw "AVORA imza sertifikasi disari aktarilamadi. Cikis kodu: $LASTEXITCODE"
    }

    $properties = @(
        "storeFile=$($primaryKey.Replace('\', '/'))"
        "storePassword=$password"
        "keyAlias=$alias"
        "keyPassword=$password"
    )
    [IO.File]::WriteAllLines(
        $propertiesFile,
        $properties,
        [Text.UTF8Encoding]::new($false)
    )
}
finally {
    Remove-Item Env:AVORA_SIGN_STORE_PASS -ErrorAction SilentlyContinue
    Remove-Item Env:AVORA_SIGN_KEY_PASS -ErrorAction SilentlyContinue
    [Array]::Clear($passwordBytes, 0, $passwordBytes.Length)
    $password = $null
}

Copy-Item -LiteralPath $primaryKey -Destination $backupKey
Copy-Item -LiteralPath $certificateFile -Destination $backupCertificate

$primaryHash = (Get-FileHash -LiteralPath $primaryKey -Algorithm SHA256).Hash
$backupHash = (Get-FileHash -LiteralPath $backupKey -Algorithm SHA256).Hash
if ($primaryHash -ne $backupHash) {
    throw "Imza anahtari yedegi dogrulanamadi."
}
[IO.File]::WriteAllText(
    $checksumFile,
    "$primaryHash  avora-app-signing.jks`n",
    [Text.UTF8Encoding]::new($false)
)

$identity = [Security.Principal.WindowsIdentity]::GetCurrent().Name
foreach ($directory in @($PrimaryDirectory, $BackupDirectory)) {
    & icacls.exe $directory /inheritance:r /grant:r "${identity}:(OI)(CI)F" "SYSTEM:(OI)(CI)F" /Q | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Dosya izinleri guvenli hale getirilemedi: $directory"
    }
    Get-ChildItem -LiteralPath $directory -Recurse -Force | ForEach-Object {
        & icacls.exe $_.FullName /inheritance:e /Q | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Alt oge izinleri guvenli hale getirilemedi: $($_.FullName)"
        }
    }
}

Write-Output "AVORA_RELEASE_SIGNING_CREATED"
Write-Output "PRIMARY_KEY=$primaryKey"
Write-Output "BACKUP_KEY=$backupKey"
Write-Output "PROPERTIES=$propertiesFile"
Write-Output "BACKUP_VERIFIED"
