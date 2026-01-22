import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  GamificationAdminService,
  RewardItem,
} from '../gamification-admin.service';

@Component({
  selector: 'app-manage-rewards',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="manage-rewards">
      <div class="header">
        <h2><i class="fas fa-gift"></i> Quản Lý Phần Thưởng</h2>
        <button class="btn btn-primary" (click)="openCreateModal()">
          <i class="fas fa-plus"></i> Tạo Phần Thưởng Mới
        </button>
      </div>

      <div class="rewards-grid" *ngIf="rewards.length > 0; else noRewards">
        <div
          class="reward-card"
          *ngFor="let reward of rewards"
          [class.out-of-stock]="reward.stock === 0"
          [class.inactive]="!reward.active"
        >
          <div class="reward-image">
            <img
              *ngIf="reward.imageUrl"
              [src]="reward.imageUrl"
              [alt]="reward.name"
            />
            <i *ngIf="!reward.imageUrl" class="fas fa-gift fa-3x"></i>
          </div>
          <div class="reward-info">
            <div class="reward-header">
              <h3>{{ reward.name }}</h3>
              <span
                class="stock-badge"
                [class.low]="reward.stock < 5"
                [class.out]="reward.stock === 0"
              >
                <i class="fas fa-box"></i> {{ reward.stock }}
              </span>
            </div>
            <p class="reward-desc">{{ reward.description }}</p>
            <div class="reward-footer">
              <div class="price">
                <i class="fas fa-coins"></i>
                <span>{{ reward.pointsCost }} điểm</span>
              </div>
              <div class="status">
                <span class="status-badge" [class.active]="reward.active">
                  {{ reward.active ? 'Đang bán' : 'Tạm dừng' }}
                </span>
              </div>
            </div>
          </div>
          <div class="reward-actions">
            <button
              class="btn-icon"
              (click)="adjustStock(reward)"
              title="Điều chỉnh tồn kho"
            >
              <i class="fas fa-warehouse"></i>
            </button>
            <button
              class="btn-icon"
              (click)="editReward(reward)"
              title="Chỉnh sửa"
            >
              <i class="fas fa-edit"></i>
            </button>
            <button
              class="btn-icon danger"
              (click)="deleteReward(reward.id!)"
              title="Xóa"
            >
              <i class="fas fa-trash"></i>
            </button>
          </div>
        </div>
      </div>

      <ng-template #noRewards>
        <div class="empty-state">
          <i class="fas fa-gift fa-4x"></i>
          <p>Chưa có phần thưởng nào. Hãy tạo phần thưởng đầu tiên!</p>
        </div>
      </ng-template>

      <!-- Modal Create/Edit Reward -->
      <div class="modal" *ngIf="showModal" (click)="closeModal()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>{{ isEditMode ? 'Chỉnh Sửa' : 'Tạo Mới' }} Phần Thưởng</h3>
            <button class="btn-close" (click)="closeModal()">×</button>
          </div>
          <form (ngSubmit)="saveReward()">
            <div class="form-group">
              <label>Tên Phần Thưởng *</label>
              <input
                type="text"
                [(ngModel)]="currentReward.name"
                name="name"
                placeholder="VD: Balo học sinh, Bút bi xanh..."
                required
              />
            </div>
            <div class="form-group">
              <label>Mô Tả *</label>
              <textarea
                [(ngModel)]="currentReward.description"
                name="description"
                placeholder="Mô tả chi tiết phần thưởng..."
                rows="3"
                required
              ></textarea>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>Giá (Điểm) *</label>
                <input
                  type="number"
                  [(ngModel)]="currentReward.pointsCost"
                  name="pointsCost"
                  min="0"
                  placeholder="VD: 1000"
                  required
                />
              </div>
              <div class="form-group">
                <label>Số Lượng Tồn Kho *</label>
                <input
                  type="number"
                  [(ngModel)]="currentReward.stock"
                  name="stock"
                  min="0"
                  placeholder="VD: 20"
                  required
                />
              </div>
            </div>
            <div class="form-group">
              <label>URL Hình Ảnh</label>
              <input
                type="url"
                [(ngModel)]="currentReward.imageUrl"
                name="imageUrl"
                placeholder="https://example.com/image.jpg"
              />
            </div>
            <div class="form-group">
              <label class="checkbox-label">
                <input
                  type="checkbox"
                  [(ngModel)]="currentReward.active"
                  name="active"
                />
                <span>Kích hoạt (Cho phép đổi quà)</span>
              </label>
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

      <!-- Modal Adjust Stock -->
      <div class="modal" *ngIf="showStockModal" (click)="closeStockModal()">
        <div class="modal-content small" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>Điều Chỉnh Tồn Kho</h3>
            <button class="btn-close" (click)="closeStockModal()">×</button>
          </div>
          <div class="stock-form">
            <p class="current-stock">
              <strong>{{ selectedReward?.name }}</strong
              ><br />
              Tồn kho hiện tại:
              <span class="highlight">{{ selectedReward?.stock }}</span>
            </p>
            <div class="form-group">
              <label>Số Lượng Mới *</label>
              <input
                type="number"
                [(ngModel)]="newStock"
                name="newStock"
                min="0"
                placeholder="Nhập số lượng mới"
              />
            </div>
            <div class="modal-actions">
              <button
                type="button"
                class="btn btn-secondary"
                (click)="closeStockModal()"
              >
                Hủy
              </button>
              <button
                type="button"
                class="btn btn-primary"
                (click)="updateStock()"
              >
                <i class="fas fa-check"></i> Cập Nhật
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .manage-rewards {
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

      .rewards-grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
        gap: 1.5rem;
      }

      .reward-card {
        background: #161b22;
        border: 1px solid #30363d;
        border-radius: 12px;
        padding: 1.5rem;
        transition: all 0.3s;
        position: relative;
      }

      .reward-card.out-of-stock {
        opacity: 0.5;
      }

      .reward-card.inactive {
        opacity: 0.7;
        border-color: #21262d;
      }

      .reward-card:hover {
        border-color: #58a6ff;
        box-shadow: 0 4px 12px rgba(88, 166, 255, 0.2);
      }

      .reward-image {
        width: 100%;
        height: 150px;
        background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-bottom: 1rem;
        overflow: hidden;
      }

      .reward-image img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .reward-image i {
        color: white;
      }

      .reward-info {
        flex: 1;
      }

      .reward-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 0.75rem;
      }

      .reward-header h3 {
        color: #a78bfa;
        font-size: 1.1rem;
        flex: 1;
      }

      .stock-badge {
        display: flex;
        align-items: center;
        gap: 0.3rem;
        padding: 0.25rem 0.75rem;
        border-radius: 12px;
        font-size: 0.85rem;
        font-weight: 600;
        background: #238636;
        color: #fff;
      }

      .stock-badge.low {
        background: #d29922;
      }

      .stock-badge.out {
        background: #f85149;
      }

      .reward-desc {
        color: #8b949e;
        font-size: 0.9rem;
        margin-bottom: 1rem;
        line-height: 1.5;
      }

      .reward-footer {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding-top: 1rem;
        border-top: 1px solid #30363d;
      }

      .price {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        color: #fbbf24;
        font-size: 1rem;
        font-weight: 600;
      }

      .price i {
        font-size: 1.2rem;
      }

      .status-badge {
        padding: 0.25rem 0.75rem;
        border-radius: 12px;
        font-size: 0.75rem;
        font-weight: 600;
        background: #21262d;
        color: #8b949e;
      }

      .status-badge.active {
        background: #238636;
        color: #fff;
      }

      .reward-actions {
        display: flex;
        gap: 0.5rem;
        margin-top: 1rem;
      }

      .btn-icon {
        flex: 1;
        background: transparent;
        border: 1px solid #30363d;
        color: #f0f6fc;
        padding: 0.5rem;
        border-radius: 6px;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
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

      .modal-content.small {
        max-width: 400px;
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

      form,
      .stock-form {
        padding: 1.5rem;
      }

      .current-stock {
        padding: 1rem;
        background: #161b22;
        border: 1px solid #30363d;
        border-radius: 8px;
        margin-bottom: 1.5rem;
        color: #f0f6fc;
      }

      .highlight {
        color: #fbbf24;
        font-size: 1.2rem;
        font-weight: 700;
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
      .form-group textarea {
        width: 100%;
        padding: 0.75rem;
        background: #161b22;
        border: 1px solid #30363d;
        border-radius: 6px;
        color: #f0f6fc;
        font-size: 1rem;
      }

      .form-group input:focus,
      .form-group textarea:focus {
        outline: none;
        border-color: #58a6ff;
      }

      .form-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 1rem;
      }

      .checkbox-label {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        cursor: pointer;
      }

      .checkbox-label input[type='checkbox'] {
        width: auto;
        cursor: pointer;
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
export class ManageRewardsComponent implements OnInit {
  rewards: RewardItem[] = [];
  showModal = false;
  showStockModal = false;
  isEditMode = false;
  currentReward: RewardItem = this.getEmptyReward();
  selectedReward: RewardItem | null = null;
  newStock: number = 0;

  constructor(private gamificationService: GamificationAdminService) {}

  ngOnInit(): void {
    this.loadRewards();
  }

  loadRewards(): void {
    this.gamificationService.getAllRewards().subscribe({
      next: (rewards) => {
        this.rewards = rewards;
      },
      error: (error) => {
        console.error('Error loading rewards:', error);
        alert('Không thể tải danh sách phần thưởng');
      },
    });
  }

  openCreateModal(): void {
    this.isEditMode = false;
    this.currentReward = this.getEmptyReward();
    this.showModal = true;
  }

  editReward(reward: RewardItem): void {
    this.isEditMode = true;
    this.currentReward = { ...reward };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.currentReward = this.getEmptyReward();
  }

  saveReward(): void {
    const request =
      this.isEditMode && this.currentReward.id
        ? this.gamificationService.updateReward(
            this.currentReward.id,
            this.currentReward,
          )
        : this.gamificationService.createReward(this.currentReward);

    request.subscribe({
      next: () => {
        alert(
          this.isEditMode
            ? 'Cập nhật phần thưởng thành công!'
            : 'Tạo phần thưởng mới thành công!',
        );
        this.loadRewards();
        this.closeModal();
      },
      error: (error) => {
        console.error('Error saving reward:', error);
        alert('Có lỗi xảy ra khi lưu phần thưởng');
      },
    });
  }

  deleteReward(id: number): void {
    if (confirm('Bạn có chắc muốn xóa phần thưởng này?')) {
      this.gamificationService.deleteReward(id).subscribe({
        next: () => {
          alert('Xóa phần thưởng thành công!');
          this.loadRewards();
        },
        error: (error) => {
          console.error('Error deleting reward:', error);
          alert('Không thể xóa phần thưởng này');
        },
      });
    }
  }

  adjustStock(reward: RewardItem): void {
    this.selectedReward = reward;
    this.newStock = reward.stock;
    this.showStockModal = true;
  }

  closeStockModal(): void {
    this.showStockModal = false;
    this.selectedReward = null;
    this.newStock = 0;
  }

  updateStock(): void {
    if (this.selectedReward?.id !== undefined) {
      this.gamificationService
        .updateRewardStock(this.selectedReward.id, this.newStock)
        .subscribe({
          next: () => {
            alert('Cập nhật tồn kho thành công!');
            this.loadRewards();
            this.closeStockModal();
          },
          error: (error) => {
            console.error('Error updating stock:', error);
            alert('Không thể cập nhật tồn kho');
          },
        });
    }
  }

  private getEmptyReward(): RewardItem {
    return {
      name: '',
      description: '',
      pointsCost: 0,
      stock: 0,
      active: true,
    };
  }
}
