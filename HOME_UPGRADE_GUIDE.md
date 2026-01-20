# Hướng Dẫn Hoàn Thiện Trang Chủ Thông Minh

## ✅ Đã Hoàn Thành

### 1. **Dữ Liệu Mẫu Tin Tức**

📄 File: `lms-backend/src/main/resources/db-sample-news.sql`

**Cách sử dụng:**

```bash
# Cách 1: Chạy trực tiếp từ MySQL
mysql -u root -p lms_db < lms-backend/src/main/resources/db-sample-news.sql

# Cách 2: Copy-paste vào MySQL Workbench hoặc DBeaver
```

**Dữ liệu được thêm:**

- 5 tin tức mẫu về: Lễ Tết, Ngày hội sách, Vinh danh độc giả, Sách mới, Hướng dẫn hệ thống

### 2. **Notification Badge - Thông Báo Tin Tức Mới** 🔴

📍 Component: `header.component.ts/html/css`

**Tính năng:**

- Badge đỏ hiển thị số tin tức chưa đọc
- Tự động cập nhật mỗi 5 phút
- Lưu timestamp đọc cuối cùng vào localStorage
- Animation pulse thu hút sự chú ý
- Click vào "Tin Tức" → đánh dấu đã đọc

**Cách sử dụng:**

1. Đăng nhập vào hệ thống
2. Badge sẽ hiện trên menu "Tin Tức" nếu có tin mới
3. Click vào để xem → badge biến mất

### 3. **Admin News Management** 📰

📍 Component: `admin-news.component.ts/html/css`
📍 Route: `/admin/news` (chỉ Admin)

**Tính năng CRUD:**

- ✅ **Create**: Tạo tin tức mới với title + content
- ✅ **Read**: Xem danh sách tất cả tin tức (grid view)
- ✅ **Update**: Chỉnh sửa tin tức đã có
- ✅ **Delete**: Xóa tin tức với confirmation
- 📧 **Email Notification**: Checkbox để gửi email đến tất cả user

**Cách truy cập:**

1. Đăng nhập với tài khoản Admin
2. Menu → Quản Lý → Tin Tức (hoặc `/admin/news`)
3. Click "Tạo Tin Tức Mới"
4. Điền form và chọn có gửi email hay không
5. Submit → Tin tức hiện trên trang chủ ngay lập tức

**Validation:**

- Title: 10-200 ký tự
- Content: 20-2000 ký tự
- Hiển thị char counter
- Real-time error messages

### 4. **AI Recommendations (Gợi Ý Thông Minh)** ✨

📍 Component: `home.component.ts`
📍 Section: "Có Thể Em Sẽ Thích"

**Hiện tại:**

- Shuffle random 6 cuốn sách có sẵn
- Chỉ hiện cho user đã login

**Cách nâng cấp AI (TODO):**

```typescript
// Step 1: Tạo endpoint mới ở Backend
@PostMapping("/user/chat/recommend-books")
public List<Book> getAIRecommendations(
  @RequestBody RecommendationRequest request
) {
  // Sử dụng RagService + VectorStore
  // Phân tích lịch sử mượn của user
  // Tìm sách tương tự bằng vector similarity
  return intelligentRecommendations;
}

// Step 2: Update Frontend
private loadPersonalizedRecommendations(): void {
  this.http.post<Book[]>(`${apiUrl}/user/chat/recommend-books`, {
    userId: this.userAuthService.getUserId(),
    preferences: userPreferences, // Từ profile
    borrowHistory: recentBorrows   // Từ circulation history
  }).subscribe(books => {
    this.recommendedBooks$ = of(books);
  });
}
```

**Vector Similarity Logic:**

```
User đã mượn: "Harry Potter", "Percy Jackson"
→ AI phân tích: Thể loại Fantasy, Thiếu niên, Phiêu lưu
→ Gợi ý: "Eragon", "The Hobbit", "Artemis Fowl"
```

## 📊 Kiến Trúc Tổng Quan

