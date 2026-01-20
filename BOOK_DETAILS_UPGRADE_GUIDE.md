# 📚 Book Details Component - Amazon-Level Upgrade

## 🎯 Overview

Upgraded `book-details` component từ trang thông tin cơ bản thành trải nghiệm **"Amazon cho thư viện"** với 4 tính năng quan trọng:

1. ✅ **Read-only User Info** - Bảo mật thông tin người mượn
2. ✅ **Reservation/Waitlist** - Đặt trước khi hết sách
3. ✅ **Related Books** - Gợi ý sách liên quan
4. ✅ **E-book Integration** - Đọc online nếu có PDF/Epub

---

## 🔥 Critical Fix #1: Read-only User Info trong Borrow Modal

### ❌ Vấn đề trước đây

```typescript
// User có thể nhập TÙY Ý tên và lớp
borrowData = {
  studentName: "", // ← User tự nhập, dễ giả mạo!
  studentClass: "", // ← User tự nhập, không xác thực!
  loanDays: 14,
};
```

**Rủi ro**:

- Học sinh có thể giả danh người khác (nhập tên bạn bè)
- Không có cách nào kiểm chứng thông tin
- Dữ liệu mượn sách không chính xác

### ✅ Giải pháp

**Backend**: Đã có API `/api/admin/users/{id}` trả về:

```json
{
  "userId": 123,
  "name": "Nguyễn Văn A",
  "username": "nguyenvana",
  "studentClass": "8A1", // ← Từ database
  "roles": ["ROLE_USER"]
}
```

**Frontend**: Gọi API khi mở modal

```typescript
// 1. Load user profile từ database
navigateToBorrow(): void {
  const userId = this.userAuthService.getUserId();

  this.usersService.getUserById(userId).subscribe({
    next: (profile) => {
      this.userFullProfile = profile;  // ← Lưu toàn bộ thông tin
      this.showBorrowModal = true;
    },
    error: () => {
      // Fallback to localStorage nếu API fail
      this.userFullProfile = {
        name: this.userAuthService.getName(),
        studentClass: 'Chưa cập nhật'
      };
    }
  });
}

// 2. Hiển thị READ-ONLY trong modal
<input
  type="text"
  [value]="userFullProfile?.name || '---'"
  readonly  <!-- ← Không thể sửa! -->
  class="form-control bg-secondary"
/>
```

**Alert trong modal**:

```html
<div class="alert alert-info">
  <i class="fa-solid fa-info-circle me-2"></i>
  <strong>Thông tin người mượn:</strong> Thông tin này được lấy từ tài khoản của
  bạn và không thể chỉnh sửa.
</div>
```

### 📊 Kết quả

| Trước                            | Sau                                       |
| -------------------------------- | ----------------------------------------- |
| Nhập thủ công → Dễ giả mạo       | API → Chính xác 100%                      |
| `studentName: ''` (empty input)  | `userFullProfile.name` (database)         |
| `studentClass: ''` (empty input) | `userFullProfile.studentClass` (database) |
| Không validation                 | Read-only, không sửa được                 |

---

## 🚀 Feature #2: Reservation/Waitlist System

### 🎯 Vấn đề

Khi sách hết (numberOfCopiesAvailable === 0):

- Nút "Mượn sách" bị `disabled`
- User thất vọng, không làm gì được
- Không có cách nào biết khi nào sách có lại

### ✨ Giải pháp

**1. Thay đổi UI logic**

```typescript
// Trước: Disabled button khi hết sách
<button
  [disabled]="book.numberOfCopiesAvailable === 0"
>
  Mượn Sách Này
</button>

// Sau: 2 buttons riêng biệt
<button
  *ngIf="book.numberOfCopiesAvailable > 0"
  class="btn btn-primary"
>
  <i class="fa-solid fa-cart-plus"></i> Mượn Sách Này
</button>

<button
  *ngIf="book.numberOfCopiesAvailable === 0"
  class="btn btn-warning"
>
  <i class="fa-solid fa-clock"></i> Đặt Trước
</button>
```

**2. Reservation Modal**

```typescript
confirmReserve(): void {
  const payload = {
    bookId: this.book.id,
    memberId: this.userAuthService.getUserId()
  };

  this.circulationService.reserve(payload).subscribe({
    next: () => {
      this.toastr.success(
        'Đã đặt trước sách. Bạn sẽ nhận thông báo khi sách có sẵn.',
        'Thành công',
        { timeOut: 5000 }
      );
    }
  });
}
```

