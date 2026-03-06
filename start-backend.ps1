# start-backend.ps1
# Run the Spring Boot backend with Vietnamese-safe (UTF-8) console encoding.

# ─── 1. Switch Windows console codepage to UTF-8 ────────────────────────────
$null = chcp 65001
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding           = [System.Text.UTF8Encoding]::new($false)
Write-Host "✔ Console codepage: UTF-8 (65001)" -ForegroundColor Green

# ─── 2. Ensure JVM also uses UTF-8 (belt-and-suspenders with .mvn/jvm.config) 
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

# ─── 3. Spring Boot environment ──────────────────────────────────────────────
$env:SPRING_PROFILES_ACTIVE = "dev"
# $env:DB_PASSWORD          = ""          # set here or export beforehand

# ─── 4. Start ────────────────────────────────────────────────────────────────
Set-Location "$PSScriptRoot\lms-backend"
mvn spring-boot:run
