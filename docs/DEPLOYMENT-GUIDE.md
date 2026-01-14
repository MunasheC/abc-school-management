# School Management System - Deployment Guide

## Deployment to Windows Server (10.106.60.33)

### Prerequisites on Server

1. **Java 17 JDK**
   - Download: https://adoptium.net/temurin/releases/
   - Install to: `C:\Program Files\Java\jdk-17`
   - Add to PATH: `C:\Program Files\Java\jdk-17\bin`
   - Verify: `java -version`

2. **MySQL Server 8.0**
   - Download: https://dev.mysql.com/downloads/mysql/
   - Install and configure
   - Create database: `school_management_db`
   - Create user with permissions

3. **NSSM (Service Manager)**
   - Download: https://nssm.cc/download
   - Extract to: `C:\nssm`
   - Add to PATH

---

## Step 1: Build Application

On your development machine:

```powershell
# Navigate to project
cd "c:\Users\bchihota\Desktop\DTI\Dev\school management system"

# Clean and build
.\mvnw clean package -DskipTests

# JAR location: target\school-management-system-0.0.1-SNAPSHOT.jar
```

---

## Step 2: Prepare Server

### Create Application Directory

```powershell
# On server 10.106.60.33
mkdir C:\apps\school-management
mkdir C:\apps\school-management\logs
```

### Transfer Files

Copy these files to `C:\apps\school-management\`:
- `school-management-system-0.0.1-SNAPSHOT.jar`
- `application-prod.properties` (see below)

---

## Step 3: Configure Production Properties

Create `C:\apps\school-management\application-prod.properties`:

```properties
# Server Configuration
server.port=8080
spring.application.name=School Management System

# Database Configuration (UPDATE THESE)
spring.datasource.url=jdbc:mysql://localhost:3306/school_management_db?createDatabaseIfNotExist=true
spring.datasource.username=school_user
spring.datasource.password=CHANGE_ME_SECURE_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Logging
logging.level.com.bank.schoolmanagement=INFO
logging.level.org.springframework.web=WARN
logging.file.name=C:/apps/school-management/logs/application.log
logging.file.max-size=10MB
logging.file.max-history=30

# CORS (UPDATE FOR PRODUCTION)
allowed.origins=http://10.106.60.33,http://localhost:3000

# Flexcube Integration
flexcube.enabled=true
flexcube.api.url=https://api.bank.example.com/flexcube/v1/payments
flexcube.default.source=COUNTER
flexcube.default.userid=FLEXSWITCH
flexcube.default.branch=120
flexcube.default.product=ABDS
flexcube.default.currency=USD
flexcube.school.collection.account=58520134002019
flexcube.api.timeout=30000
```

---

## Step 4: Install as Windows Service

### Using NSSM

```powershell
# Install service
nssm install SchoolManagementAPI "C:\Program Files\Java\jdk-17\bin\java.exe"

# Set application parameters
nssm set SchoolManagementAPI AppParameters "-jar C:\apps\school-management\school-management-system-0.0.1-SNAPSHOT.jar --spring.config.location=C:\apps\school-management\application-prod.properties"

# Set working directory
nssm set SchoolManagementAPI AppDirectory "C:\apps\school-management"

# Set display name and description
nssm set SchoolManagementAPI DisplayName "School Management API"
nssm set SchoolManagementAPI Description "School Management System REST API Service"

# Set startup type to automatic
nssm set SchoolManagementAPI Start SERVICE_AUTO_START

# Set log files
nssm set SchoolManagementAPI AppStdout "C:\apps\school-management\logs\service-stdout.log"
nssm set SchoolManagementAPI AppStderr "C:\apps\school-management\logs\service-stderr.log"

# Set restart options
nssm set SchoolManagementAPI AppExit Default Restart
nssm set SchoolManagementAPI AppRestartDelay 5000

# Start the service
nssm start SchoolManagementAPI
```

### Verify Service

```powershell
# Check service status
nssm status SchoolManagementAPI

# View service in Services app
services.msc

# Check logs
Get-Content C:\apps\school-management\logs\application.log -Tail 50
```

---

## Step 5: Configure Firewall

```powershell
# Allow inbound traffic on port 8080
New-NetFirewallRule -DisplayName "School Management API" `
    -Direction Inbound `
    -LocalPort 8080 `
    -Protocol TCP `
    -Action Allow `
    -Profile Domain,Private

# Verify
Get-NetFirewallRule -DisplayName "School Management API"
```

---

## Step 6: Test Deployment

### Health Check

```powershell
# Test from server itself
Invoke-WebRequest -Uri http://localhost:8080/actuator/health

# Test from another machine
Invoke-WebRequest -Uri http://10.106.60.33:8080/api/schools
```

### API Endpoints

- Health: `http://10.106.60.33:8080/actuator/health`
- Schools: `http://10.106.60.33:8080/api/schools`
- Students: `http://10.106.60.33:8080/api/school/students`
- Bank Lookup: `http://10.106.60.33:8080/api/bank/lookup`

