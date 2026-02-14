import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { BorrowService } from '../services/borrow.service';
import { UserAuthService } from '../services/user-auth.service';
import { LoanDetails } from '../services/admin.service';
import { ToastrService } from 'ngx-toastr';
import { BooksService } from '../services/books.service';

interface BookCondition {
  value: string;
  label: string;
  damageFee: number;
  description: string;
  icon: string;
  examples: string;
  recommendation: string;
  whenToUse: string;
}

interface ReturnPreview extends LoanDetails {
  bookCoverUrl?: string;
  daysOverdue: number;
  overdueFine: number;
  damageFee: number;
  totalFine: number;
  isOverdue: boolean;
}

@Component({
  selector: 'app-return-book',
  templateUrl: './return-book.component.html',
  styleUrls: ['./return-book.component.css'],
  standalone: false,
})
export class ReturnBookComponent implements OnInit {
  borrows: LoanDetails[] = [];
  userId: number | null = null;
  loading = new Set<number>();

  // Preview Modal
  showPreviewModal = false;
  showFinalConfirmModal = false;
  returnPreview: ReturnPreview | null = null;

  // Book Condition
  selectedCondition: string = 'NORMAL';
  bookConditions: BookCondition[] = [
    {
      value: 'NORMAL',
      label: 'Bình thường',
      damageFee: 0,
      description: 'Sách nguyên vẹn, không hư hỏng',
      icon: 'fa-circle-check',
      examples: 'Không rách, không bẩn, không ghi chú lên sách',
      recommendation: 'Xử lý: Không thu phí phạt hư hỏng',
      whenToUse: 'Chọn khi sách còn nguyên trạng như lúc mượn',
    },
    {
      value: 'SLIGHTLY_DAMAGED',
      label: 'Hư hỏng nhẹ',
      damageFee: 50000,
      description: 'Rách nhẹ, gấp góc trang',
      icon: 'fa-triangle-exclamation',
      examples: 'Nhàu bìa, gấp góc, rách mép nhỏ, bẩn nhẹ',
      recommendation: 'Xử lý: Thu phí bảo trì, vẫn có thể cho mượn lại',
      whenToUse: 'Chọn khi chỉ ảnh hưởng nhẹ, vẫn đọc bình thường',
    },
    {
      value: 'DAMAGED',
      label: 'Hư hỏng nặng',
      damageFee: 100000,
      description: 'Rách nhiều trang, bẩn nặng',
      icon: 'fa-circle-xmark',
      examples: 'Rách nhiều trang, bung gáy, ẩm mốc, mất trang',
      recommendation: 'Xử lý: Thu phí cao, cân nhắc loại khỏi lưu thông',
      whenToUse: 'Chọn khi sách bị hư hại rõ, khó tiếp tục cho mượn',
    },
    {
      value: 'LOST',
      label: 'Mất sách',
      damageFee: 200000,
      description: 'Không tìm thấy sách (đền 200% giá trị)',
      icon: 'fa-book',
      examples: 'Không hoàn trả được bản sách đã mượn',
      recommendation: 'Xử lý: Đền bù theo chính sách thư viện',
      whenToUse: 'Chọn khi người mượn không thể nộp lại bản sách',
    },
  ];

  // Fine Payment
  finePaymentMethod: string = 'CASH'; // CASH or DEBT

  // Multi-select return
  selectedLoans = new Set<number>();

  // Search by user
  searchMode: 'loan' | 'user' = 'loan';
  searchQuery = '';
  searchResults: LoanDetails[] = [];
  isSearching = false;

  errorMessage = '';
  successMessage = '';

  // Fine calculation settings (TODO: load from backend)
  finePerDay = 5000; // 5,000 VND per day
  maxFine = 200000; // Max 200,000 VND

  constructor(
    private borrowService: BorrowService,
    private booksService: BooksService,
    private userAuthService: UserAuthService,
    private router: Router,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.userId = this.userAuthService.getUserId();

    if (!this.userId) {
      this.router.navigate(['/login'], {
        queryParams: { returnUrl: '/return-book' },
      });
      return;
    }
    this.loadUserBorrows();
  }

  private loadUserBorrows() {
    this.borrowService.getMyLoanHistory().subscribe({
      next: (data: LoanDetails[]) => {
        this.borrows = (data || []).filter((b) => b.status !== 'RETURNED');
      },
      error: (err: HttpErrorResponse) => {
        console.error('[ReturnBook] getMyLoanHistory error:', err);
        this.errorMessage = 'Could not retrieve your borrowed books list.';
      },
    });
  }

