# PowerShell Deployment Script for School Management System
# Run this script on the Windows Server (10.106.60.33)

param(
    [string]$JarPath = "C:\apps\school-management\school-management-system-0.0.1-SNAPSHOT.jar",
    [string]$PropertiesPath = "C:\apps\school-management\application-prod.properties",
    [string]$ServiceName = "SchoolManagementAPI"
)

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "School Management System - Deployment Script" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Function to check if running as Administrator
function Test-Administrator {
    $user = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($user)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

# Check for admin rights
if (-not (Test-Administrator)) {
    Write-Host "ERROR: This script must be run as Administrator!" -ForegroundColor Red
    Write-Host "Right-click PowerShell and select 'Run as Administrator'" -ForegroundColor Yellow
    exit 1
}

Write-Host "Step 1: Checking Prerequisites..." -ForegroundColor Green
Write-Host "-----------------------------------" -ForegroundColor Gray

# Check Java
Write-Host "Checking Java installation..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "  ✓ Java found: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Java not found!" -ForegroundColor Red
    Write-Host "  Please install Java 17 JDK from: https://adoptium.net/temurin/releases/" -ForegroundColor Yellow
    exit 1
}

# Check NSSM
Write-Host "Checking NSSM..." -ForegroundColor Yellow
try {
    $nssmVersion = nssm version
    Write-Host "  ✓ NSSM found: $nssmVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ NSSM not found!" -ForegroundColor Red
    Write-Host "  Please download from: https://nssm.cc/download" -ForegroundColor Yellow
    Write-Host "  Extract to C:\nssm and add to PATH" -ForegroundColor Yellow
    exit 1
}

