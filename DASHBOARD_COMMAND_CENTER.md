# Dashboard Command Center - Hướng dẫn Tính năng Mới

## Tổng quan

Dashboard đã được nâng cấp từ "Báo cáo tĩnh" thành **"Command Center"** thực thụ với 6 tính năng mới giúp Admin ra quyết định nhanh chóng và chính xác.

---

## 1. 📅 Date Range Filter (Bộ lọc Thời gian)

**Vị trí**: Góc trên phải, bên cạnh tiêu đề Dashboard

**Chức năng**: Lọc toàn bộ dữ liệu theo khoảng thời gian

- 🔹 **Hôm nay**: Hiển thị dữ liệu trong ngày
- 🔹 **Tuần này**: Dữ liệu 7 ngày gần nhất
- 🔹 **Tháng này**: Dữ liệu 30 ngày gần nhất (mặc định)
- 🔹 **Năm nay**: Dữ liệu từ đầu năm đến nay

**Cách sử dụng**:

1. Click vào dropdown góc phải
2. Chọn khoảng thời gian mong muốn
3. Toàn bộ biểu đồ và số liệu tự động reload

**Kỹ thuật**:

```typescript
// Component state
dateRange: 'today' | 'week' | 'month' | 'year' = 'month';

// Handler
onDateRangeChange(range: string) {
  this.dateRange = range;
  this.loadAllData(); // Reload all data with new filter
}
```

---

## 2. ⚡ Quick Actions (Thao tác Nhanh)

**Vị trí**: Hàng nút ngay dưới header

**3 nút tắt**:

1. **Tạo phiếu mượn nhanh** (Xanh) → `/admin/create-loan`
2. **Quét mã trả sách** (Xanh lá) → `/admin/scanner`
3. **Thêm user mới** (Xanh dương) → `/create-user`

**Lợi ích**:

- Giảm 3 lần click so với navigation thông thường
- Truy cập nhanh các tác vụ thường dùng nhất
- Tối ưu workflow cho Admin khi đang xem Dashboard

**Code**:

```typescript
quickCreateLoan() {
  window.location.href = '/admin/create-loan';
}
```

---

## 3. 🚨 Alert/Warning Cards (Thẻ Cảnh báo)

**Vị trí**: Hàng alert ngay trên biểu đồ

**3 loại cảnh báo tự động**:

### 3.1. Cảnh báo Sách Quá hạn (Đỏ)

- **Điều kiện**: `details.stats.overdueLoans > 0`
- **Thông tin**: Số phiếu mượn quá hạn
- **Hành động**: Click → Hiển thị bảng danh sách quá hạn

### 3.2. Cảnh báo Tiền Phạt (Vàng)

- **Điều kiện**: `details.stats.totalUnpaidFines > 0`
- **Thông tin**: Tổng tiền phạt chưa thu (VND)
- **Hành động**: Click → Hiển thị danh sách nợ tiền

### 3.3. Thông báo Sách HOT (Xanh)

- **Điều kiện**: Sách top 1 có `loanCount > 10`
- **Thông tin**: Tên sách + số lượt mượn
- **Hành động**: Không clickable (chỉ thông báo)

**Kỹ thuật**:

```html
<div
  class="alert alert-danger cursor-pointer"
  (click)="showSection('LOANS_OVERDUE')"
  *ngIf="details.stats.overdueLoans > 0"
>
  <!-- Content -->
</div>
```

---

## 4. 📊 Top 5 Sách Hot (Bar Chart)

**Vị trí**: Cột bên phải (thay thế 1/3 không gian chart cũ)

**Dữ liệu**: Lấy từ `details.mostLoanedBooks` (top 5 phần tử)

**Biểu đồ**:

- Type: **Horizontal Bar Chart** (Chart.js)
- Màu sắc: 5 màu khác nhau cho mỗi sách
- Trục X: Số lượt mượn
- Trục Y: Tên sách (cắt ngắn 20 ký tự)

**Lợi ích**:

- Admin biết nên nhập thêm sách gì
- Phát hiện xu hướng đọc của người dùng
- Quyết định chiến lược marketing sách

**Code**:

```typescript
renderTopBooksChart() {
  const topBooks = this.details.mostLoanedBooks.slice(0, 5);
  const labels = topBooks.map(b => b.bookName.substring(0, 20) + '...');
  const data = topBooks.map(b => b.loanCount);

  new Chart(ctx, {
    type: 'bar',
    data: { labels, datasets: [{ data, backgroundColor: ['#ffc107', ...] }] },
    options: { indexAxis: 'y' } // Horizontal
  });
}
```

---

## 5. ✨ Skeleton Loading (Hiệu ứng Shimmer)

**Vị trí**: Toàn bộ dashboard khi `isLoading = true`

**Thay thế**: Spinner tròn đơn giản → Skeleton với hiệu ứng shimmer

**Hiệu ứng gồm**:

- Skeleton Quick Actions (3 nút)
- Skeleton Alert Cards (3 cards)
- Skeleton Charts (3 biểu đồ)
- Skeleton Stat Cards (6 thẻ thống kê)

**CSS Animation**:

```css
.skeleton {
  animation: skeleton-loading 1s linear infinite alternate;
  background: linear-gradient(90deg, #2d3748 25%, #4a5568 50%, #2d3748 75%);
  background-size: 200% 100%;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
```

