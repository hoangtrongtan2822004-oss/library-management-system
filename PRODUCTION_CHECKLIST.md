# 🚀 Production Deployment Checklist

**Last Updated:** January 12, 2026  
**Version:** 1.0.0

---

## 📋 Pre-Deployment Checklist

### 🔐 Security (CRITICAL)

- [ ] **Change ALL default passwords**
  - [ ] MySQL root password
  - [ ] Admin account password (`admin123` → strong password)
  - [ ] User account passwords
- [ ] **Generate production JWT secret**

  ```bash
  openssl rand -base64 32
  ```

  - [ ] Minimum 32 bytes
  - [ ] Stored in environment variable (not in code!)
  - [ ] Different from dev secret

- [ ] **API Keys secured**

  - [ ] Gemini API key in environment variable
  - [ ] Gmail app password for email service
  - [ ] No keys in Git history

- [ ] **Database security**

  - [ ] Production database on separate server
  - [ ] Firewall rules configured (only app server can access)
  - [ ] SSL/TLS enabled for database connections
  - [ ] Regular backups scheduled

- [ ] **HTTPS enabled**

  - [ ] SSL/TLS certificate installed
  - [ ] HTTP → HTTPS redirect configured
  - [ ] HSTS header enabled
  - [ ] Certificate auto-renewal setup

- [ ] **CORS configured properly**
  - [ ] Only trusted domains allowed
  - [ ] No wildcard (`*`) in production
  - [ ] Check `WebSecurityConfiguration.java`

### ⚙️ Configuration

- [ ] **Environment variables set**

  ```bash
  DB_URL=jdbc:mysql://prod-db-server:3306/lms_db
  DB_USERNAME=lms_app_user
  DB_PASSWORD=<strong-password>
  APP_JWT_SECRET=<32-byte-secret>
  GEMINI_API_KEY=<your-key>
  MAIL_USERNAME=<email>
  MAIL_PASSWORD=<app-password>
  REDIS_HOST=prod-redis-server
  REDIS_PORT=6379
  ```

- [ ] **Spring profiles**

  - [ ] `SPRING_PROFILES_ACTIVE=prod`
  - [ ] Production profile configured in `application-prod.properties`

- [ ] **Logging configuration**

  - [ ] `spring.jpa.show-sql=false`
  - [ ] Log level: `INFO` (not DEBUG)
  - [ ] Log rotation configured
  - [ ] Log aggregation setup (ELK, CloudWatch, etc.)

- [ ] **Database configuration**
  - [ ] Connection pool size optimized
  - [ ] `spring.jpa.hibernate.ddl-auto=validate` (not update!)
  - [ ] Database migrations tested

### 🧪 Testing

- [ ] **Unit tests passed**

  ```bash
  mvn test
  ```

- [ ] **Integration tests passed**

  ```bash
  mvn verify
  ```

- [ ] **Security testing**

  - [ ] OWASP ZAP scan completed
  - [ ] SQL injection tests passed
  - [ ] XSS vulnerability tests passed
  - [ ] Authentication bypass attempts blocked

- [ ] **Load testing**

  - [ ] Performance tested with expected traffic
  - [ ] Database connection pool sized correctly
  - [ ] Memory usage optimized
  - [ ] Response times acceptable (<200ms avg)

- [ ] **User acceptance testing**
  - [ ] All features tested by QA team
  - [ ] Edge cases handled
  - [ ] Error messages user-friendly

### 📦 Build & Deployment

- [ ] **Build production artifacts**

  ```bash
  # Backend
  cd lms-backend
  mvn clean package -DskipTests

  # Frontend
  cd lms-frontend
  npm run build --prod
  ```

- [ ] **Docker images**

  - [ ] Backend image built and tagged
  - [ ] Frontend image built and tagged
  - [ ] Images pushed to registry
  - [ ] Image scanning completed (no critical vulnerabilities)

- [ ] **Database migrations**

  - [ ] Backup taken before migration
  - [ ] Migration scripts tested on staging
  - [ ] Rollback plan prepared

- [ ] **Infrastructure ready**
  - [ ] Server provisioned (CPU, RAM, disk)
  - [ ] Firewall rules configured
  - [ ] Load balancer configured (if applicable)
  - [ ] Health checks configured

### 📊 Monitoring & Observability

- [ ] **Application monitoring**

  - [ ] Spring Boot Actuator enabled
  - [ ] Health endpoint: `/actuator/health`
  - [ ] Metrics endpoint: `/actuator/metrics`
  - [ ] Prometheus/Grafana setup (optional)

- [ ] **Logging**

  - [ ] Centralized logging configured
  - [ ] Error alerts setup
  - [ ] Log retention policy defined

- [ ] **Alerts configured**

  - [ ] High error rate alerts
  - [ ] Database connection issues
  - [ ] High memory/CPU usage
  - [ ] Disk space low

- [ ] **Backup & Recovery**
  - [ ] Database backups scheduled (daily recommended)
  - [ ] Backup restoration tested
  - [ ] Disaster recovery plan documented

### 📝 Documentation