---

## Step 7: Optional - IIS Reverse Proxy

### Install IIS Components

```powershell
# Install IIS
Install-WindowsFeature -name Web-Server -IncludeManagementTools

# Download and install:
# 1. URL Rewrite: https://www.iis.net/downloads/microsoft/url-rewrite
# 2. Application Request Routing: https://www.iis.net/downloads/microsoft/application-request-routing
```

### Configure Reverse Proxy

Create `C:\inetpub\wwwroot\web.config`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <system.webServer>
        <rewrite>
            <rules>
                <rule name="SchoolManagementAPI" stopProcessing="true">
                    <match url="^api/(.*)" />
                    <action type="Rewrite" url="http://localhost:8080/api/{R:1}" />
                    <serverVariables>
                        <set name="HTTP_X_FORWARDED_HOST" value="{HTTP_HOST}" />
                        <set name="HTTP_X_FORWARDED_PROTO" value="http" />
                    </serverVariables>
                </rule>
            </rules>
        </rewrite>
        <httpProtocol>
            <customHeaders>
                <add name="Access-Control-Allow-Origin" value="*" />
                <add name="Access-Control-Allow-Methods" value="GET, POST, PUT, DELETE, OPTIONS" />
                <add name="Access-Control-Allow-Headers" value="Content-Type, X-School-ID" />
            </customHeaders>
        </httpProtocol>
    </system.webServer>
</configuration>
```

### Enable ARR Proxy

```powershell
# Open IIS Manager
# Navigate to Server → Application Request Routing Cache
# Server Proxy Settings → Enable proxy
# Click Apply
```

**Now accessible via:**
- Direct: `http://10.106.60.33:8080/api/...`
- Through IIS: `http://10.106.60.33/api/...`

---

## Service Management Commands

```powershell
# Start service
nssm start SchoolManagementAPI

# Stop service
nssm stop SchoolManagementAPI

# Restart service
nssm restart SchoolManagementAPI

# Check status
nssm status SchoolManagementAPI

# View service logs
Get-Content C:\apps\school-management\logs\application.log -Wait

# Uninstall service (if needed)
nssm stop SchoolManagementAPI
nssm remove SchoolManagementAPI confirm
```

---

## Troubleshooting

### Service won't start

```powershell
# Check Java installation
java -version

# Check JAR file exists
Test-Path "C:\apps\school-management\school-management-system-0.0.1-SNAPSHOT.jar"

# Check properties file
Test-Path "C:\apps\school-management\application-prod.properties"

# View error logs
Get-Content C:\apps\school-management\logs\service-stderr.log
```

### Database connection issues

```powershell
# Test MySQL connection
mysql -h localhost -u school_user -p

# Verify database exists
SHOW DATABASES;

# Check MySQL service
Get-Service -Name MySQL80
```

### Port already in use

```powershell
# Find what's using port 8080
netstat -ano | findstr :8080

# Kill process (replace PID)
taskkill /PID <PID> /F
```

---

## Updates / Redeployment

```powershell
# 1. Build new JAR on dev machine
.\mvnw clean package -DskipTests

# 2. Stop service on server
nssm stop SchoolManagementAPI

# 3. Backup old JAR
Copy-Item "C:\apps\school-management\school-management-system-0.0.1-SNAPSHOT.jar" `
          "C:\apps\school-management\backups\school-management-$(Get-Date -Format 'yyyyMMdd-HHmmss').jar"

# 4. Upload new JAR

# 5. Start service
nssm start SchoolManagementAPI

# 6. Verify
Invoke-WebRequest -Uri http://localhost:8080/actuator/health
```

---

## Monitoring

### Application Logs

```powershell
# Tail logs
Get-Content C:\apps\school-management\logs\application.log -Wait -Tail 50

# Search for errors
Select-String -Path "C:\apps\school-management\logs\application.log" -Pattern "ERROR"
```

### Performance Monitoring

```powershell
# CPU and Memory usage
Get-Process java | Select-Object CPU, WorkingSet, Id

# Service uptime
Get-Service SchoolManagementAPI | Select-Object Status, StartType
```

---

## Security Checklist

- [ ] Change default database password
- [ ] Configure HTTPS (SSL certificate)
- [ ] Update CORS allowed origins
- [ ] Disable H2 console in production
- [ ] Set `spring.jpa.show-sql=false`
- [ ] Configure proper logging levels
- [ ] Set up database backups
- [ ] Configure Windows Firewall rules
- [ ] Restrict MySQL remote access
- [ ] Use strong Flexcube API credentials

---

## Production URLs

After deployment:

- **API Base**: `http://10.106.60.33:8080`
- **Health Check**: `http://10.106.60.33:8080/actuator/health`
- **Swagger UI**: `http://10.106.60.33:8080/swagger-ui.html`
- **Bank Lookup**: `http://10.106.60.33:8080/api/bank/lookup`
- **School API**: `http://10.106.60.33:8080/api/schools`