**Lợi ích**:

- Tạo cảm giác chuyên nghiệp
- Giảm "flash of empty content"
- User biết Dashboard đang load, không bối rối

---

## 6. 🎯 Enhanced Recent Activities (Nâng cấp Hoạt động)

**3 nâng cấp so với bảng cũ**:

### 6.1. Icon theo Loại Hoạt động

```typescript
getActivityIcon(activity: any): string {
  if (activity.type?.includes('LOAN')) return 'fa-hand-holding';
  if (activity.type?.includes('RETURN')) return 'fa-check-circle';
  if (activity.type?.includes('FINE')) return 'fa-coins';
  return 'fa-circle';
}
```

### 6.2. Màu sắc Status

```typescript
getActivityColor(activity: any): string {
  if (activity.type?.includes('LOAN')) return 'text-info';      // Xanh
  if (activity.type?.includes('RETURN')) return 'text-success'; // Xanh lá
  if (activity.type?.includes('FINE')) return 'text-danger';    // Đỏ
  return 'text-secondary';
}
```

### 6.3. Relative Time (Thời gian Tương đối)

```typescript
getRelativeTime(date: string): string {
  const diffMins = Math.floor((now - then) / 60000);

  if (diffMins < 1) return 'Vừa xong';
  if (diffMins < 60) return `${diffMins} phút trước`;
  if (diffHours < 24) return `${diffHours} giờ trước`;
  return `${diffDays} ngày trước`;
}
```

**HTML Usage**:

```html
<tr *ngFor="let activity of details.recentActivities">
  <td>
    <i
      class="fas {{ getActivityIcon(activity) }} {{ getActivityColor(activity) }}"
    ></i>
  </td>
  <td>{{ activity.description }}</td>
  <td>{{ getRelativeTime(activity.timestamp) }}</td>
</tr>
```

---

## Layout Mới

**Trước**: 8 + 4 columns (2 charts)

```
[ Loan Trend (8 cols) | Status Pie (4 cols) ]
```

**Sau**: 5 + 4 + 3 columns (3 charts)

```
[ Loan Trend (5) | Status Pie (4) | Top 5 Books (3) ]
```

**Lợi ích**:

- Tận dụng 100% không gian màn hình
- Hiển thị nhiều thông tin hơn không bị chồng chéo
- Layout cân bằng hơn (5-4-3 thay vì 8-4)

---

## Checklist Kiểm tra

✅ Date Range Filter dropdown hoạt động
✅ Click Quick Actions chuyển đúng route
✅ Alert Cards hiển thị khi có dữ liệu
✅ Alert Cards clickable → show detail table
✅ Top 5 Books chart render với màu sắc
✅ Skeleton loading hiển thị khi isLoading = true
✅ Skeleton biến mất khi data loaded
✅ Recent Activities có icon + màu + relative time

---

## Testing

**Test case 1**: Date Range Filter

1. Chọn "Hôm nay" → Kiểm tra `console.log` có call API với filter mới
2. Chọn "Năm nay" → Số liệu thay đổi

**Test case 2**: Quick Actions

1. Click "Tạo phiếu mượn" → Chuyển đến `/admin/create-loan`
2. Click "Quét mã" → Chuyển đến `/admin/scanner`
3. Click "Thêm user" → Chuyển đến `/create-user`

**Test case 3**: Alert Cards

1. Nếu có sách quá hạn → Card đỏ xuất hiện
2. Click vào card đỏ → Bảng "Danh sách quá hạn" hiển thị
3. Nếu có tiền phạt → Card vàng xuất hiện

**Test case 4**: Top 5 Chart

1. Mở Dashboard → Chart "Top 5 Sách Hot" hiển thị bên phải
2. Kiểm tra 5 sách có màu khác nhau
3. Kiểm tra tên sách không bị tràn (max 20 ký tự)

**Test case 5**: Skeleton Loading

1. Refresh Dashboard → Skeleton xuất hiện ngay lập tức
2. Sau 1-2 giây → Skeleton biến mất, data xuất hiện
3. Kiểm tra hiệu ứng shimmer (gradient chạy ngang)

---

## Tổng kết

Dashboard đã được nâng cấp từ **Static Dashboard** (chỉ xem) thành **Command Center** (tương tác + hành động):

| Trước                    | Sau                            |
| ------------------------ | ------------------------------ |
| Chỉ xem số liệu          | Lọc theo thời gian             |
| 2 biểu đồ cố định        | 3 biểu đồ (thêm Top 5)         |
| Không có cảnh báo        | 3 loại alert tự động           |
| Phải navigate thủ công   | Quick Actions 1-click          |
| Spinner đơn giản         | Skeleton loading chuyên nghiệp |
| Bảng activities đơn điệu | Icon + màu + relative time     |

**Kết quả**: Admin có thể:

- ✅ Ra quyết định nhanh hơn (nhờ alerts)
- ✅ Thao tác nhanh hơn (nhờ quick actions)
- ✅ Hiểu dữ liệu sâu hơn (nhờ top books chart)
- ✅ Trải nghiệm mượt mà hơn (nhờ skeleton loading)

---

**Ngày tạo**: 18/01/2026  
**Phiên bản**: 2.0 (Command Center)  
**Tác giả**: GitHub Copilot + User
