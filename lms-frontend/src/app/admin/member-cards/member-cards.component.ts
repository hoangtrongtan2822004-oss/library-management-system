import { Component, OnInit } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import {
  AdminService,
  MemberCard,
  Page,
} from 'src/app/services/admin.service';

@Component({
  selector: 'app-member-cards',
  templateUrl: './member-cards.component.html',
  styleUrls: ['./member-cards.component.css'],
  standalone: false,
})
export class MemberCardsComponent implements OnInit {
  cards: MemberCard[] = [];
  loading = false;
  errorMessage = '';

  // Filters
  keyword = '';
  statusFilter = '';
  barcodeTypeFilter = '';
  from = '';
  to = '';

  // Pagination
  page = 0;
  size = 10;
  totalPages = 0;
  totalElements = 0;

  // Form
  form = {
    id: undefined as number | undefined,
    userId: null as number | null,
    barcodeType: 'CODE128' as 'CODE128' | 'QR',
    expiredAt: '',
    metadata: '',
  };

  constructor(
    private adminService: AdminService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.loadCards();
  }

  loadCards(): void {
    this.loading = true;
    this.errorMessage = '';
    this.adminService
      .searchMemberCards({
        keyword: this.keyword.trim() || undefined,
        status: this.statusFilter || undefined,
        barcodeType: this.barcodeTypeFilter || undefined,
        from: this.from || undefined,
        to: this.to || undefined,
        page: this.page,
        size: this.size,
      })
      .subscribe({
        next: (res: Page<MemberCard>) => {
          this.cards = res.content;
          this.totalPages = res.totalPages;
          this.totalElements = res.totalElements;
        },
        error: (err) => {
          this.errorMessage = 'Không tải được danh sách thẻ';
          this.toastr.error(this.errorMessage);
          console.error(err);
        },
        complete: () => (this.loading = false),
      });
  }

  applyFilters(): void {
    this.page = 0;
    this.loadCards();
  }

  resetFilters(): void {
    this.keyword = '';
    this.statusFilter = '';
    this.barcodeTypeFilter = '';
    this.from = '';
    this.to = '';
    this.page = 0;
    this.loadCards();
  }

  onPageChange(pageIndex: number): void {
    if (pageIndex < 0 || pageIndex >= this.totalPages) return;
    this.page = pageIndex;
    this.loadCards();
  }

  submitForm(): void {
    if (!this.form.userId) {
      this.toastr.warning('Vui lòng nhập userId');
      return;
    }

    const payload = {
      userId: this.form.userId,
      barcodeType: this.form.barcodeType,
      expiredAt: this.form.expiredAt || undefined,
      metadata: this.form.metadata || undefined,
    } as any;

    const action$ = this.form.id
      ? this.adminService.updateMemberCard(this.form.id, payload)
      : this.adminService.createMemberCard(payload);

    action$.subscribe({
      next: (card) => {
        this.toastr.success(this.form.id ? 'Đã cập nhật thẻ' : 'Đã tạo thẻ');
        this.resetForm();
        this.loadCards();
      },
      error: (err) => {
        this.toastr.error('Không lưu được thẻ');
        console.error(err);
      },
    });
  }

  editCard(card: MemberCard): void {
    this.form = {
      id: card.id,
      userId: card.userId,
      barcodeType: card.barcodeType,
      expiredAt: card.expiredAt ? card.expiredAt.slice(0, 16) : '',
      metadata: card.metadata || '',
    };
  }

  resetForm(): void {
    this.form = {
      id: undefined,
      userId: null,
      barcodeType: 'CODE128',
      expiredAt: '',
      metadata: '',
    };
  }

  revoke(card: MemberCard): void {
    if (!confirm('Thu hồi thẻ này?')) return;
    this.adminService.revokeMemberCard(card.id).subscribe({
      next: () => {
        this.toastr.success('Đã thu hồi thẻ');
        this.loadCards();
      },
      error: (err) => {
        this.toastr.error('Thu hồi thất bại');
        console.error(err);
      },
    });
  }

  downloadPdf(card: MemberCard): void {
    this.adminService.downloadMemberCardPdf(card.id).subscribe({
      next: (blob) => this.saveBlob(blob, `member-card-${card.cardNumber}.pdf`),
      error: (err) => {
        this.toastr.error('Không tải được PDF');
        console.error(err);
      },
    });
  }

  downloadPng(card: MemberCard): void {
    this.adminService.downloadMemberCardBarcode(card.id).subscribe({
      next: (blob) => this.saveBlob(blob, `member-card-${card.cardNumber}.png`),
      error: (err) => {
        this.toastr.error('Không tải được barcode');
        console.error(err);
      },
    });
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
