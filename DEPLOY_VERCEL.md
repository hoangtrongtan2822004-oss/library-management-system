# 🌐 Deploy Frontend lên Vercel - MIỄN PHÍ!

## ✅ Tại sao chọn Vercel?

- ✨ **Miễn phí hoàn toàn** cho personal projects
- ⚡ **Tự động deploy** mỗi khi push code lên GitHub
- 🌍 **CDN toàn cầu** - tốc độ cực nhanh
- 🔒 **HTTPS tự động** - không cần config SSL
- 📊 **Analytics miễn phí**

---

## 🚀 Bước 1: Chuẩn bị code

### 1.1. Tạo file vercel.json (đã có sẵn)

Kiểm tra file `lms-frontend/vercel.json`:

```json
{
  "$schema": "https://openapi.vercel.sh/vercel.json",
  "buildCommand": "npm run build",
  "outputDirectory": "dist/lms-frontend",
  "framework": "angular",
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

### 1.2. Update API Base URL

Sửa `lms-frontend/src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiBaseUrl: "https://your-backend-on-render.onrender.com/api", // Đổi URL này sau khi deploy backend
};
```

### 1.3. Push code lên GitHub

```bash
git add .
git commit -m "feat: ready for Vercel deployment"
git push origin main
```

---

## 🎯 Bước 2: Deploy lên Vercel

### Option 1: Deploy qua Dashboard (Dễ nhất)

1. **Đăng ký Vercel**:

   - Truy cập: https://vercel.com/signup
   - Chọn "Continue with GitHub"
   - Authorize Vercel truy cập GitHub

2. **Import Project**:

   - Click "Add New..." → "Project"
   - Chọn repository `library-management-system`
   - Click "Import"

3. **Configure Project**:

   ```
   Framework Preset: Angular
   Root Directory: lms-frontend
   Build Command: npm run build
   Output Directory: dist/lms-frontend
   Install Command: npm ci --legacy-peer-deps
   ```

4. **Environment Variables** (nếu cần):

   ```
   NODE_ENV=production
   ```

5. **Deploy**:
   - Click "Deploy"
   - Chờ 2-3 phút
   - ✅ **Done!** - Bạn sẽ có URL: `https://your-app.vercel.app`

### Option 2: Deploy qua CLI (Nhanh hơn)

```bash
# Install Vercel CLI
npm install -g vercel

# Login
vercel login

# Deploy
cd lms-frontend
vercel --prod
```

Trả lời các câu hỏi:

- Set up and deploy? **Y**
- Which scope? **Your account**
- Link to existing project? **N**
- Project name? **library-management-system**
- Directory? **.**
- Override settings? **N**

---

## 🔧 Bước 3: Cấu hình Domain & Settings

### 3.1. Custom Domain (Optional)

1. Vào **Project Settings** → **Domains**
2. Thêm domain của bạn (ví dụ: `lms.yourdomain.com`)
3. Cập nhật DNS records theo hướng dẫn Vercel
4. Chờ DNS propagate (5-10 phút)

### 3.2. Environment Variables

Nếu cần thêm biến môi trường:

1. Vào **Project Settings** → **Environment Variables**
2. Thêm:
   ```
   Name: API_BASE_URL
   Value: https://your-backend.onrender.com/api
   ```

### 3.3. Build Settings

Đảm bảo settings đúng:

- **Framework**: Angular
- **Root Directory**: `lms-frontend`
- **Build Command**: `npm run build`
- **Output Directory**: `dist/lms-frontend`
- **Install Command**: `npm ci --legacy-peer-deps`

---

## 🔄 Bước 4: Tự động deploy (CI/CD)

Mỗi khi bạn push code lên GitHub:

```bash
git add .
git commit -m "fix: update homepage"
git push origin main
```

→ Vercel sẽ **tự động detect** và deploy trong **2-3 phút**!

Xem tiến trình tại: https://vercel.com/dashboard

---

## 🎨 Bước 5: Kiểm tra deployment

