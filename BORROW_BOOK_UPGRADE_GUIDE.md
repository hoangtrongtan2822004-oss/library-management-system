# 📚 Borrow Book Component - Upgrade Guide

## 🎯 Tổng quan nâng cấp

Component `borrow-book` đã được nâng cấp toàn diện với **4 tính năng chính** để cải thiện trải nghiệm người dùng:

### ✅ Các tính năng đã thêm:

1. **🛒 Borrow Cart (Giỏ Sách)** - Mượn hàng loạt
2. **⚡ Debounce Search** - Tìm kiếm thông minh
3. **📷 Continuous Scanner** - Quét QR liên tục với âm thanh
4. **💎 Skeleton Loading + CSS Variables** - UI hiện đại

---

## 1. 🛒 Borrow Cart (Giỏ Sách)

### Vấn đề cũ:

- Người dùng phải mượn **từng cuốn một**
- Click "Mượn" → Modal → Xác nhận → Đóng → Lặp lại
- **Rất chậm** khi muốn mượn 3-5 cuốn cùng lúc

### Giải pháp:

- Chuyển sang mô hình **Add to Cart**
- Thêm nhiều sách vào giỏ → Mượn tất cả một lần

### Cách sử dụng:

#### **Thêm sách vào giỏ:**

```typescript
addToCart(book: Book): void {
  // Check if already in cart
  if (this.isInCart(book.id)) {
    this.toastr.info('Sách này đã có trong giỏ!');
    return;
  }

  this.cartItems.push({ book, addedAt: new Date() });
  this.toastr.success(`Đã thêm "${book.name}" vào giỏ!`);
}
```

#### **Xem giỏ sách:**

- Click button **🛒** (góc trên cùng)
- Badge đỏ hiển thị số lượng sách trong giỏ
- Modal hiển thị danh sách đầy đủ với thumbnail

#### **Mượn tất cả:**

```typescript
borrowAllFromCart(): void {
  const borrowPromises = this.cartItems.map(item => {
    const payload: BorrowCreate = {
      bookId: item.book.id,
      memberId: this.userId!,
      loanDays: this.borrowData.loanDays,
    };
    return this.circulationService.loan(payload).toPromise();
  });

  Promise.all(borrowPromises).then(() => {
    this.toastr.success(`Đã mượn thành công ${this.cartItems.length} cuốn sách!`);
  });
}
```

### UI Changes:

```html
<!-- Button giỏ sách với badge -->
<button class="btn btn-warning position-relative" (click)="toggleCartModal()">
  <i class="fa-solid fa-shopping-cart"></i>
  <span *ngIf="cartCount > 0" class="badge rounded-pill bg-danger">
    {{ cartCount }}
  </span>
</button>

<!-- Thẻ sách: thay "Mượn" thành "Thêm vào giỏ" -->
<button
  class="btn btn-primary"
  [disabled]="isInCart(book.id)"
  (click)="addToCart(book)"
>
  <i [ngClass]="isInCart(book.id) ? 'fa-check' : 'fa-cart-plus'"></i>
  {{ isInCart(book.id) ? "Đã thêm" : "Thêm vào giỏ" }}
</button>
```

---

## 2. ⚡ Debounce Search

### Vấn đề cũ:

- `onFilterChange()` gọi API **ngay lập tức** khi user gõ phím
- Gõ "Harry Potter" (12 ký tự) → 12 API calls
- **Lag server** và waste resources

### Giải pháp:

- Áp dụng **RxJS debounceTime(500ms)**
- Chỉ gọi API khi user **ngừng gõ** 500ms

### Implementation:

```typescript
// Setup trong ngOnInit
private searchSubject = new Subject<string>();

ngOnInit(): void {
  this.searchSubject
    .pipe(
      debounceTime(500),        // Đợi 500ms sau khi ngừng gõ
      distinctUntilChanged(),   // Chỉ gọi nếu giá trị thay đổi
      takeUntil(this.destroy$)
    )
    .subscribe(() => {
      this.currentPage = 1;
      this.loadBooks();
    });
}

// Trigger khi user gõ
onFilterChange(): void {
  this.searchSubject.next(this.searchTerm);
}

// Filter dropdown thì instant (không cần debounce)
onGenreChange(): void {
  this.currentPage = 1;
  this.loadBooks();
}
```

### Performance Impact:

| Scenario            | Before       | After      |
| ------------------- | ------------ | ---------- |
| Gõ "Harry Potter"   | 12 API calls | 1 API call |
| User gõ nhầm và xóa | 10+ calls    | 1 call     |
| Server load         | High         | Low        |

---

## 3. 📷 Continuous Scanner (Quét QR liên tục)

### Vấn đề cũ:

- Quét 1 QR → Tắt camera ngay lập tức
- Phải mở lại camera cho mỗi cuốn sách
- **Không phù hợp** với quy trình thủ thư

### Giải pháp:

- Scanner **không tắt** sau khi quét
- Tự động thêm sách vào giỏ
- Phát âm thanh **"Bíp"** để xác nhận

### Implementation:

```typescript
onCodeResult(resultString: string) {
  const bookId = Number(resultString);

  this.booksService.getBookById(bookId).subscribe({
    next: (book) => {
      if (book) {
        this.playBeep('success');        // Âm thanh "Bíp"
        this.addToCart(book);            // Tự động thêm vào giỏ
        this.toastr.success(`✓ ${book.name}`);
      }
    },
    error: () => this.playBeep('error')  // Âm "Bíp" lỗi
  });
  // ❌ KHÔNG còn: this.enableScanner = false;
}

// Phát âm thanh bằng Web Audio API
private playBeep(type: 'success' | 'error'): void {
  const audioContext = new AudioContext();
  const oscillator = audioContext.createOscillator();

  if (type === 'success') {
    oscillator.frequency.value = 800;  // Nốt cao
    oscillator.type = 'sine';
  } else {
    oscillator.frequency.value = 400;  // Nốt thấp
    oscillator.type = 'square';
  }

  oscillator.start(audioContext.currentTime);
  oscillator.stop(audioContext.currentTime + 0.1);
}
```

### Quy trình mới:

1. Thủ thư mở Scanner **1 lần**
2. Quét liên tục 5-10 cuốn sách
3. Mỗi cuốn → "Bíp" → Tự động vào giỏ
4. Click "Mượn tất cả" → Xong!

### UI Update:

```html
<p class="scanner-status">
  <i class="fa-solid fa-qrcode"></i> Quét liên tục - Đã thêm:
  <span class="badge bg-warning">{{ cartCount }}</span>
</p>
```

---

## 4. 💎 Skeleton Loading + CSS Variables

### A. Skeleton Loading

#### Vấn đề cũ:

- Hiển thị **spinner tròn** đơn điệu
- Không thể hiện cấu trúc của nội dung sắp tải

#### Giải pháp:

- Hiển thị **khung xương** (skeleton) giống hệt thẻ sách
- Hiệu ứng shimmer animation

#### Implementation:

```typescript
// Component
skeletonArray = Array(8).fill(0); // 8 skeleton cards
```

```html
<!-- Skeleton cards thay vì spinner -->
<div *ngIf="isLoadingPage" class="row g-4">
  <div class="col-md-3" *ngFor="let item of skeletonArray">
    <div class="card h-100 book-card">
      <div class="skeleton skeleton-cover" style="height: 300px"></div>
      <div class="card-body">
        <div class="skeleton skeleton-title mb-2"></div>
        <div class="skeleton skeleton-text mb-3"></div>
        <div class="skeleton skeleton-button"></div>
      </div>
    </div>
  </div>
</div>
```

```css
/* Animation shimmer */
.skeleton {
  background: linear-gradient(90deg, #161b22 0%, #21262d 50%, #161b22 100%);
  background-size: 200% 100%;
  animation: loading 1.5s ease-in-out infinite;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
```

### B. CSS Variables

#### Vấn đề cũ:

- Hardcode màu: `#161b22`, `#0d1117`, `#30363d`
- Muốn làm Light Mode → Phải sửa **hàng chục chỗ**

#### Giải pháp:

- Đưa màu ra **CSS Variables**
- Dễ dàng switch theme sau này

#### Implementation:

```css
/* Define variables */
:root {
  --bg-card: #161b22;
  --bg-modal: #0d1117;
  --bg-input: #21262d;
  --border-color: #30363d;
  --text-primary: #c9d1d9;
  --text-secondary: #8b949e;
  --text-link: #58a6ff;
  --text-link-hover: #79c0ff;
  --shadow-hover: rgba(0, 0, 0, 0.5);
  --focus-ring: rgba(88, 166, 255, 0.2);
}

/* Use variables */
.book-card {
  background-color: var(--bg-card);
  border: 1px solid var(--border-color);
}

.book-card:hover {
  box-shadow: 0 10px 30px var(--shadow-hover);
  border-color: var(--text-link);
}
```

#### Tương lai: Light Mode

```css
/* Thêm vào styles.css hoặc theme service */
[data-theme="light"] {
  --bg-card: #ffffff;
  --bg-modal: #f6f8fa;
  --bg-input: #ffffff;
  --border-color: #d0d7de;
  --text-primary: #24292f;
  --text-secondary: #57606a;
  /* ... */
}
```

---

## 📊 So sánh Before/After

| Tính năng                | Before                 | After                |
| ------------------------ | ---------------------- | -------------------- |
| **Mượn nhiều sách**      | Mỗi lần 1 cuốn         | Giỏ sách hàng loạt   |
| **API calls khi search** | 12 calls (gõ 12 ký tự) | 1 call (debounce)    |
| **Scanner workflow**     | Tắt sau mỗi QR         | Quét liên tục + beep |
| **Loading state**        | Spinner đơn điệu       | Skeleton hiện đại    |
| **Theme switching**      | Sửa 50+ chỗ            | Đổi biến CSS         |
| **UX Score**             | ⭐⭐⭐                 | ⭐⭐⭐⭐⭐           |

---

## 🧪 Testing Checklist

### Test Cart:

- [ ] Thêm 3 sách vào giỏ → Badge hiển thị số 3
- [ ] Click button giỏ → Modal hiện danh sách
- [ ] Click "Xóa tất cả" → Giỏ trống
- [ ] Click "Mượn tất cả" → API call hàng loạt
- [ ] Toast notification hiển thị "Đã mượn 3 cuốn"

### Test Debounce:

- [ ] Gõ "Har" → Đợi 500ms → API call
- [ ] Gõ tiếp "ry" nhanh → Chỉ 1 API call (không phải 2)
- [ ] Dropdown thể loại → Instant (không debounce)

### Test Scanner:

- [ ] Mở scanner → Camera bật
- [ ] Quét QR cuốn 1 → "Bíp" → Thêm vào giỏ → Camera VẪN BẬT
- [ ] Quét tiếp cuốn 2 → "Bíp" → Badge tăng lên 2
- [ ] Quét QR lỗi → Âm "bíp" khác + Toast error

### Test Skeleton:

- [ ] Reload trang → Hiện 8 skeleton cards
- [ ] Skeleton có animation shimmer
- [ ] Sau ~1s → Skeleton biến mất → Hiện sách thật

### Test CSS Variables:

- [ ] Inspect element → Check `var(--bg-card)` đang dùng
- [ ] Thử đổi biến trong DevTools → Toàn bộ theme đổi

---

## 🎨 UI Screenshots (Conceptual)

### Cart Modal:

```
┌─────────────────────────────────────┐
│ 🛒 Giỏ Sách (3)              [X]   │
├─────────────────────────────────────┤
│ [1] [Thumbnail] Harry Potter       │
│     J.K. Rowling · Còn 5    [Trash]│
│                                     │
│ [2] [Thumbnail] Clean Code         │
│     Robert Martin · Còn 3   [Trash]│
│                                     │
│ [3] [Thumbnail] Design Patterns    │
│     Gang of Four · Còn 2    [Trash]│
├─────────────────────────────────────┤
│ Thông tin người mượn:              │
│ Họ tên: [Nguyễn Văn A]            │
│ Lớp:    [9A1]                      │
│ Thời hạn: [14 ngày ▼]             │
├─────────────────────────────────────┤
│ [Xóa tất cả] [Đóng] [Mượn tất cả] │
└─────────────────────────────────────┘
```

### Skeleton Loading:

```
┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐
│░░░░░░░│ │░░░░░░░│ │░░░░░░░│ │░░░░░░░│
│░░░░░░░│ │░░░░░░░│ │░░░░░░░│ │░░░░░░░│  ← Shimmer animation
│░░░░░░░│ │░░░░░░░│ │░░░░░░░│ │░░░░░░░│
│       │ │       │ │       │ │       │
│░░░ ░░░│ │░░░ ░░░│ │░░░ ░░░│ │░░░ ░░░│  ← Title
│░░ ░░  │ │░░ ░░  │ │░░ ░░  │ │░░ ░░  │  ← Author
│░░░░░░░│ │░░░░░░░│ │░░░░░░░│ │░░░░░░░│  ← Button
└───────┘ └───────┘ └───────┘ └───────┘
```

---

## 🚀 Triển khai

### Files đã thay đổi:

1. `borrow-book.component.ts` - Logic chính
2. `borrow-book.component.html` - UI templates
3. `borrow-book.component.css` - Styles + animations

### Không cần thay đổi:

- Backend APIs (vẫn dùng `CirculationService.loan()`)
- Database schema
- Other components

### Test ngay:

```bash
cd lms-frontend
npm start
# Mở http://localhost:4200/borrow-book
```

---

## 💡 Ý tưởng mở rộng

### Tính năng có thể thêm sau:

1. **LocalStorage Cart** - Lưu giỏ sách khi reload trang
2. **Cart Expiry** - Tự động xóa sách sau 30 phút
3. **Barcode Scanner** - Hỗ trợ quét mã vạch (không chỉ QR)
4. **Cart Sharing** - Chia sẻ giỏ sách với thủ thư khác
5. **Bulk Operations** - Export/Import giỏ sách từ CSV

### Backend improvements:

1. **Batch Borrow API** - Endpoint nhận array bookIds
2. **Transaction Log** - Lưu lịch sử mượn hàng loạt
3. **Notification** - Gửi email tổng hợp các sách đã mượn

---

## 📚 Resources

- **RxJS Debounce**: https://rxjs.dev/api/operators/debounceTime
- **Web Audio API**: https://developer.mozilla.org/en-US/docs/Web/API/Web_Audio_API
- **CSS Variables**: https://developer.mozilla.org/en-US/docs/Web/CSS/Using_CSS_custom_properties
- **Skeleton Loading**: https://css-tricks.com/building-skeleton-screens-css-custom-properties/

---

## ✅ Completion Status

- [x] Borrow Cart (Giỏ sách)
- [x] Debounce Search
- [x] Continuous Scanner với beep
- [x] Skeleton Loading
- [x] CSS Variables
- [x] Documentation

**🎉 Nâng cấp hoàn tất! Component borrow-book giờ đã ở mức chuyên nghiệp!**