  // Search by Loan ID or User
  searchLoan(): void {
    if (!this.searchQuery.trim()) {
      this.toastr.warning('Vui lòng nhập ID phiếu mượn hoặc tên người dùng');
      return;
    }

    this.isSearching = true;
    this.searchResults = [];

    if (this.searchMode === 'loan') {
      // Search by loan ID
      const loanId = Number(this.searchQuery);
      if (isNaN(loanId)) {
        this.toastr.error('ID phiếu mượn phải là số');
        this.isSearching = false;
        return;
      }

      // Find in current borrows list
      const loan = this.borrows.find((b) => b.loanId === loanId);
      if (loan) {
        this.openPreviewModal(loan);
      } else {
        this.toastr.error('Không tìm thấy phiếu mượn này hoặc đã được trả');
      }
      this.isSearching = false;
    } else {
      // Search by user name (TODO: call API to search)
      // For now, filter from current list
      const query = this.searchQuery.toLowerCase();
      this.searchResults = this.borrows.filter((b) =>
        b.userName?.toLowerCase().includes(query),
      );

      if (this.searchResults.length === 0) {
        this.toastr.info(
          'Không tìm thấy sách nào đang mượn của người dùng này',
        );
      }
      this.isSearching = false;
    }
  }

  // Calculate fine for a loan
  calculateFine(loan: LoanDetails): ReturnPreview {
    const now = new Date();
    const dueDateValue = this.getDueDateValue(loan);
    const dueDate = dueDateValue ? new Date(dueDateValue) : now;
    const daysOverdue = Math.max(
      0,
      Math.ceil((now.getTime() - dueDate.getTime()) / (1000 * 60 * 60 * 24)),
    );

    const overdueFine = Math.min(daysOverdue * this.finePerDay, this.maxFine);
    const condition = this.bookConditions.find(
      (c) => c.value === this.selectedCondition,
    );
    const damageFee = condition?.damageFee || 0;
    const totalFine = overdueFine + damageFee;

    return {
      ...loan,
      daysOverdue,
      overdueFine,
      damageFee,
      totalFine,
      isOverdue: daysOverdue > 0,
    };
  }

  openPreviewModal(loan: LoanDetails): void {
    this.selectedCondition = 'NORMAL';
    this.finePaymentMethod = 'CASH';
    this.returnPreview = this.calculateFine(loan);
    this.showPreviewModal = true;

    if (loan.bookId) {
      this.booksService.getBookById(loan.bookId).subscribe({
        next: (book) => {
          if (
            !this.returnPreview ||
            this.returnPreview.loanId !== loan.loanId
          ) {
            return;
          }

          this.returnPreview = {
            ...this.returnPreview,
            bookName: this.returnPreview.bookName || book.name,
            bookCoverUrl: book.coverUrl || this.returnPreview.bookCoverUrl,
          } as ReturnPreview;
        },
        error: () => {
          // Keep fallback image and existing data when detail API fails
        },
      });
    }
  }

  closePreviewModal(): void {
    this.showPreviewModal = false;
    this.showFinalConfirmModal = false;
    this.returnPreview = null;
    this.selectedCondition = 'NORMAL';
  }

  requestReturnConfirmation(): void {
    if (!this.returnPreview || !this.returnPreview.loanId) {
      this.toastr.error('Không tìm thấy thông tin phiếu mượn');
      return;
    }

    this.showFinalConfirmModal = true;
    this.toastr.info('Vui lòng xác nhận lần cuối để hoàn tất trả sách');
  }

  closeFinalConfirmModal(): void {
    this.showFinalConfirmModal = false;
  }

  onConditionChange(): void {
    if (this.returnPreview) {
      this.returnPreview = this.calculateFine(this.returnPreview);
    }
  }

  getConditionBadgeClass(value: string): string {
    switch (value) {
      case 'NORMAL':
        return 'badge-success';
      case 'SLIGHTLY_DAMAGED':
        return 'badge-warning';
      case 'DAMAGED':
        return 'badge-danger';
      case 'LOST':
        return 'badge-dark';
      default:
        return 'badge-secondary';
    }
  }