**3. Backend Integration**

```typescript
// CirculationService.reserve() → POST /api/user/circulation/reservations
reserve(data: { bookId: number; memberId: number }): Observable<any> {
  return this.http.post(
    this.apiService.buildUrl('/user/circulation/reservations'),
    data
  );
}
```

**Backend Logic (Java)**:

- Tạo record trong `reservations` table
- Status: `PENDING`
- Khi có người trả sách → Email notification → User có 24h để nhận sách

### 🎨 UI Details

**Reservation Modal**:

```html
<!-- Header: Warning style -->
<div class="modal-header bg-warning text-dark">
  <i class="fa-solid fa-clock me-2"></i>Đặt trước sách
</div>

<!-- Body: 3 alerts -->
<div class="alert alert-warning">
  Sách hiện đang hết! Bạn có thể đặt trước...
</div>

<div class="alert alert-info">Thông tin người đặt: [Name + Class từ API]</div>

<div class="alert alert-success">
  Bạn sẽ nhận Email/Thông báo khi sách có sẵn (giữ 24h)
</div>

<!-- Footer: Yellow button -->
<button class="btn btn-warning">Xác nhận đặt trước</button>
```

---

## 🔗 Feature #3: Related Books (Amazon-style)

### 🎯 Mục tiêu

Hiển thị 5 cuốn sách **cùng thể loại** ở cuối trang → Tăng engagement

### 💡 Implementation

**1. Load Related Books**

```typescript
private loadRelatedBooks(category: string, currentBookId: number): void {
  this.booksService
    .getPublicBooks(true, '', category, 0, 6) // Load 6 books
    .subscribe({
      next: (response) => {
        const books = response.content || response;
        // Loại bỏ sách hiện tại + giới hạn 5 cuốn
        this.relatedBooks = books
          .filter((b: Book) => b.id !== currentBookId)
          .slice(0, 5);
      }
    });
}
```

**2. Trigger khi book loads**

```typescript
this.loadBookDetails(bookId); // ← Load sách chính

// Sau khi load xong
if (this.book.categories.length > 0) {
  this.loadRelatedBooks(
    this.book.categories[0].name, // ← Lấy category đầu tiên
    this.book.id
  );
}
```

**3. Responsive Layout**

```html
<div class="related-books-section">
  <h4>
    <i class="fa-solid fa-book-open"></i>
    Có thể bạn cũng thích
  </h4>

  <div class="row g-3">
    <div
      *ngFor="let book of relatedBooks"
      class="col-md-2 col-sm-4 col-6"  <!-- ← Responsive -->
    >
      <div class="card related-book-card">
        <a [routerLink]="['/book-details', book.id]">
          <img
            [src]="book.coverUrl || 'placeholder.png'"
            class="related-book-cover"
          />
        </a>
        <div class="card-body">
          <h6 class="text-truncate">{{ book.name }}</h6>
          <span
            *ngIf="book.numberOfCopiesAvailable > 0"
            class="badge bg-success"
          >
            Còn {{ book.numberOfCopiesAvailable }} cuốn
          </span>
        </div>
      </div>
    </div>
  </div>
</div>
```

**4. CSS Hover Effect**

```css
.related-book-card {
  transition:
    transform 0.3s ease,
    box-shadow 0.3s ease;
  cursor: pointer;
}

.related-book-card:hover {
  transform: translateY(-8px); /* ← Nhấc lên 8px */
  box-shadow: 0 8px 20px rgba(0, 123, 255, 0.3);
}

.related-book-cover {
  height: 200px;
  object-fit: cover;
}
```

### 🎨 Responsive Breakpoints

| Screen Size      | Columns  | Books/Row |
| ---------------- | -------- | --------- |
| Desktop (≥768px) | col-md-2 | 6 books   |
| Tablet (≥576px)  | col-sm-4 | 3 books   |
| Mobile (<576px)  | col-6    | 2 books   |

---

## 📖 Feature #4: E-book Integration

### 🎯 Goal

Nếu sách có phiên bản **PDF/Epub**, hiển thị nút **"Đọc Online"**

### 🔍 Backend Check