- [ ] **API documentation**

  - [ ] Swagger UI accessible
  - [ ] All endpoints documented
  - [ ] Authentication documented

- [ ] **Runbooks created**

  - [ ] How to deploy
  - [ ] How to rollback
  - [ ] Common issues & solutions
  - [ ] On-call procedures

- [ ] **Change log maintained**
  - [ ] Version numbers
  - [ ] What changed in each release
  - [ ] Known issues documented

---

## 🎯 Deployment Steps

### 1. Pre-Deployment

```bash
# 1. Tag release
git tag -a v1.0.0 -m "Production release v1.0.0"
git push origin v1.0.0

# 2. Backup database
mysqldump -u root -p lms_db > backup_$(date +%Y%m%d).sql

# 3. Notify team
# Send notification to #deployments channel
```

### 2. Build

```bash
# Backend
cd lms-backend
mvn clean package -DskipTests
# Output: target/lms-0.0.1-SNAPSHOT.jar

# Frontend
cd lms-frontend
npm run build --prod
# Output: dist/lms-frontend/
```

### 3. Deploy

```bash
# Option A: Docker Compose
docker-compose -f docker-compose.prod.yml up -d

# Option B: Manual deployment
# 1. Copy JAR to server
scp target/lms-0.0.1-SNAPSHOT.jar user@prod-server:/opt/lms/

# 2. Copy frontend build
scp -r dist/lms-frontend/* user@prod-server:/var/www/lms/

# 3. Start services
ssh user@prod-server
cd /opt/lms
./start.sh
```

### 4. Verification

```bash
# Health check
curl https://your-domain.com/actuator/health

# Test login
curl -X POST https://your-domain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"<new-password>"}'

# Check frontend
curl -I https://your-domain.com/
```

### 5. Post-Deployment

```bash
# Monitor logs
tail -f /opt/lms/logs/application.log

# Check metrics
curl https://your-domain.com/actuator/metrics

# Verify database connections
mysql -h prod-db-server -u lms_app_user -p lms_db -e "SHOW PROCESSLIST;"
```

---

## 🆘 Rollback Plan

### If Deployment Fails

1. **Stop new version**

   ```bash
   docker-compose down
   # or
   systemctl stop lms-backend
   ```

2. **Restore database (if changed)**

   ```bash
   mysql -u root -p lms_db < backup_20260112.sql
   ```

3. **Revert to previous version**

   ```bash
   git checkout v0.9.9
   docker-compose up -d
   ```

4. **Verify rollback**

   ```bash
   curl https://your-domain.com/actuator/health
   ```

5. **Notify team & investigate**
   - Document what went wrong
   - Create post-mortem
   - Plan fix for next release

---

## 📈 Post-Deployment Monitoring

### First 24 Hours

- [ ] Monitor error logs every hour
- [ ] Check database performance
- [ ] Monitor API response times
- [ ] Watch for memory leaks
- [ ] Verify scheduled jobs running (email notifications)

### First Week

- [ ] Review error patterns
- [ ] Optimize slow queries
- [ ] Fine-tune cache settings
- [ ] Gather user feedback
- [ ] Plan hotfixes if needed

### Ongoing

- [ ] Weekly log review
- [ ] Monthly security patches
- [ ] Quarterly dependency updates
- [ ] Bi-annual disaster recovery drill

---

## 🛠️ Common Issues & Solutions

### Issue: "Out of memory" error

**Solution:**

```bash
# Increase JVM heap size
java -Xms512m -Xmx2048m -jar lms-0.0.1-SNAPSHOT.jar

# Or in docker-compose.yml
environment:
  JAVA_OPTS: "-Xms512m -Xmx2048m"
```

### Issue: Database connection pool exhausted

**Solution:**

```properties
# application-prod.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

### Issue: Slow API responses

**Solution:**

1. Enable database query logging temporarily
2. Identify N+1 queries
3. Add `@EntityGraph` or `@BatchSize` annotations
4. Enable Redis caching for frequent queries

### Issue: Email notifications not sending

**Solution:**

1. Check Gmail app password (not account password!)
2. Verify firewall allows SMTP (port 587)
3. Check logs for detailed error
4. Test with `telnet smtp.gmail.com 587`

---

## 📞 Emergency Contacts

| Role          | Name        | Contact       | Availability     |
| ------------- | ----------- | ------------- | ---------------- |
| Tech Lead     | [Your Name] | [Email/Phone] | 24/7             |
| DevOps        | [Name]      | [Email/Phone] | Business hours   |
| DBA           | [Name]      | [Email/Phone] | On-call rotation |
| Product Owner | [Name]      | [Email]       | Business hours   |

---

## ✅ Sign-Off

- [ ] **Development Team Lead:** **********\_********** Date: **\_\_\_**
- [ ] **QA Lead:** **********\_********** Date: **\_\_\_**
- [ ] **DevOps Engineer:** **********\_********** Date: **\_\_\_**
- [ ] **Product Owner:** **********\_********** Date: **\_\_\_**

---

**Deployment Status:** ⏳ Ready for Deployment

_Once all checkboxes are completed, this application is ready for production!_
