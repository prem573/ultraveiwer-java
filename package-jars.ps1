param()

$ErrorActionPreference = "Stop"

Write-Host "=== Packaging Runnable Standalone JARs ===" -ForegroundColor Cyan

# 1. Run Maven package
mvn clean package -DskipTests

# 2. Prepare release directory
$releaseDir = Join-Path $PSScriptRoot "release"
if (-not (Test-Path $releaseDir)) {
    New-Item -ItemType Directory -Path $releaseDir | Out-Null
}

# 3. Copy Client JAR
$clientJarSrc = Join-Path $PSScriptRoot "client-app\target\client-app-1.0.0-SNAPSHOT-standalone.jar"
$clientJarDest = Join-Path $releaseDir "remote-desktop-client.jar"
Copy-Item -Path $clientJarSrc -Destination $clientJarDest -Force
Write-Host "Created Client JAR: $clientJarDest" -ForegroundColor Green

# 4. Copy Relay JAR
$relayJarSrc = Join-Path $PSScriptRoot "relay-server\target\relay-server-1.0.0-SNAPSHOT.jar"
$relayJarDest = Join-Path $releaseDir "remote-desktop-relay.jar"
Copy-Item -Path $relayJarSrc -Destination $relayJarDest -Force
Write-Host "Created Relay JAR:  $relayJarDest" -ForegroundColor Green

Write-Host "`nAll runnable JARs are ready in the 'release/' folder!" -ForegroundColor Cyan
Write-Host "Run Client: java -jar release/remote-desktop-client.jar"
Write-Host "Run Relay:  java -jar release/remote-desktop-relay.jar"