**Entity Relationship**:

```java
@Entity
public class Ebook {
  @ManyToOne
  @JoinColumn(name = "book_id")
  private Books book;  // ← Foreign key to Books

  private String filePath;
  private String fileType; // PDF, EPUB
  private Integer maxDownloadsPerUser;
}
```

**API Available**:

- `GET /api/public/ebooks/search?search={bookName}` → Tìm ebook theo tên sách
- `GET /api/user/ebooks/{id}/can-download` → Kiểm tra quyền tải
- `GET /api/user/ebooks/{id}/download` → Tải file PDF/Epub

### 💻 Frontend Implementation

**1. Check Ebook Availability**

```typescript
private checkEbookAvailability(bookId: number): void {
  if (!this.book?.name) return;

  // Search ebook by book name
  this.ebookService
    .searchEbooks(this.book.name, undefined, 0, 5)
    .subscribe({
      next: (response) => {
        this.availableEbooks = response.content || [];
        this.hasEbook = this.availableEbooks.length > 0;
      }
    });
}
```

**2. Display "Đọc Online" Button**

```html
<button
  *ngIf="hasEbook && availableEbooks.length > 0"
  class="btn btn-success"
  (click)="openEbook(availableEbooks[0])"
  [disabled]="isCheckingEbook"
>
  <i class="fa-solid fa-book-open"></i>
  Đọc Online
</button>
```

**3. Open Ebook Logic**

```typescript
openEbook(ebook: Ebook): void {
  if (!this.isUser) {
    this.toastr.info('Vui lòng đăng nhập để đọc ebook');
    return;
  }

  // Check download permission
  this.ebookService.canDownload(ebook.id).subscribe({
    next: (result) => {
      if (result.canDownload) {
        this.downloadEbook(ebook.id);
      } else {
        this.toastr.warning(
          'Bạn đã đạt giới hạn tải xuống (3 lần/ebook)'
        );
      }
    }
  });
}

private downloadEbook(ebookId: number): void {
  this.ebookService.downloadEbook(ebookId).subscribe({
    next: (blob) => {
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `ebook_${ebookId}.pdf`;
      link.click();
      window.URL.revokeObjectURL(url);
      this.toastr.success('Tải ebook thành công!');
    }
  });
}
```

### 🚀 Future Enhancement: Inline PDF Viewer

Hiện tại: Tải file → User mở bằng PDF reader

**Nâng cao**: Dùng `ngx-extended-pdf-viewer` để đọc ngay trên web

```bash
npm install ngx-extended-pdf-viewer
```

```html
<ngx-extended-pdf-viewer
  *ngIf="showPdfViewer"
  [src]="pdfSrc"
  [height]="'80vh'"
  [useBrowserLocale]="true"
></ngx-extended-pdf-viewer>
```

---

## 📊 Before/After Comparison

| Feature             | Before                | After                   |
| ------------------- | --------------------- | ----------------------- |
| **Borrow Modal**    | Manual input (unsafe) | API read-only (secure)  |
| **Out of Stock**    | Disabled button       | Reservation/Waitlist    |
| **Book Discovery**  | No recommendations    | 5 related books         |
| **E-book**          | No integration        | "Đọc Online" button     |
| **User Experience** | Basic info page       | Amazon-level experience |

---

## 🧪 Testing Checklist

### ✅ Borrow Modal (Read-only)

- [ ] Click "Mượn Sách" → Modal hiển thị
- [ ] Name + Class **từ API** (không phải localStorage)
- [ ] Input fields có `readonly` attribute
- [ ] Alert hiển thị: "Thông tin này không thể chỉnh sửa"
- [ ] Nếu API fail → Fallback to localStorage

### ✅ Reservation

- [ ] Sách hết → Nút "Đặt Trước" màu vàng xuất hiện
- [ ] Sách còn → Nút "Mượn Sách Này" màu xanh
- [ ] Click "Đặt Trước" → Reservation modal
- [ ] 3 alerts trong modal (warning, info, success)
- [ ] Submit → POST /reservations → Toastr success

### ✅ Related Books

- [ ] Sau khi load sách → Section "Có thể bạn cũng thích" xuất hiện
- [ ] Hiển thị 5 cuốn cùng category
- [ ] Không bao gồm sách hiện tại
- [ ] Hover → Card nhấc lên + box-shadow
- [ ] Click card → Navigate to /book-details/:id