  getConditionLabel(value: string): string {
    return (
      this.bookConditions.find((condition) => condition.value === value)
        ?.label || 'Không xác định'
    );
  }

  // Multi-select
  toggleLoanSelection(loanId: number): void {
    if (this.selectedLoans.has(loanId)) {
      this.selectedLoans.delete(loanId);
    } else {
      this.selectedLoans.add(loanId);
    }
  }

  selectAll(): void {
    this.searchResults.forEach((loan) => {
      if (loan.loanId) this.selectedLoans.add(loan.loanId);
    });
  }

  deselectAll(): void {
    this.selectedLoans.clear();
  }

  returnMultiple(): void {
    if (this.selectedLoans.size === 0) {
      this.toastr.warning('Vui lòng chọn ít nhất một cuốn sách để trả');
      return;
    }

    const count = this.selectedLoans.size;
    if (!confirm(`Xác nhận trả ${count} cuốn sách đã chọn?`)) {
      return;
    }

    const selectedIds = Array.from(this.selectedLoans);
    let successCount = 0;
    let errorCount = 0;

    selectedIds.forEach((loanId, index) => {
      this.borrowService.returnLoan(loanId).subscribe({
        next: () => {
          successCount++;
          if (index === selectedIds.length - 1) {
            this.toastr.success(
              `Trả thành công ${successCount}/${count} cuốn sách`,
            );
            this.selectedLoans.clear();
            this.searchResults = [];
            this.searchQuery = '';
            this.loadUserBorrows();
          }
        },
        error: () => {
          errorCount++;
          if (index === selectedIds.length - 1) {
            this.toastr.error(`Lỗi khi trả ${errorCount}/${count} cuốn sách`);
          }
        },
      });
    });
  }

  confirmReturn(): void {
    if (!this.returnPreview || !this.returnPreview.loanId) {
      this.toastr.error('Không tìm thấy thông tin phiếu mượn');
      return;
    }

    const preview = this.returnPreview;
    const confirmedCondition = this.selectedCondition;
    const confirmedPaymentMethod = this.finePaymentMethod;
    this.showFinalConfirmModal = false;
    this.showPreviewModal = false;
    this.returnPreview = null;
    this.selectedCondition = 'NORMAL';

    this.toastr.info('Đang xử lý trả sách...');
    this.loading.add(preview.loanId);

    // Build payload with condition and fine info
    const payload: any = {
      loanId: preview.loanId,
      condition: confirmedCondition,
      overdueFine: preview.overdueFine,
      damageFee: preview.damageFee,
      totalFine: preview.totalFine,
      finePaymentMethod: confirmedPaymentMethod,
    };

    this.borrowService.returnLoan(preview.loanId).subscribe({
      next: () => {
        if (preview.totalFine > 0) {
          const fineMsg =
            confirmedPaymentMethod === 'CASH'
              ? `Đã thu ${preview.totalFine.toLocaleString('vi-VN')}đ tiền phạt`
              : `Ghi nợ ${preview.totalFine.toLocaleString('vi-VN')}đ vào tài khoản`;
          this.toastr.success(`Trả sách thành công! ${fineMsg}`);
        } else {
          this.toastr.success(`Trả sách "${preview.bookName}" thành công!`);
        }
        this.loadUserBorrows();
        this.searchResults = [];
        this.searchQuery = '';
      },
      error: (err: HttpErrorResponse) => {
        this.toastr.error(err.error?.message || 'Lỗi khi trả sách');
      },
      complete: () => {
        this.loading.delete(preview.loanId!);
      },
    });
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  getBorrowerDisplay(loan: LoanDetails | ReturnPreview): string {
    const anyLoan = loan as any;
    return (
      anyLoan.borrowerName ||
      anyLoan.borrowerUsername ||
      anyLoan.userName ||
      'Chưa có thông tin'
    );
  }

  getDueDateValue(loan: LoanDetails | ReturnPreview): string | undefined {
    const anyLoan = loan as any;
    return anyLoan.dueDate || anyLoan.returnDate;
  }

  getBookCoverUrl(loan: LoanDetails | ReturnPreview): string {
    const anyLoan = loan as any;
    return (
      anyLoan.bookCoverUrl ||
      anyLoan.coverUrl ||
      anyLoan.bookImageUrl ||
      'assets/images/placeholder.png'
    );
  }

  formatCurrency(amount: number): string {
    return amount.toLocaleString('vi-VN') + 'đ';
  }
}
