# 🚀 Deployment Guide

## Prerequisites

### Required Tools

- Docker 20.10+ and Docker Compose 2.0+
- Git
- AWS CLI (for AWS deployment)
- kubectl (for Kubernetes deployment)

### Required Accounts

- GitHub account with repository access
- Docker Hub account (for image registry)
- AWS account (for ECS deployment) OR Azure/GCP account

### Required Environment Variables

Create a `.env.prod` file in the root directory:

```bash
# Database
DB_URL=jdbc:mysql://mysql:3306/lms_db
DB_USERNAME=lms_user
DB_PASSWORD=your_secure_password_here

# JWT
APP_JWT_SECRET=your_32_byte_minimum_secret_key_here

# Gemini AI
GEMINI_API_KEY=your_gemini_api_key_here

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password_here

# Email
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password_here

# CORS
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

---

## 🐳 Docker Deployment (Production)

### Step 1: Build Optimized Images

```bash
# Build backend with multi-stage Dockerfile
cd lms-backend
docker build -f Dockerfile.optimized -t your-dockerhub-username/lms-backend:latest .

# Build frontend with Nginx
cd ../lms-frontend
docker build -f Dockerfile.optimized -t your-dockerhub-username/lms-frontend:latest .
```

### Step 2: Push to Docker Hub

```bash
docker login
docker push your-dockerhub-username/lms-backend:latest
docker push your-dockerhub-username/lms-frontend:latest
```

### Step 3: Deploy with Docker Compose

```bash
# Load environment variables
export $(cat .env.prod | xargs)

# Start all services
docker-compose -f docker-compose.prod.yml up -d

# Check service health
docker-compose -f docker-compose.prod.yml ps
docker-compose -f docker-compose.prod.yml logs -f

# Verify health endpoints
curl http://localhost:8080/actuator/health
curl http://localhost:80/health
```

### Step 4: Initialize Database

```bash
# Import initial data (if needed)
docker exec -i lms-mysql mysql -u lms_user -p lms_db < lms_db_backup.sql
```

---

## ☁️ AWS ECS Deployment

### Prerequisites Setup

1. **Install AWS CLI**:

   ```bash
   aws configure
   # Enter: Access Key ID, Secret Access Key, Region (us-east-1)
   ```

2. **Create ECR Repositories**:

   ```bash
   aws ecr create-repository --repository-name lms-backend --region us-east-1
   aws ecr create-repository --repository-name lms-frontend --region us-east-1
   ```

3. **Configure GitHub Secrets**:
   Go to GitHub → Settings → Secrets and variables → Actions → New repository secret:

   - `AWS_ACCESS_KEY_ID`: Your AWS access key
   - `AWS_SECRET_ACCESS_KEY`: Your AWS secret key
   - `AWS_REGION`: `us-east-1`
   - `ECR_REGISTRY`: `123456789012.dkr.ecr.us-east-1.amazonaws.com`
   - `ECS_CLUSTER`: `lms-cluster`
   - `ECS_SERVICE_BACKEND`: `lms-backend-service`
   - `ECS_SERVICE_FRONTEND`: `lms-frontend-service`
   - `ECS_TASK_DEFINITION_BACKEND`: `lms-backend-task`
   - `ECS_TASK_DEFINITION_FRONTEND`: `lms-frontend-task`

### Manual Deployment Steps

1. **Create VPC and Subnets** (if not exists):

   ```bash
   aws ec2 create-vpc --cidr-block 10.0.0.0/16
   aws ec2 create-subnet --vpc-id vpc-xxx --cidr-block 10.0.1.0/24
   ```

2. **Create ECS Cluster**:

   ```bash
   aws ecs create-cluster --cluster-name lms-cluster --region us-east-1
   ```

3. **Create Task Definitions**:

   - Backend: `aws/ecs-task-definition-backend.json`
   - Frontend: `aws/ecs-task-definition-frontend.json`

   ```bash
   aws ecs register-task-definition --cli-input-json file://aws/ecs-task-definition-backend.json
   aws ecs register-task-definition --cli-input-json file://aws/ecs-task-definition-frontend.json
   ```

4. **Create Services**:
   ```bash
   aws ecs create-service \
     --cluster lms-cluster \
     --service-name lms-backend-service \
     --task-definition lms-backend-task \
     --desired-count 2 \
     --launch-type FARGATE \
     --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx],securityGroups=[sg-xxx],assignPublicIp=ENABLED}"
   ```

### Automated Deployment (GitHub Actions)

Push to `main` branch triggers CI/CD pipeline:

```bash
git add .
git commit -m "Deploy to production"
git push origin main
```

The workflow will:

1. Run tests (backend + frontend)
2. Run security scans (Trivy + OWASP)
3. Build and push Docker images
4. Deploy to ECS with health checks
5. Rollback on failure

---

## 🎯 Health Checks & Monitoring

### Verify Deployment

```bash
# Backend health
curl https://api.yourdomain.com/actuator/health