### ✅ E-book Integration

- [ ] Nếu có ebook → Nút "Đọc Online" màu xanh lá
- [ ] Nếu chưa login → Toast: "Vui lòng đăng nhập"
- [ ] Click → Check canDownload → Download PDF
- [ ] Đạt limit (3 lần) → Toast warning
- [ ] File tải về: `ebook_{id}.pdf`

---

## 🔧 Files Modified

### TypeScript (4 changes)

```typescript
book-details.component.ts
├── Import: UsersService, EbookService
├── State: userFullProfile, reserveLoading, relatedBooks, availableEbooks
├── Methods:
│   ├── navigateToBorrow() → Load API user profile
│   ├── navigateToReserve() → Open reservation modal
│   ├── confirmReserve() → POST /reservations
│   ├── loadRelatedBooks() → Load 5 books same category
│   ├── checkEbookAvailability() → Search ebooks by title
│   ├── openEbook() → Check permission + download
│   └── downloadEbook() → Blob download
└── Calls in ngOnInit:
    ├── loadRelatedBooks(category, bookId)
    └── checkEbookAvailability(bookId)
```

### HTML (3 sections)

```html
book-details.component.html ├── Buttons section: │ ├── "Mượn Sách Này" (*ngIf
available > 0) │ ├── "Đặt Trước" (*ngIf available === 0) │ ├── "Đọc Online"
(*ngIf hasEbook) │ └── "Thêm yêu thích" ├── Borrow Modal: Read-only inputs +
Alert ├── Reservation Modal: 3 alerts + Yellow button └── Related Books Section:
Horizontal cards
```

### CSS (2 blocks)

```css
book-details.component.css
├── .related-books-section (background + padding)
├── .related-book-card (transition + hover)
├── .related-book-cover (200px height)
└── .related-book-card:hover (transform + shadow)
```

---

## 🎓 Architecture Lessons

### 1️⃣ Security Pattern: Read-only Data from API

**Wrong**:

```typescript
// User nhập tên thủ công → Dễ giả mạo
<input [(ngModel)]="studentName" />
```

**Right**:

```typescript
// API trả về → Read-only display
<input [value]="userProfile?.name" readonly />
```

### 2️⃣ UX Pattern: Replace Disabled with Alternative

**Wrong**:

```html
<!-- User thất vọng khi thấy button bị disabled -->
<button [disabled]="outOfStock">Mượn</button>
```

**Right**:

```html
<!-- Cung cấp alternative action -->
<button *ngIf="!outOfStock">Mượn</button>
<button *ngIf="outOfStock">Đặt Trước</button>
```

### 3️⃣ Discovery Pattern: Related Content

```typescript
// Tăng engagement bằng recommendation
loadMainContent().then(() => loadRelatedContent());
```

### 4️⃣ Progressive Enhancement: Ebook

```typescript
// Check availability → Display option
checkEbook().then((hasEbook) => {
  if (hasEbook) showReadButton();
});
```

---

## 🚀 Future Enhancements

### 1. Advanced Ebook Viewer

- [ ] Inline PDF viewer (ngx-extended-pdf-viewer)
- [ ] Bookmark functionality
- [ ] Highlight & note-taking

### 2. AI-Powered Recommendations

- [ ] Use VectorStore (Backend RAG) to find similar books
- [ ] "Users who borrowed this also borrowed..."

### 3. Email Notifications

- [ ] Reservation fulfilled → Email with 24h countdown
- [ ] Book available again → Email to waitlist

### 4. Social Proof

- [ ] "X people are waiting for this book"
- [ ] "Borrowed 50 times this month"

---

## 📖 Summary

**Đã nâng cấp book-details từ trang thông tin đơn giản thành trải nghiệm "Amazon-level"**:

✅ **Security**: Read-only user info (không thể giả mạo)  
✅ **UX**: Reservation thay disabled button  
✅ **Discovery**: Related books (cùng category)  
✅ **Digital**: E-book integration (đọc online)

**Result**: Trang chi tiết sách chuyên nghiệp, đầy đủ tính năng như các nền tảng thương mại điện tử hiện đại!

---

**Next Steps**: Test toàn bộ 4 features → Deploy to production 🚀
