import { Component, OnInit } from '@angular/core';
import { AdminService, MemberCard, Page } from '../../services/admin.service';

@Component({
  selector: 'app-member-cards',
  templateUrl: './member-cards.component.html',
  styleUrls: ['./member-cards.component.css'],
  standalone: false,
})
export class MemberCardsComponent implements OnInit {
  cardsPage?: Page<MemberCard>;
  isLoading = false;

  keyword = '';
  status = '';
  barcodeType = '';
  from = '';
  to = '';

  page = 0;
  size = 20;

  newUserId?: number;
  newBarcodeType: 'CODE128' | 'QR' = 'CODE128';
  newExpiredAt = '';
  newMetadata = '';

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadCards();
  }

  loadCards(): void {
    this.isLoading = true;
    this.adminService
      .searchMemberCards({
        keyword: this.keyword || undefined,
        status: this.status || undefined,
        barcodeType: this.barcodeType || undefined,
        from: this.from || undefined,
        to: this.to || undefined,
        page: this.page,
        size: this.size,
      })
      .subscribe({
        next: (page) => {
          this.cardsPage = page;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
        },
      });
  }

  applyFilters(): void {
    this.page = 0;
    this.loadCards();
  }

  resetFilters(): void {
    this.keyword = '';
    this.status = '';
    this.barcodeType = '';
    this.from = '';
    this.to = '';
    this.page = 0;
    this.loadCards();
  }

  nextPage(): void {
    if (!this.cardsPage || this.page + 1 >= this.cardsPage.totalPages) return;
    this.page += 1;
    this.loadCards();
  }

  prevPage(): void {
    if (this.page === 0) return;
    this.page -= 1;
    this.loadCards();
  }

  createCard(): void {
    if (!this.newUserId) return;
    this.adminService
      .createMemberCard({
        userId: this.newUserId,
        barcodeType: this.newBarcodeType,
        expiredAt: this.newExpiredAt || undefined,
        metadata: this.newMetadata || undefined,
      })
      .subscribe({
        next: () => {
          this.newUserId = undefined;
          this.newExpiredAt = '';
          this.newMetadata = '';
          this.loadCards();
        },
      });
  }

  revokeCard(card: MemberCard): void {
    const reason = window.prompt('Reason for revoke?') || undefined;
    this.adminService.revokeMemberCard(card.id, reason).subscribe({
      next: () => this.loadCards(),
    });
  }

  downloadPdf(card: MemberCard): void {
    this.adminService.downloadMemberCardPdf(card.id).subscribe((blob) => {
      this.downloadBlob(blob, `member-card-${card.id}.pdf`);
    });
  }

  downloadBarcode(card: MemberCard): void {
    this.adminService.downloadMemberCardBarcode(card.id).subscribe((blob) => {
      this.downloadBlob(blob, `member-card-${card.id}.png`);
    });
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
