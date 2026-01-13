# 🚀 Deployment Quick Reference

**Cheatsheet cho các lệnh deployment thường dùng**

---

## 🐳 Docker Commands

### Start Services

```bash
# Full stack (MySQL + Redis + Backend + Frontend)
docker-compose -f docker-compose.simple.yml up --build

# Detached mode (run in background)
docker-compose -f docker-compose.simple.yml up -d --build

# View logs
docker-compose -f docker-compose.simple.yml logs -f

# View specific service logs
docker-compose -f docker-compose.simple.yml logs -f backend
```

### Stop Services

```bash
# Stop all services
docker-compose -f docker-compose.simple.yml stop

# Stop and remove containers
docker-compose -f docker-compose.simple.yml down

# Stop and remove volumes (⚠️ deletes data!)
docker-compose -f docker-compose.simple.yml down -v
```

### Health Checks

```bash
# Check all services
docker-compose -f docker-compose.simple.yml ps

# Backend health
curl http://localhost:8080/actuator/health

# MySQL health
docker-compose -f docker-compose.simple.yml exec mysql mysqladmin ping -h localhost -uroot -proot

# Redis health
docker-compose -f docker-compose.simple.yml exec redis redis-cli ping
```

### Debugging

```bash
# Shell into backend
docker-compose -f docker-compose.simple.yml exec backend sh

# Shell into MySQL
docker-compose -f docker-compose.simple.yml exec mysql mysql -uroot -proot lms_db

# Shell into frontend
docker-compose -f docker-compose.simple.yml exec frontend sh

# View resource usage
docker stats
```

---

## ☁️ Vercel (Frontend)

### Deploy via CLI

```bash
# Install Vercel CLI
npm i -g vercel

# Login
vercel login

# Deploy to production
cd lms-frontend
vercel --prod

# Deploy to preview
vercel
```

### Environment Variables

```bash
# Add variable
vercel env add API_BASE_URL production

# List variables
vercel env ls

# Pull variables to local .env
vercel env pull
```

### Logs

```bash
# View logs
vercel logs <deployment-url>

# Follow logs
vercel logs --follow
```

---

## 🚂 Render (Backend)

### Deploy via Dashboard

1. Go to https://dashboard.render.com/
2. New → Web Service
3. Connect GitHub repo
4. Configure:
   - Name: `lms-backend`
   - Environment: Docker
   - Dockerfile Path: `lms-backend/Dockerfile`
   - Docker Context: `lms-backend`

### Via render.yaml

```bash
# Push code with render.yaml
git add render.yaml
git commit -m "feat: add render blueprint"
git push origin main

# Render auto-detects and deploys
```

### Useful Curl Commands

```bash
# Health check
curl https://your-app.onrender.com/actuator/health

# Test login
curl -X POST https://your-app.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'

# Test public endpoint
curl https://your-app.onrender.com/api/public/books
```

---

## 🚂 Railway (Backend)

### Deploy via CLI

```bash
# Install Railway CLI
npm i -g @railway/cli

# Login
railway login

# Initialize project
railway init

# Deploy
railway up

# Link to existing project
railway link <project-id>
```

### Database Commands

```bash
# Connect to MySQL
railway connect mysql

# Connect to Redis
railway connect redis

# View logs
railway logs

# Follow logs
railway logs --follow
```

### Environment Variables

```bash
# List variables
railway variables

# Add variable
railway variables set KEY=value

# Delete variable
railway variables delete KEY
```

---

## 🔐 Generate Secrets

### JWT Secret (32+ bytes)

```bash
# PowerShell
-join ((48..57)+(65..90)+(97..122)|Get-Random -Count 32|%{[char]$_})

# Linux/Mac
openssl rand -base64 32

# Online
# https://randomkeygen.com/
```

### MySQL Password

```bash
# PowerShell
-join ((48..57)+(65..90)+(97..122)|Get-Random -Count 16|%{[char]$_})

# Linux/Mac
openssl rand -base64 16
```

---

## 📊 Health Check Endpoints

### Backend

```bash
# Actuator health
curl http://localhost:8080/actuator/health

# Detailed health (requires authentication)
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness

# All actuator endpoints
curl http://localhost:8080/actuator
```

### Database

```bash
# MySQL
mysql -h 127.0.0.1 -P 3307 -uroot -proot -e "SELECT 1"

# Via Docker
docker-compose -f docker-compose.simple.yml exec mysql mysqladmin ping
```

### Redis

```bash
# Redis CLI
redis-cli -h localhost -p 6379 ping

# Via Docker
docker-compose -f docker-compose.simple.yml exec redis redis-cli ping
```

---

## 🧪 Test Commands

### Test Login

```bash
# Local
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'

# Production
curl -X POST https://your-backend.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'
```

### Test API with Token

```bash
# 1. Login and save token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}' \
  | jq -r '.token')

# 2. Use token to call protected endpoint
curl http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🗄️ Database Commands

### Import Data

```bash
# Local MySQL
mysql -h 127.0.0.1 -P 3307 -uroot -proot lms_db < lms_db_backup.sql

# Docker MySQL
docker-compose -f docker-compose.simple.yml exec -T mysql \
  mysql -uroot -proot lms_db < lms_db_backup.sql