### Check List

- ✅ Website load được: `https://your-app.vercel.app`
- ✅ Routing hoạt động: Thử `/login`, `/books`, `/admin`
- ✅ API calls hoạt động (nếu đã deploy backend)
- ✅ HTTPS tự động enable
- ✅ Mobile responsive

### Debug nếu lỗi

```bash
# Xem build logs
vercel logs <deployment-url>

# Xem logs realtime
vercel logs --follow
```

---

## 📊 Monitoring & Analytics

### Built-in Analytics (FREE)

1. Vào **Analytics** tab
2. Xem:
   - Page views
   - Unique visitors
   - Top pages
   - Geographic distribution

### Performance Insights

1. Vào **Speed Insights** (FREE tier: 500 views/month)
2. Xem Core Web Vitals:
   - LCP (Largest Contentful Paint)
   - FID (First Input Delay)
   - CLS (Cumulative Layout Shift)

---

## 🔐 Security & Best Practices

### 1. Environment Secrets

**KHÔNG BAO GIỜ** commit API keys vào Git. Dùng Vercel Environment Variables.

### 2. CORS Configuration

Đảm bảo backend cho phép origin từ Vercel:

```java
// WebSecurityConfiguration.java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList(
        "http://localhost:4200",
        "https://your-app.vercel.app"  // Add this
    ));
    // ... rest of config
}
```

### 3. Rate Limiting

Vercel FREE tier limits:

- ✅ Bandwidth: 100GB/month
- ✅ Deployments: Unlimited
- ✅ Build time: 6000 minutes/month
- ⚠️ Functions: 100GB-Hours/month

---

## 🆘 Troubleshooting

### ❌ Build failed: "Module not found"

**Giải pháp**: Đảm bảo `package.json` có đầy đủ dependencies:

```bash
cd lms-frontend
npm install
npm run build  # Test local trước
```

### ❌ Page not found (404) khi refresh

**Giải pháp**: Đã fix bằng `vercel.json` rewrites. Nếu vẫn lỗi:

```json
{
  "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }]
}
```

### ❌ API calls fail (CORS error)

**Giải pháp**:

1. Kiểm tra backend đã allow origin từ Vercel
2. Đảm bảo `environment.prod.ts` có đúng `apiBaseUrl`

### ❌ "Too many requests" error

**Giải pháp**: Nâng cấp lên **Pro plan** ($20/month) để tăng limits.

---

## 💡 Tips & Tricks

### 1. Preview Deployments

Mỗi Pull Request sẽ tự động tạo 1 preview URL:

```
https://your-app-git-feature-branch.vercel.app
```

### 2. Rollback dễ dàng

Nếu deployment mới có bug:

1. Vào **Deployments** tab
2. Click deployment cũ → "Promote to Production"

### 3. Custom subdomains

FREE plan cho phép tạo unlimited subdomains:

- `staging.your-app.vercel.app`
- `dev.your-app.vercel.app`

---

## 🎉 Kết quả

Sau khi deploy xong, bạn có:

✅ **Live Demo**: `https://your-app.vercel.app`  
✅ **HTTPS**: Tự động, không cần config  
✅ **CDN Global**: Tốc độ cực nhanh  
✅ **Auto Deploy**: Mỗi lần push code  
✅ **Free Forever**: Cho personal projects

---

## 📞 Next Steps

1. ✅ **Deploy Backend**: Xem `DEPLOY_RENDER.md`
2. 🔗 **Connect Frontend ↔ Backend**: Update `apiBaseUrl`
3. 🎯 **Share Link**: Gửi cho nhà tuyển dụng/khách hàng
4. 🚀 **Optional**: Setup custom domain

---

## 🔗 Useful Links

- [Vercel Dashboard](https://vercel.com/dashboard)
- [Vercel Docs - Angular](https://vercel.com/docs/frameworks/angular)
- [Vercel CLI Docs](https://vercel.com/docs/cli)
- [Pricing Plans](https://vercel.com/pricing)
