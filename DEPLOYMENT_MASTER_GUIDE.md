# 🚀 Deployment Master Guide

**🎯 Mục tiêu:** Đưa Library Management System lên live demo trong 30 phút!

---

## 📋 Table of Contents

1. [Chọn nền tảng deployment](#1-chọn-nền-tảng)
2. [Roadmap: From Local → Live](#2-roadmap)
3. [Quick Start (Fastest Path)](#3-quick-start)
4. [Production Checklist](#4-production-checklist)
5. [After Deployment](#5-after-deployment)

---

## 1. Chọn nền tảng

### Frontend (Angular)

| Platform         | Cost | Setup Time | Pros                                  | Cons                     |
| ---------------- | ---- | ---------- | ------------------------------------- | ------------------------ |
| **Vercel** ⭐    | FREE | 5 mins     | Auto deploy, fastest CDN, zero config | Limited build minutes    |
| **Netlify**      | FREE | 5 mins     | Similar to Vercel, great DX           | Slightly slower          |
| **GitHub Pages** | FREE | 10 mins    | Simple, integrated with GitHub        | No server-side rendering |

**🏆 Recommendation: Vercel** (Best for Angular, auto-deploy, global CDN)

---

### Backend (Spring Boot + MySQL + Redis)

| Platform       | Cost            | Setup Time | Pros                                 | Cons                          | Sleep?  |
| -------------- | --------------- | ---------- | ------------------------------------ | ----------------------------- | ------- |
| **Render**     | FREE            | 15 mins    | True free tier, PostgreSQL/MySQL     | Sleeps after 15min ⚠️         | ✅ Yes  |
| **Railway** ⭐ | $5 credit/month | 10 mins    | No sleep, fast deploy, Docker native | Requires credit card after $5 | ❌ No   |
| **Fly.io**     | FREE (limited)  | 15 mins    | Good performance, Docker             | Complex config                | Partial |
| **Heroku**     | $5/month        | 10 mins    | Easy setup, add-ons                  | No free tier anymore          | ❌ No   |

**🏆 Recommendations:**

- 💰 **100% Free:** Render (accept 15min sleep)
- 💪 **Best Performance:** Railway ($5/month, no sleep, faster)

---

## 2. Roadmap

```
┌─────────────────────────────────────────────────────────────┐
│ Step 1: Prepare Code (5 mins)                              │
├─────────────────────────────────────────────────────────────┤
│ ✅ Test local với Docker: docker-compose up                 │
│ ✅ Push to GitHub                                            │
│ ✅ Verify .env.example has all variables                    │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│ Step 2: Deploy Backend (15 mins)                           │
├─────────────────────────────────────────────────────────────┤
│ Option A: Render (Free)                                     │
│   → See: DEPLOY_RENDER.md                                   │
│                                                              │
│ Option B: Railway ($5/month)                                │
│   → See: DEPLOY_RAILWAY.md                                  │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│ Step 3: Deploy Frontend (5 mins)                           │
├─────────────────────────────────────────────────────────────┤
│ Vercel (Recommended)                                         │
│   → See: DEPLOY_VERCEL.md                                   │
│                                                              │
│ 1. Connect GitHub repo                                      │
│ 2. Set Root: lms-frontend                                   │
│ 3. Add env: API_BASE_URL=<backend-url>                     │
│ 4. Deploy!                                                   │
└─────────────────────────────────────────────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────┐
│ Step 4: Connect & Test (5 mins)                            │
├─────────────────────────────────────────────────────────────┤
│ ✅ Update frontend apiBaseUrl → backend URL                 │
│ ✅ Update backend CORS → allow frontend origin              │
│ ✅ Test: Login, CRUD books, chatbot                         │
│ ✅ Share demo URL! 🎉                                       │
└─────────────────────────────────────────────────────────────┘
```

**Total Time: ~30 minutes** ⚡

---

## 3. Quick Start

### Path A: 100% FREE (Render + Vercel)

```bash
# 1. Test local
docker-compose -f docker-compose.simple.yml up

# 2. Push to GitHub
git add .
git commit -m "feat: ready for deployment"
git push origin main

# 3. Deploy Backend to Render
# → Follow: DEPLOY_RENDER.md
# → Result: https://your-backend.onrender.com

# 4. Deploy Frontend to Vercel
# → Follow: DEPLOY_VERCEL.md
# → Update environment.prod.ts:
#   apiBaseUrl: 'https://your-backend.onrender.com/api'
# → Result: https://your-app.vercel.app

# 5. Test
curl https://your-backend.onrender.com/actuator/health
open https://your-app.vercel.app
```

**Pros:**

- ✅ 100% FREE
- ✅ No credit card
- ✅ Auto deploy from GitHub

**Cons:**

- ⚠️ Backend sleeps after 15min (first request takes 30s)
- ⚠️ Build time limits

---

### Path B: BEST PERFORMANCE (Railway + Vercel)

```bash
# Same as Path A, but deploy backend to Railway
# → Follow: DEPLOY_RAILWAY.md
# → Result: https://your-backend.up.railway.app
```

**Pros:**

- ✅ Backend NEVER sleeps
- ✅ Faster deploy
- ✅ Better performance

**Cons:**

- 💰 $5 FREE/month, then ~$10/month for small app

---

## 4. Production Checklist

### 🔐 Security

```bash
# Before deploy:
□ Change JWT secret: APP_JWT_SECRET (32+ bytes random)
□ Use strong DB password
□ Enable HTTPS (auto on Vercel/Render/Railway)
□ Update CORS origins
□ Remove test accounts or use strong passwords
□ Disable Spring Boot DevTools in prod
□ Set SPRING_PROFILES_ACTIVE=prod
```

**Generate secrets:**

```powershell
# PowerShell: Generate 32-byte secret
-join ((48..57)+(65..90)+(97..122)|Get-Random -Count 32|%{[char]$_})
```

---

### 📊 Environment Variables

**Backend (Required):**

```bash
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://...
DB_USERNAME=...
DB_PASSWORD=...
APP_JWT_SECRET=<32-byte-random-string>
REDIS_HOST=...
REDIS_PORT=6379
JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC
```

**Backend (Optional):**

```bash
GEMINI_API_KEY=...          # For chatbot
MAIL_HOST=smtp.gmail.com    # For password reset
MAIL_USERNAME=...
MAIL_PASSWORD=...
```

**Frontend:**

```bash
# In environment.prod.ts (NOT .env)
apiBaseUrl: 'https://your-backend.com/api'
```

---

### 🗄️ Database

```bash
# Import initial data
mysql -h <host> -u <user> -p<password> <database> < lms_db_backup.sql

# Or use Railway CLI:
railway connect mysql
SOURCE lms_db_backup.sql;
```

---

### 🧪 Testing

```bash
# Test backend health
curl https://your-backend.com/actuator/health

# Test API endpoints
curl https://your-backend.com/api/public/books

# Test login
curl -X POST https://your-backend.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'

# Test frontend
open https://your-app.vercel.app
# → Login with admin/user123
# → Browse books
# → Try chatbot
```

---

## 5. After Deployment

### 📝 Update README

```markdown
## 🌐 Live Demo

**Frontend:** https://your-app.vercel.app
**Backend API:** https://your-backend.com/api

**Test Accounts:**

- Admin: admin@example.com / admin123
- User: user@example.com / user123
```

---

### 🔗 Share Your Demo

**Portfolio:**

```
🚀 Library Management System
- Full-stack: Spring Boot + Angular + MySQL
- Features: AI Chatbot, Gamification, JWT Auth
- Live: https://your-app.vercel.app
- Code: https://github.com/yourusername/library-management-system
```

**LinkedIn Post:**

```
🎉 Just deployed my Library Management System!

Tech stack:
- Backend: Spring Boot 3.5 + Java 21
- Frontend: Angular + TypeScript
- Database: MySQL + Redis
- AI: Google Gemini Chatbot
- Deployment: Vercel + Render/Railway

Try it: https://your-app.vercel.app
GitHub: https://github.com/yourusername/repo

#FullStack #SpringBoot #Angular #Java #WebDevelopment
```

---

### 📊 Monitoring

**Vercel:**

- Analytics: https://vercel.com/dashboard/analytics
- Logs: https://vercel.com/dashboard/deployments

**Render:**

- Metrics: Dashboard → Service → Metrics
- Logs: Dashboard → Service → Logs

**Railway:**

- Usage: Dashboard → Usage tab
- Logs: `railway logs`

---

### 🔄 CI/CD (Optional)

Enable auto-deploy:

**Vercel:**

- ✅ Auto-enabled: Push to `main` → auto deploy

**Render:**

- ✅ Auto-enabled: Push to `main` → auto deploy
- Configure: Settings → Auto-Deploy

**Railway:**

- ✅ Auto-enabled: Push to `main` → auto deploy
- Configure: Settings → Automatic Deploys

---

### 🐛 Troubleshooting

#### ❌ Frontend shows "API connection error"

**Check:**

1. Backend health: `curl https://backend.com/actuator/health`
2. CORS: Check backend allows frontend origin
3. API URL: Verify `environment.prod.ts` has correct URL

#### ❌ Backend 503 Service Unavailable (Render)

**Cause:** Free tier sleep

**Solutions:**

1. Wait 30s for wake-up
2. Use cron job to ping every 14 min: https://cron-job.org
3. Upgrade to Starter plan ($7/mo)

#### ❌ Database connection error

**Check:**

1. DB credentials correct
2. Database is running
3. Connection string format: `jdbc:mysql://host:port/db`
4. Use internal URL (e.g., `mysql.railway.internal` not public URL)

#### ❌ Out of memory

**Fix:**

```bash
# Optimize JVM
JAVA_OPTS=-Xms256m -Xmx450m -XX:MaxRAMPercentage=70.0 -XX:+UseG1GC

# Or increase RAM on platform
```

---

## 🎯 Next Steps

After successful deployment:

1. **⭐ Add to Portfolio**

   - Update resume/CV
   - Add to LinkedIn projects
   - Share on GitHub profile README

2. **📈 Implement Analytics**

   - Vercel Analytics (free)
   - Google Analytics
   - Sentry for error tracking

3. **🚀 Advanced Features** (Optional)

   - [ ] Real-time notifications (WebSocket)
   - [ ] Dark mode completion
   - [ ] Payment integration (VNPay/MoMo)
   - [ ] PWA support
   - [ ] Internationalization (i18n)

4. **🔐 Harden Security**
   - [ ] Rate limiting on login
   - [ ] CAPTCHA on sensitive endpoints
   - [ ] Audit logs
   - [ ] Backup strategy

---

## 📚 Documentation

| Guide                                                         | Description                       |
| ------------------------------------------------------------- | --------------------------------- |
| [🐳 START_DOCKER.md](./START_DOCKER.md)                       | Local Docker setup                |
| [📘 DEPLOY_VERCEL.md](./DEPLOY_VERCEL.md)                     | Deploy frontend to Vercel         |
| [📙 DEPLOY_RENDER.md](./DEPLOY_RENDER.md)                     | Deploy backend to Render (FREE)   |
| [🚂 DEPLOY_RAILWAY.md](./DEPLOY_RAILWAY.md)                   | Deploy backend to Railway ($5/mo) |
| [🔒 SECURITY_BEST_PRACTICES.md](./SECURITY_BEST_PRACTICES.md) | Security checklist                |

---

## 🎉 Congratulations!

Bạn đã hoàn thành deployment! 🚀

**Your live demo is ready to share with:**

- 👔 Employers (CV/Resume)
- 💼 Clients (Portfolio)
- 🎓 Professors (Graduation project)
- 👥 Friends (Show off!)

**Questions?** Check documentation or open GitHub issue.

**Good luck!** 🍀