```
┌─────────────────────────────────────────────────────────┐
│                    TRANG CHỦ (Home)                      │
├─────────────────────────────────────────────────────────┤
│ 1. Hero Banner (Search + Autocomplete)                  │
│ 2. Danh Mục (Dynamic từ DB)                             │
│ 3. 📰 Bảng Tin (Latest 3 news)                          │
│ 4. 🏆 Leaderboard (Top 5 độc giả)                       │
│ 5. ✨ Gợi Ý AI (Personalized, nếu login)               │
│ 6. Sách Mới Nhập (Latest 10 books)                      │
└─────────────────────────────────────────────────────────┘
         ↓                    ↓                    ↓
    Categories          News Badge           AI Engine
    (CategoryService)   (NewsService)    (Future: ChatbotController)
```

## 🚀 Cách Kiểm Tra

### Test News System:

```bash
# 1. Chạy SQL script
mysql -u root -p lms_db < db-sample-news.sql

# 2. Restart backend
cd lms-backend && mvn spring-boot:run

# 3. Restart frontend
cd lms-frontend && npm start

# 4. Kiểm tra:
- Trang chủ: http://localhost:4200 → Xem "Bảng Tin Thư Viện"
- Admin: http://localhost:4200/admin/news → Quản lý tin tức
- Header: Badge đỏ hiện số tin mới
```

### Test Notification Badge:

```bash
# 1. Đăng nhập
# 2. Xóa localStorage key "lastReadNewsDate"
localStorage.removeItem('lastReadNewsDate');

# 3. Refresh → Badge hiện số tin tức
# 4. Click "Tin Tức" → Badge biến mất
```

### Test Admin CRUD:

```bash
# 1. Login as admin (username: admin, password: admin)
# 2. Navigate to /admin/news
# 3. Create new news with email notification checked
# 4. Check email (if configured)
# 5. Edit/Delete news items
```

## 🎯 Roadmap Tiếp Theo

### Phase 1: AI Enhancement (Ưu tiên cao)

- [ ] Tạo `RecommendationService.java` sử dụng VectorStore
- [ ] Endpoint `/user/chat/recommend-books`
- [ ] Train model với dữ liệu lịch sử mượn
- [ ] A/B testing: AI vs Random recommendations

### Phase 2: Analytics Dashboard

- [ ] Track click-through rate trên recommendations
- [ ] Heatmap: Sách nào được click nhiều nhất
- [ ] Conversion rate: Gợi ý → Mượn sách

### Phase 3: Social Features

- [ ] Comment/React trên tin tức
- [ ] Share tin tức qua email/social
- [ ] Bookmark tin tức yêu thích

### Phase 4: Mobile App

- [ ] Push notification cho tin tức mới
- [ ] In-app notification center
- [ ] Offline mode để đọc tin tức

## 📝 Notes

**Security:**

- Admin news endpoints đã có `@PreAuthorize("hasRole('ROLE_ADMIN')")`
- Public news endpoints không cần auth
- Notification badge chỉ hiện khi login

**Performance:**

- News list có pagination (limit parameter)
- Notification badge cache 5 phút
- Autocomplete debounce 300ms

**UX Best Practices:**

- Skeleton loading states
- Error fallbacks
- Empty states với hướng dẫn
- Confirmation dialogs cho delete actions

## 🐛 Troubleshooting

**Badge không hiện:**

```javascript
// Check localStorage
console.log(localStorage.getItem("lastReadNewsDate"));

// Force refresh
localStorage.removeItem("lastReadNewsDate");
location.reload();
```

**Tin tức không load:**

```bash
# Check backend logs
tail -f lms-backend/logs/spring.log | grep "PublicNewsController"

# Check API endpoint
curl http://localhost:8080/api/public/news/latest?limit=5
```

**Email không gửi:**

```properties
# Check application.properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

---

**Phát triển bởi:** Đội ngũ CNTT - THCS Phương Tú  
**Cập nhật:** 17/01/2026
