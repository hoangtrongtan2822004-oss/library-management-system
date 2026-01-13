# 🚀 Quick Start Guide - Environment Setup

## Prerequisites

- Java 21+
- MySQL 8.0+
- Node.js 18+ (for frontend)
- Maven 3.8+

---

## ⚙️ Step 1: Configure Environment Variables

### Option A: Using .env file (Recommended for Local Development)

1. **Copy the template:**

   ```bash
   cp .env.example .env
   ```

2. **Edit `.env` with your credentials:**

   ```bash
   # Required - Database
   DB_PASSWORD=your_mysql_root_password

   # Required - JWT (generate: openssl rand -base64 32)
   APP_JWT_SECRET=your-super-secure-jwt-secret-here

   # Required - Google Gemini API (https://aistudio.google.com/app/apikey)
   GEMINI_API_KEY=AIzaSy...

   # Optional - Email (Gmail example)
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=your-gmail-app-password
   ```

3. **Verify `.env` is in `.gitignore`** (already done!)

### Option B: Using PowerShell (Windows)

```powershell
# Set for current session
$env:DB_PASSWORD="123456"
$env:APP_JWT_SECRET="my-super-secret-key-minimum-32-bytes"
$env:GEMINI_API_KEY="AIzaSy..."

# Or set permanently (User level)
[Environment]::SetEnvironmentVariable("DB_PASSWORD", "123456", "User")
[Environment]::SetEnvironmentVariable("APP_JWT_SECRET", "your-secret", "User")
[Environment]::SetEnvironmentVariable("GEMINI_API_KEY", "AIzaSy...", "User")
```

### Option C: Using Bash (Linux/Mac)

```bash
# Add to ~/.bashrc or ~/.zshrc
export DB_PASSWORD="123456"
export APP_JWT_SECRET="my-super-secret-key"
export GEMINI_API_KEY="AIzaSy..."

# Reload shell
source ~/.bashrc
```

---

## 🗄️ Step 2: Setup Database

```sql
-- Create database
CREATE DATABASE IF NOT EXISTS lms_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Import initial data (optional)
USE lms_db;
SOURCE data.sql;
```

---

## 🎯 Step 3: Run Backend

```bash
cd lms-backend

# Option 1: Maven
mvn spring-boot:run -Dspring.profiles.active=dev

# Option 2: Using batch file (Windows)
cd ..
.\start-backend.bat
```

**Backend runs on:** http://localhost:8080

**Swagger UI:** http://localhost:8080/swagger-ui/index.html

---

## 🌐 Step 4: Run Frontend

```bash
cd lms-frontend
npm install
npm start
```

**Frontend runs on:** http://localhost:4200

---

## 🧪 Test Credentials

### Admin Account

- **Username:** `admin`
- **Password:** `admin123`

### Regular User

- **Username:** `user`
- **Password:** `user123`

---

## 🐳 Docker Setup (Alternative)

```bash
# Create .env file first!
docker-compose up -d
```

Services:

- **Backend:** http://localhost:8080
- **Frontend:** http://localhost:80
- **MySQL:** localhost:3308
- **Redis:** localhost:6379

---

## 🔍 Troubleshooting

### Problem: "Access denied for user 'root'@'localhost'"

**Solution:** Check your `DB_PASSWORD` environment variable

```bash
# Windows PowerShell
echo $env:DB_PASSWORD

# Linux/Mac
echo $DB_PASSWORD
```

### Problem: "GEMINI_API_KEY not configured"

**Solution:** Get free API key at https://aistudio.google.com/app/apikey

```bash
$env:GEMINI_API_KEY="AIzaSy..."  # Windows
export GEMINI_API_KEY="AIzaSy..."  # Linux/Mac
```

### Problem: "JWT secret too short"

**Solution:** Generate secure secret

```bash
# Linux/Mac
openssl rand -base64 32

# PowerShell (Windows)
[Convert]::ToBase64String((1..32 | ForEach-Object {Get-Random -Maximum 256}))
```

### Problem: Backend starts but can't connect to MySQL

**Solution:** Verify MySQL is running and port is correct

```bash
# Check MySQL status
sudo systemctl status mysql  # Linux
Get-Service MySQL  # Windows PowerShell

# Test connection
mysql -u root -p -h 127.0.0.1 -P 3306
```

---

## 📊 Verify Installation

1. **Backend Health Check:**

   ```bash
   curl http://localhost:8080/actuator/health
   ```

   Expected: `{"status":"UP"}`

2. **Login Test:**

   ```bash
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"admin123"}'
   ```

   Expected: JWT token in response

3. **Frontend:** Open http://localhost:4200 and login

---

## 🚢 Deployment Checklist

Before deploying to production:

- [ ] Change all default passwords
- [ ] Set strong JWT secret (32+ bytes)
- [ ] Use production database (not localhost)
- [ ] Enable HTTPS
- [ ] Set `spring.jpa.show-sql=false`
- [ ] Configure proper logging levels
- [ ] Enable CORS only for trusted domains
- [ ] Set up database backups
- [ ] Configure email SMTP properly
- [ ] Monitor application logs
- [ ] Set up error alerting

---

## 📚 Next Steps

1. Read [SECURITY_BEST_PRACTICES.md](./SECURITY_BEST_PRACTICES.md)
2. Check [START_HERE.md](./START_HERE.md) for features overview
3. See [QUICK_REFERENCE.md](./QUICK_REFERENCE.md) for API endpoints
4. Review [docker-compose.yml](./docker-compose.yml) for production setup

---

## 🆘 Need Help?

- Check backend logs: `lms-backend/logs/application.log`
- Enable debug mode: `logging.level.com.ibizabroker.lms=DEBUG`
- Review API docs: http://localhost:8080/swagger-ui/index.html
- Test database connection: `mysql -u root -p -h 127.0.0.1 lms_db`
