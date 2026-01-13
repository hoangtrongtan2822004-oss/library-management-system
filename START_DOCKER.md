# 🚀 Hướng Dẫn Chạy Docker - 1 Lệnh Duy Nhất!

## ⚡ Quick Start (Nhanh nhất)

```bash
# Clone repo (nếu chưa có)
git clone https://github.com/your-username/library-management-system.git
cd library-management-system

# Chạy toàn bộ stack bằng 1 lệnh
docker-compose -f docker-compose.simple.yml up --build
```

**Chờ 2-3 phút** để build xong, sau đó truy cập:

- 🌐 **Frontend**: http://localhost
- 🔧 **Backend API**: http://localhost:8080
- 🏥 **Health Check**: http://localhost:8080/actuator/health
- 📊 **Database**: localhost:3307 (MySQL)
- ⚡ **Cache**: localhost:6379 (Redis)

---

## 📋 Chi tiết các service

| Service  | Container Name      | Port      | Credentials                      |
| -------- | ------------------- | --------- | -------------------------------- |
| Frontend | lms-frontend-simple | 80        | -                                |
| Backend  | lms-backend-simple  | 8080      | -                                |
| MySQL    | lms-mysql-simple    | 3307→3306 | root/root, lms_user/lms_password |
| Redis    | lms-redis-simple    | 6379      | -                                |

---

## 🛠️ Các lệnh hữu ích

### Xem logs

```bash
# Xem logs tất cả services
docker-compose -f docker-compose.simple.yml logs -f

# Xem logs riêng từng service
docker-compose -f docker-compose.simple.yml logs -f backend
docker-compose -f docker-compose.simple.yml logs -f frontend
docker-compose -f docker-compose.simple.yml logs -f mysql
```

### Dừng và xóa

```bash
# Dừng tất cả containers (giữ data)
docker-compose -f docker-compose.simple.yml stop

# Dừng và xóa containers (giữ volumes/data)
docker-compose -f docker-compose.simple.yml down

# Xóa hết (bao gồm volumes/data)
docker-compose -f docker-compose.simple.yml down -v
```

### Rebuild khi thay đổi code

```bash
# Rebuild chỉ backend
docker-compose -f docker-compose.simple.yml up --build backend

# Rebuild chỉ frontend
docker-compose -f docker-compose.simple.yml up --build frontend

# Rebuild tất cả
docker-compose -f docker-compose.simple.yml up --build
```

### Truy cập vào container

```bash
# Vào backend container
docker exec -it lms-backend-simple sh

# Vào MySQL container
docker exec -it lms-mysql-simple mysql -u lms_user -p
# Password: lms_password

# Vào Redis container
docker exec -it lms-redis-simple redis-cli
```

---

## 🐛 Troubleshooting

### ❌ Backend không start được

**Lỗi**: `Access denied for user 'root'@'localhost'`

**Giải pháp**: Đảm bảo MySQL container đã chạy xong. Chờ thêm 30s rồi restart:

```bash
docker-compose -f docker-compose.simple.yml restart backend
```

### ❌ Port 80 hoặc 3307 đã bị chiếm

**Giải pháp**: Đổi port trong `docker-compose.simple.yml`:

```yaml
# Frontend: đổi 80 thành 8000
ports:
  - "8000:80"

# MySQL: đổi 3307 thành 3308
ports:
  - "3308:3306"
```

### ❌ Out of memory khi build

**Giải pháp**: Tăng RAM cho Docker Desktop:

- Docker Desktop → Settings → Resources → Memory
- Tăng lên ít nhất 4GB

---

## 🎯 Test Accounts

Sau khi khởi động xong, đăng nhập bằng:

**Admin Account:**

- Email: `admin@example.com`
- Password: `admin123`

**User Account:**

- Email: `user@example.com`
- Password: `user123`

---

## 🔐 Bảo mật (Production)

⚠️ **QUAN TRỌNG**: File `docker-compose.simple.yml` hiện tại chỉ dùng cho **development/testing**.

Khi deploy production, phải:

1. **Đổi JWT Secret**: Trong `docker-compose.simple.yml`, section `backend.environment.APP_JWT_SECRET`

   ```yaml
   APP_JWT_SECRET: your-random-32-bytes-secret-key-here
   ```

2. **Đổi MySQL Password**: Thay `MYSQL_ROOT_PASSWORD` và `MYSQL_PASSWORD`

3. **Enable HTTPS**: Dùng Nginx reverse proxy với Let's Encrypt SSL

4. **Tách riêng secrets**: Dùng file `.env` thay vì hardcode trong docker-compose

---

## 📊 Monitoring

### Health Checks

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Frontend health
curl http://localhost/

# MySQL health
docker exec lms-mysql-simple mysqladmin ping -h localhost -u root -p
```

### Resource Usage

```bash
# Xem CPU/RAM usage
docker stats

# Xem disk usage
docker system df
```

---

## 🚀 Next Steps

Sau khi chạy thành công local, tiếp theo:

1. ✅ **Push lên GitHub** (nếu chưa có)
2. 🌐 **Deploy Frontend lên Vercel** (xem `DEPLOY_VERCEL.md`)
3. 🔧 **Deploy Backend lên Render** (xem `DEPLOY_RENDER.md`)
4. 🎉 **Có live demo để share!**

---

## 💡 Tips

- **Build lần đầu**: Mất 5-10 phút (download dependencies)
- **Build lần sau**: Chỉ 30s-1 phút (nhờ Docker layer caching)
- **Development**: Dùng `docker-compose.simple.yml` (đơn giản)
- **Production**: Dùng `docker-compose.prod.yml` (security hardened)

---

## 🆘 Support

- **GitHub Issues**: https://github.com/your-username/library-management-system/issues
- **Documentation**: Xem folder `.github/` cho thêm guides
