param(
    [string]$PackageType = "app-image" # "app-image", "exe", or "msi"
)

$ErrorActionPreference = "Stop"

Write-Host "=== Clean-Room Remote Desktop Packaging Script ===" -ForegroundColor Cyan

# 1. Locate jpackage
$jpackage = Get-Command "jpackage" -ErrorAction SilentlyContinue
if (-not $jpackage) {
    $possiblePaths = @(
        "$env:JAVA_HOME\bin\jpackage.exe",
        "C:\Program Files\Java\jdk-27.0.1.0.3\bin\jpackage.exe",
        "C:\Program Files\Java\jdk-21\bin\jpackage.exe"
    )
    foreach ($p in $possiblePaths) {
        if (Test-Path $p) {
            $jpackage = $p
            break
        }
    }
} else {
    $jpackage = $jpackage.Source
}

if (-not $jpackage -or -not (Test-Path $jpackage)) {
    Write-Error "Could not locate jpackage.exe. Please ensure JDK 21+ is installed and JAVA_HOME is set."
}
Write-Host "Using jpackage: $jpackage" -ForegroundColor Green

# 2. Build Maven modules
Write-Host "Building Maven project..." -ForegroundColor Yellow
mvn clean package -DskipTests

# 3. Verify standalone JAR
$clientTarget = Join-Path $PSScriptRoot "client-app\target"
$standaloneJar = "client-app-1.0.0-SNAPSHOT-standalone.jar"
$jarPath = Join-Path $clientTarget $standaloneJar

if (-not (Test-Path $jarPath)) {
    Write-Error "Standalone jar not found at $jarPath"
}
Write-Host "Found standalone jar: $jarPath" -ForegroundColor Green

# 4. Prepare output directory
$distDir = Join-Path $PSScriptRoot "dist"
if (Test-Path $distDir) {
    Remove-Item -Recurse -Force $distDir
}
New-Item -ItemType Directory -Path $distDir | Out-Null

# 5. Run jpackage
Write-Host "Packaging application as $PackageType..." -ForegroundColor Yellow

$jpackageArgs = @(
    "--type", $PackageType,
    "--name", "CleanRoomRemoteDesktop",
    "--app-version", "1.0.0",
    "--input", $clientTarget,
    "--main-jar", $standaloneJar,
    "--main-class", "com.cleanroom.remotedesktop.client.ClientLauncher",
    "--dest", $distDir
)

if ($PackageType -in @("exe", "msi")) {
    $jpackageArgs += @("--win-shortcut", "--win-menu")
}

& $jpackage @jpackageArgs

if ($LASTEXITCODE -eq 0) {
    Write-Host "=== Packaging Complete! Output located in: $distDir ===" -ForegroundColor Green
} else {
    Write-Error "jpackage failed with exit code $LASTEXITCODE"
}