# Check MySQL
Write-Host "Checking MySQL..." -ForegroundColor Yellow
$mysqlService = Get-Service -Name "MySQL*" -ErrorAction SilentlyContinue
if ($mysqlService) {
    Write-Host "  ✓ MySQL service found: $($mysqlService.Name) - Status: $($mysqlService.Status)" -ForegroundColor Green
} else {
    Write-Host "  ! MySQL service not found - please ensure MySQL is installed" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Step 2: Creating Directory Structure..." -ForegroundColor Green
Write-Host "-----------------------------------" -ForegroundColor Gray

$appDir = "C:\apps\school-management"
$logsDir = "$appDir\logs"
$backupsDir = "$appDir\backups"

@($appDir, $logsDir, $backupsDir) | ForEach-Object {
    if (-not (Test-Path $_)) {
        New-Item -ItemType Directory -Path $_ -Force | Out-Null
        Write-Host "  ✓ Created: $_" -ForegroundColor Green
    } else {
        Write-Host "  ✓ Exists: $_" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "Step 3: Checking Application Files..." -ForegroundColor Green
Write-Host "-----------------------------------" -ForegroundColor Gray

# Check JAR file
if (Test-Path $JarPath) {
    $jarSize = (Get-Item $JarPath).Length / 1MB
    Write-Host "  ✓ JAR file found: $JarPath ($([math]::Round($jarSize, 2)) MB)" -ForegroundColor Green
} else {
    Write-Host "  ✗ JAR file not found: $JarPath" -ForegroundColor Red
    Write-Host "  Please copy the JAR file to this location" -ForegroundColor Yellow
    exit 1
}

# Check properties file
if (Test-Path $PropertiesPath) {
    Write-Host "  ✓ Properties file found: $PropertiesPath" -ForegroundColor Green
} else {
    Write-Host "  ! Properties file not found: $PropertiesPath" -ForegroundColor Yellow
    Write-Host "  Creating template properties file..." -ForegroundColor Yellow
    
    $propsTemplate = @"
server.port=8080
spring.application.name=School Management System

spring.datasource.url=jdbc:mysql://localhost:3306/school_management_db?createDatabaseIfNotExist=true
spring.datasource.username=school_user
spring.datasource.password=CHANGE_ME
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

logging.level.com.bank.schoolmanagement=INFO
logging.file.name=C:/apps/school-management/logs/application.log

flexcube.enabled=true
flexcube.school.collection.account=58520134002019
"@
    
    Set-Content -Path $PropertiesPath -Value $propsTemplate
    Write-Host "  ✓ Template created - PLEASE UPDATE DATABASE PASSWORD!" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Step 4: Service Management..." -ForegroundColor Green
Write-Host "-----------------------------------" -ForegroundColor Gray

# Check if service exists
$existingService = nssm status $ServiceName 2>&1
if ($existingService -notlike "*not found*") {
    Write-Host "  ! Service already exists: $ServiceName" -ForegroundColor Yellow
    $response = Read-Host "  Do you want to reinstall? (Y/N)"
    if ($response -eq 'Y' -or $response -eq 'y') {
        Write-Host "  Stopping existing service..." -ForegroundColor Yellow
        nssm stop $ServiceName
        Start-Sleep -Seconds 3
        Write-Host "  Removing existing service..." -ForegroundColor Yellow
        nssm remove $ServiceName confirm
        Start-Sleep -Seconds 2
    } else {
        Write-Host "  Skipping service installation" -ForegroundColor Gray
        Write-Host ""
        Write-Host "To restart existing service, run:" -ForegroundColor Yellow
        Write-Host "  nssm restart $ServiceName" -ForegroundColor Cyan
        exit 0
    }
}

# Find Java executable
$javaExe = (Get-Command java).Source
if (-not $javaExe) {
    $javaExe = "C:\Program Files\Java\jdk-17\bin\java.exe"
}

Write-Host "  Installing service: $ServiceName" -ForegroundColor Yellow
Write-Host "  Java: $javaExe" -ForegroundColor Gray

# Install service
nssm install $ServiceName $javaExe
nssm set $ServiceName AppParameters "-jar $JarPath --spring.config.location=$PropertiesPath"
nssm set $ServiceName AppDirectory $appDir
nssm set $ServiceName DisplayName "School Management API"
nssm set $ServiceName Description "School Management System REST API Service"
nssm set $ServiceName Start SERVICE_AUTO_START
nssm set $ServiceName AppStdout "$logsDir\service-stdout.log"
nssm set $ServiceName AppStderr "$logsDir\service-stderr.log"
nssm set $ServiceName AppExit Default Restart
nssm set $ServiceName AppRestartDelay 5000

Write-Host "  ✓ Service installed successfully" -ForegroundColor Green

Write-Host ""
Write-Host "Step 5: Configuring Firewall..." -ForegroundColor Green
Write-Host "-----------------------------------" -ForegroundColor Gray

$firewallRule = Get-NetFirewallRule -DisplayName "School Management API" -ErrorAction SilentlyContinue
if ($firewallRule) {
    Write-Host "  ✓ Firewall rule already exists" -ForegroundColor Gray
} else {
    Write-Host "  Creating firewall rule for port 8080..." -ForegroundColor Yellow
    New-NetFirewallRule -DisplayName "School Management API" `
        -Direction Inbound `
        -LocalPort 8080 `
        -Protocol TCP `
        -Action Allow `
        -Profile Domain,Private | Out-Null
    Write-Host "  ✓ Firewall rule created" -ForegroundColor Green
}

Write-Host ""
Write-Host "Step 6: Starting Service..." -ForegroundColor Green
Write-Host "-----------------------------------" -ForegroundColor Gray

Write-Host "  Starting $ServiceName..." -ForegroundColor Yellow
nssm start $ServiceName
Start-Sleep -Seconds 5

$status = nssm status $ServiceName
Write-Host "  Service Status: $status" -ForegroundColor $(if ($status -like "*running*") { "Green" } else { "Red" })

Write-Host ""
Write-Host "Step 7: Verifying Deployment..." -ForegroundColor Green
Write-Host "-----------------------------------" -ForegroundColor Gray

Write-Host "  Waiting for application to start (30 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 10
    if ($response.StatusCode -eq 200) {
        Write-Host "  ✓ Health check passed!" -ForegroundColor Green
        Write-Host "  Response: $($response.Content)" -ForegroundColor Gray
    }
} catch {
    Write-Host "  ✗ Health check failed!" -ForegroundColor Red
    Write-Host "  Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Check logs at: $logsDir" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Service Information:" -ForegroundColor Yellow
Write-Host "  Name: $ServiceName" -ForegroundColor Gray
Write-Host "  Status: $(nssm status $ServiceName)" -ForegroundColor Gray
Write-Host "  Log Files: $logsDir" -ForegroundColor Gray
Write-Host ""
Write-Host "Application URLs:" -ForegroundColor Yellow
Write-Host "  Local: http://localhost:8080" -ForegroundColor Cyan
Write-Host "  Remote: http://10.106.60.33:8080" -ForegroundColor Cyan
Write-Host "  Health: http://10.106.60.33:8080/actuator/health" -ForegroundColor Cyan
Write-Host ""
Write-Host "Useful Commands:" -ForegroundColor Yellow
Write-Host "  Start:   nssm start $ServiceName" -ForegroundColor Cyan
Write-Host "  Stop:    nssm stop $ServiceName" -ForegroundColor Cyan
Write-Host "  Restart: nssm restart $ServiceName" -ForegroundColor Cyan
Write-Host "  Status:  nssm status $ServiceName" -ForegroundColor Cyan
Write-Host "  Logs:    Get-Content $logsDir\application.log -Wait -Tail 50" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "  1. Update database password in: $PropertiesPath" -ForegroundColor Gray
Write-Host "  2. Restart service: nssm restart $ServiceName" -ForegroundColor Gray
Write-Host "  3. Test API endpoints" -ForegroundColor Gray
Write-Host "  4. Configure HTTPS (optional)" -ForegroundColor Gray
Write-Host ""