# Railway
railway connect mysql
SOURCE lms_db_backup.sql;

# Render (via connection string)
mysql -h dpg-xxx.render.com -u lms_user -p lms_db < lms_db_backup.sql
```

### Export Data

```bash
# Local
mysqldump -h 127.0.0.1 -P 3307 -uroot -proot lms_db > backup_$(date +%Y%m%d).sql

# Docker
docker-compose -f docker-compose.simple.yml exec mysql \
  mysqldump -uroot -proot lms_db > backup_$(date +%Y%m%d).sql
```

### Connect to Database

```bash
# Local
mysql -h 127.0.0.1 -P 3307 -uroot -proot lms_db

# Docker
docker-compose -f docker-compose.simple.yml exec mysql mysql -uroot -proot lms_db

# Railway
railway connect mysql

# Render (get URL from dashboard)
mysql -h <host> -u <user> -p <database>
```

---

## 📝 Git Commands

### Push to Deploy

```bash
# Standard workflow
git add .
git commit -m "feat: your changes"
git push origin main

# Vercel and Render/Railway auto-deploy on push to main
```

### Create Feature Branch

```bash
# Create and switch to feature branch
git checkout -b feature/your-feature

# Push feature branch
git push origin feature/your-feature

# Vercel creates preview deployment automatically
```

### Rollback

```bash
# View commit history
git log --oneline

# Rollback to previous commit
git reset --hard <commit-hash>
git push --force origin main

# Or revert specific commit (safer)
git revert <commit-hash>
git push origin main
```

---

## 🔧 Common Fixes

### Fix Port Conflicts

```bash
# Find process using port 8080
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### Fix Docker Out of Memory

```bash
# Increase Docker memory
# Docker Desktop → Settings → Resources → Memory → 4GB

# Or optimize JAVA_OPTS
JAVA_OPTS=-Xms256m -Xmx450m -XX:MaxRAMPercentage=70.0
```

### Fix CORS Issues

```java
// WebSecurityConfiguration.java
config.setAllowedOrigins(Arrays.asList(
    "http://localhost:4200",
    "https://your-frontend.vercel.app",
    "https://your-backend.onrender.com"  // Add all origins
));
```

---

## 📊 Monitoring Commands

### Docker Stats

```bash
# Real-time resource usage
docker stats

# Specific container
docker stats <container-name>
```

### Logs

```bash
# Backend logs (Docker)
docker-compose -f docker-compose.simple.yml logs -f backend

# Filter by level
docker-compose logs backend | grep ERROR

# Last 100 lines
docker-compose logs --tail=100 backend
```

### Performance

```bash
# JVM memory usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP requests
curl http://localhost:8080/actuator/metrics/http.server.requests

# Database connections
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

---

## 🚨 Emergency Commands

### Stop Everything

```bash
# Stop Docker services
docker-compose -f docker-compose.simple.yml down

# Stop all Docker containers
docker stop $(docker ps -aq)

# Remove all containers (⚠️ careful!)
docker rm $(docker ps -aq)
```

### Reset Database

```bash
# Stop services
docker-compose -f docker-compose.simple.yml down -v

# Restart (will recreate DB)
docker-compose -f docker-compose.simple.yml up --build
```

### Force Rebuild

```bash
# Rebuild without cache
docker-compose -f docker-compose.simple.yml build --no-cache

# Rebuild and start
docker-compose -f docker-compose.simple.yml up --build --force-recreate
```

---

## 🔗 Quick Links

| Service  | Local                                 | Production                               |
| -------- | ------------------------------------- | ---------------------------------------- |
| Frontend | http://localhost                      | https://your-app.vercel.app              |
| Backend  | http://localhost:8080                 | https://your-backend.com                 |
| Health   | http://localhost:8080/actuator/health | https://your-backend.com/actuator/health |
| MySQL    | localhost:3307                        | (internal network)                       |
| Redis    | localhost:6379                        | (internal network)                       |

---

## 📞 Platform Dashboards

- **Vercel:** https://vercel.com/dashboard
- **Render:** https://dashboard.render.com/
- **Railway:** https://railway.app/dashboard
- **GitHub Actions:** https://github.com/yourusername/repo/actions

---

## 💡 Pro Tips

1. **Use environment-specific configs:**

   ```bash
   # Development
   SPRING_PROFILES_ACTIVE=dev

   # Production
   SPRING_PROFILES_ACTIVE=prod
   ```

2. **Always test locally before deploy:**

   ```bash
   docker-compose -f docker-compose.simple.yml up --build
   ```

3. **Monitor logs after deploy:**

   ```bash
   # Vercel
   vercel logs --follow

   # Railway
   railway logs --follow
   ```

4. **Keep secrets safe:**

   - Never commit `.env` files
   - Use platform's environment variables
   - Rotate secrets regularly

5. **Backup before major changes:**

   ```bash
   # Backup database
   mysqldump ... > backup_$(date +%Y%m%d).sql

   # Create git tag
   git tag -a v1.0.0 -m "Production release"
   git push --tags
   ```

---

**Last Updated:** $(date +%Y-%m-%d)

For detailed guides, see:

- [START_DOCKER.md](./START_DOCKER.md)
- [DEPLOYMENT_MASTER_GUIDE.md](./DEPLOYMENT_MASTER_GUIDE.md)