# Expected response:
# {"status":"UP"}

# Frontend health
curl https://yourdomain.com/health

# Expected response:
# healthy
```

### Monitor Logs

**Docker Compose:**

```bash
docker-compose -f docker-compose.prod.yml logs -f backend
docker-compose -f docker-compose.prod.yml logs -f frontend
```

**AWS ECS:**

```bash
aws logs tail /ecs/lms-backend --follow --region us-east-1
aws logs tail /ecs/lms-frontend --follow --region us-east-1
```

### Prometheus Metrics (if enabled)

```bash
# Access Prometheus
http://localhost:9090

# View metrics
http://localhost:8080/actuator/prometheus
```

---

## 🔄 Rollback Procedures

### Docker Compose Rollback

```bash
# Stop current deployment
docker-compose -f docker-compose.prod.yml down

# Pull previous image version
docker pull your-dockerhub-username/lms-backend:previous-tag

# Start with previous version
docker-compose -f docker-compose.prod.yml up -d
```

### AWS ECS Rollback

```bash
# List task definitions
aws ecs list-task-definitions --family-prefix lms-backend

# Update service to previous version
aws ecs update-service \
  --cluster lms-cluster \
  --service lms-backend-service \
  --task-definition lms-backend-task:5
```

### Automated Rollback (GitHub Actions)

The deploy workflow automatically rolls back on failure. Manual rollback:

```bash
# Go to GitHub Actions → Workflows → Deploy
# Re-run the last successful deployment job
```

---

## 🛡️ Security Checklist

Before production deployment:

- [ ] Change all default passwords in `.env.prod`
- [ ] Use strong JWT secret (32+ bytes, random)
- [ ] Enable HTTPS/TLS (use AWS Certificate Manager or Let's Encrypt)
- [ ] Configure firewall rules (allow only 80/443/8080 from LB)
- [ ] Enable AWS WAF for DDoS protection
- [ ] Set up AWS CloudWatch alarms for:
  - High CPU usage (>80%)
  - High memory usage (>85%)
  - Failed health checks
  - 5xx error rate (>1%)
- [ ] Enable AWS RDS automated backups (MySQL)
- [ ] Configure Redis password authentication
- [ ] Rotate secrets every 90 days
- [ ] Enable audit logging in Spring Boot Actuator

---

## 📊 Performance Tuning

### Database Optimization

```sql
-- Add indexes for frequently queried columns
CREATE INDEX idx_books_isbn ON Books(isbn);
CREATE INDEX idx_loans_status ON Loan(returnDate);
CREATE INDEX idx_users_email ON Users(email);

-- Optimize MySQL configuration
SET GLOBAL innodb_buffer_pool_size = 2147483648; -- 2GB
SET GLOBAL max_connections = 200;
```

### Redis Caching Strategy

```properties
# application-prod.properties
spring.cache.redis.time-to-live=3600000
spring.cache.redis.cache-null-values=false
```

### JVM Tuning (Docker)

```dockerfile
# Already configured in Dockerfile.optimized
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
```

---

## 🆘 Troubleshooting

### Issue: Backend container keeps restarting

**Solution:**

```bash
# Check logs
docker logs lms-backend

# Common causes:
# 1. Database not ready → Wait for MySQL health check
# 2. Missing environment variables → Check .env.prod
# 3. Port conflict → Change PORT in .env.prod
```

### Issue: Frontend shows "Cannot connect to backend"

**Solution:**

```bash
# Check backend is running
curl http://localhost:8080/actuator/health

# Check nginx proxy configuration
docker exec -it lms-frontend cat /etc/nginx/nginx.conf

# Verify network connectivity
docker exec -it lms-frontend ping backend
```

### Issue: High database connection errors

**Solution:**

```properties
# Increase HikariCP pool size in application-prod.properties
spring.datasource.hikari.maximum-pool-size=50
spring.datasource.hikari.minimum-idle=10
```

---

## 📞 Support

- **Documentation**: See [START_HERE.md](../START_HERE.md)
- **GitHub Issues**: https://github.com/your-repo/issues
- **Email**: support@yourdomain.com
