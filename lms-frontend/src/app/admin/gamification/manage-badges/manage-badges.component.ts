import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GamificationAdminService, Badge } from '../gamification-admin.service';

@Component({
  selector: 'app-manage-badges',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="manage-badges">
      <div class="header">
        <h2><i class="fas fa-medal"></i> Quản Lý Huy Hiệu</h2>
        <button class="btn btn-primary" (click)="openCreateModal()">
          <i class="fas fa-plus"></i> Tạo Huy Hiệu Mới
        </button>
      </div>

      <div class="badges-grid" *ngIf="badges.length > 0; else noBadges">
        <div class="badge-card" *ngFor="let badge of badges">
          <div class="badge-icon">
            <img
              *ngIf="badge.iconUrl"
              [src]="badge.iconUrl"
              [alt]="badge.name"
            />
            <i *ngIf="!badge.iconUrl" class="fas fa-medal fa-3x"></i>
          </div>
          <div class="badge-info">
            <h3>{{ badge.name }}</h3>
            <p class="badge-desc">{{ badge.description }}</p>
            <div class="badge-stats">
              <span class="stat"
                ><i class="fas fa-star"></i> {{ badge.points }} điểm</span
              >
              <span class="stat"
                ><i class="fas fa-chart-line"></i> Yêu cầu:
                {{ badge.requirement }}</span
              >
            </div>
          </div>
          <div class="badge-actions">
            <button
              class="btn-icon"
              (click)="editBadge(badge)"
              title="Chỉnh sửa"
            >
              <i class="fas fa-edit"></i>
            </button>
            <button
              class="btn-icon"
              (click)="uploadIcon(badge)"
              title="Đổi icon"
            >
              <i class="fas fa-upload"></i>
            </button>
            <button
              class="btn-icon danger"
              (click)="deleteBadge(badge.id!)"
              title="Xóa"
            >
              <i class="fas fa-trash"></i>
            </button>
          </div>
        </div>
      </div>

      <ng-template #noBadges>
        <div class="empty-state">
          <i class="fas fa-medal fa-4x"></i>
          <p>Chưa có huy hiệu nào. Hãy tạo huy hiệu đầu tiên!</p>
        </div>
      </ng-template>

      <!-- Modal Create/Edit Badge -->
      <div class="modal" *ngIf="showModal" (click)="closeModal()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>{{ isEditMode ? 'Chỉnh Sửa' : 'Tạo Mới' }} Huy Hiệu</h3>
            <button class="btn-close" (click)="closeModal()">×</button>
          </div>
          <form (ngSubmit)="saveBadge()">
            <div class="form-group">
              <label>Tên Huy Hiệu *</label>
              <input
                type="text"
                [(ngModel)]="currentBadge.name"
                name="name"
                placeholder="VD: Mọt Sách, Người Dậy Sớm..."
                required
              />
            </div>
            <div class="form-group">
              <label>Mô Tả *</label>
              <textarea
                [(ngModel)]="currentBadge.description"
                name="description"
                placeholder="Mô tả điều kiện nhận huy hiệu..."
                rows="3"
                required
              ></textarea>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>Điểm Thưởng *</label>
                <input
                  type="number"
                  [(ngModel)]="currentBadge.points"
                  name="points"
                  min="0"
                  placeholder="VD: 100"
                  required
                />
              </div>
              <div class="form-group">
                <label>Yêu Cầu (số lượng) *</label>
                <input
                  type="number"
                  [(ngModel)]="currentBadge.requirement"
                  name="requirement"
                  min="1"
                  placeholder="VD: 10 cuốn sách"
                  required
                />
              </div>
            </div>
            <div class="form-group">
              <label>Tier (Cấp độ)</label>
              <select [(ngModel)]="currentBadge.tier" name="tier">
                <option value="BRONZE">Đồng</option>
                <option value="SILVER">Bạc</option>
                <option value="GOLD">Vàng</option>
                <option value="PLATINUM">Bạch Kim</option>
              </select>
            </div>
            <div class="modal-actions">
              <button
                type="button"
                class="btn btn-secondary"
                (click)="closeModal()"
              >
                Hủy
              </button>
              <button type="submit" class="btn btn-primary">
                <i class="fas fa-save"></i> Lưu
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Hidden file input for icon upload -->
      <input
        type="file"
        #fileInput
        (change)="onIconSelected($event)"
        accept="image/*"
        style="display: none"
      />
    </div>
  `,
  styles: [
    `
      .manage-badges {
        padding: 1rem;
      }

      .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 2rem;
      }

      .header h2 {
        color: #f0f6fc;
        font-size: 1.5rem;
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      .badges-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
        gap: 1.5rem;
      }

      .badge-card {
        background: #161b22;
        border: 1px solid #30363d;
        border-radius: 12px;
        padding: 1.5rem;
        display: flex;
        gap: 1rem;
        transition: all 0.3s;
      }

      .badge-card:hover {
        border-color: #58a6ff;
        box-shadow: 0 4px 12px rgba(88, 166, 255, 0.2);
      }

      .badge-icon {
        flex-shrink: 0;
        width: 80px;
        height: 80px;
        background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;
      }

      .badge-icon img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        border-radius: 50%;
      }

      .badge-info {
        flex: 1;
      }

      .badge-info h3 {
        color: #fbbf24;
        font-size: 1.2rem;
        margin-bottom: 0.5rem;
      }

      .badge-desc {
        color: #8b949e;
        font-size: 0.9rem;
        margin-bottom: 0.75rem;
      }

      .badge-stats {
        display: flex;
        gap: 1rem;
      }

      .stat {
        color: #58a6ff;
        font-size: 0.85rem;
        display: flex;
        align-items: center;
        gap: 0.3rem;
      }

      .badge-actions {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }

      .btn-icon {
        background: transparent;
        border: 1px solid #30363d;
        color: #f0f6fc;
        padding: 0.5rem;
        border-radius: 6px;
        cursor: pointer;
        transition: all 0.2s;
      }

      .btn-icon:hover {
        background: #1c2128;
        border-color: #58a6ff;
        color: #58a6ff;
      }

      .btn-icon.danger:hover {
        border-color: #f85149;
        color: #f85149;
      }

      .empty-state {
        text-align: center;
        padding: 4rem 2rem;
        color: #8b949e;
      }

      .empty-state i {
        color: #30363d;
        margin-bottom: 1rem;
      }

      /* Modal Styles */
      .modal {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.8);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 1000;
      }

      .modal-content {
        background: #0d1117;
        border: 1px solid #30363d;
        border-radius: 12px;
        width: 90%;
        max-width: 600px;
        max-height: 90vh;
        overflow-y: auto;
      }

      .modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1.5rem;
        border-bottom: 1px solid #30363d;
      }

      .modal-header h3 {
        color: #f0f6fc;
        margin: 0;
      }

      .btn-close {
        background: transparent;
        border: none;
        color: #8b949e;
        font-size: 2rem;
        cursor: pointer;
        line-height: 1;
        padding: 0;
      }

      .btn-close:hover {
        color: #f0f6fc;
      }

      form {
        padding: 1.5rem;
      }

      .form-group {
        margin-bottom: 1.5rem;
      }

      .form-group label {
        display: block;
        color: #f0f6fc;
        margin-bottom: 0.5rem;
        font-weight: 500;
      }

      .form-group input,
      .form-group textarea,
      .form-group select {
        width: 100%;
        padding: 0.75rem;
        background: #161b22;
        border: 1px solid #30363d;
        border-radius: 6px;
        color: #f0f6fc;
        font-size: 1rem;
      }

      .form-group input:focus,
      .form-group textarea:focus,
      .form-group select:focus {
        outline: none;
        border-color: #58a6ff;
      }

      .form-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1rem;
      }

      .modal-actions {
        display: flex;
        gap: 1rem;
        justify-content: flex-end;
        padding-top: 1rem;
        border-top: 1px solid #30363d;
      }

      .btn {
        padding: 0.75rem 1.5rem;
        border: none;
        border-radius: 6px;
        font-weight: 600;
        cursor: pointer;
        display: flex;
        align-items: center;
        gap: 0.5rem;
        transition: all 0.2s;
      }

      .btn-primary {
        background: #238636;
        color: white;
      }

      .btn-primary:hover {
        background: #2ea043;
      }

      .btn-secondary {
        background: #21262d;
        color: #f0f6fc;
      }

      .btn-secondary:hover {
        background: #30363d;
      }
    `,
  ],
})
export class ManageBadgesComponent implements OnInit {
  badges: Badge[] = [];
  showModal = false;
  isEditMode = false;
  currentBadge: Badge = this.getEmptyBadge();
  selectedBadgeForIcon: Badge | null = null;

  constructor(private gamificationService: GamificationAdminService) {}

  ngOnInit(): void {
    this.loadBadges();
  }

  loadBadges(): void {
    this.gamificationService.getAllBadges().subscribe({
      next: (badges) => {
        this.badges = badges;
      },
      error: (error) => {
        console.error('Error loading badges:', error);
        alert('Không thể tải danh sách huy hiệu');
      },
    });
  }

  openCreateModal(): void {
    this.isEditMode = false;
    this.currentBadge = this.getEmptyBadge();
    this.showModal = true;
  }

  editBadge(badge: Badge): void {
    this.isEditMode = true;
    this.currentBadge = { ...badge };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.currentBadge = this.getEmptyBadge();
  }

  saveBadge(): void {
    const request =
      this.isEditMode && this.currentBadge.id
        ? this.gamificationService.updateBadge(
            this.currentBadge.id,
            this.currentBadge,
          )
        : this.gamificationService.createBadge(this.currentBadge);

    request.subscribe({
      next: () => {
        alert(
          this.isEditMode
            ? 'Cập nhật huy hiệu thành công!'
            : 'Tạo huy hiệu mới thành công!',
        );
        this.loadBadges();
        this.closeModal();
      },
      error: (error) => {
        console.error('Error saving badge:', error);
        alert('Có lỗi xảy ra khi lưu huy hiệu');
      },
    });
  }

  deleteBadge(id: number): void {
    if (confirm('Bạn có chắc muốn xóa huy hiệu này?')) {
      this.gamificationService.deleteBadge(id).subscribe({
        next: () => {
          alert('Xóa huy hiệu thành công!');
          this.loadBadges();
        },
        error: (error) => {
          console.error('Error deleting badge:', error);
          alert('Không thể xóa huy hiệu này');
        },
      });
    }
  }

  uploadIcon(badge: Badge): void {
    this.selectedBadgeForIcon = badge;
    const fileInput = document.querySelector(
      'input[type="file"]',
    ) as HTMLInputElement;
    fileInput?.click();
  }

  onIconSelected(event: any): void {
    const file = event.target.files[0];
    if (file && this.selectedBadgeForIcon?.id) {
      this.gamificationService
        .uploadBadgeIcon(this.selectedBadgeForIcon.id, file)
        .subscribe({
          next: () => {
            alert('Upload icon thành công!');
            this.loadBadges();
          },
          error: (error) => {
            console.error('Error uploading icon:', error);
            alert('Không thể upload icon');
          },
        });
    }
  }

  private getEmptyBadge(): Badge {
    return {
      name: '',
      description: '',
      points: 0,
      requirement: 1,
      tier: 'BRONZE',
    };
  }
}
