import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  GamificationAdminService,
  Challenge,
} from '../gamification-admin.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-manage-challenges',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="manage-challenges">
      <div class="header">
        <h2><i class="fas fa-flag-checkered"></i> Quản Lý Thử Thách</h2>
        <button class="btn btn-primary" (click)="openCreateModal()">
          <i class="fas fa-plus"></i> Tạo Thử Thách Mới
        </button>
      </div>

      <div
        class="challenges-grid"
        *ngIf="challenges.length > 0; else noChallenges"
      >
        <div
          class="challenge-card"
          *ngFor="let challenge of challenges"
          [class.inactive]="!challenge.active"
        >
          <div class="challenge-header">
            <div class="challenge-title">
              <h3>{{ challenge.name }}</h3>
              <span class="status-badge" [class.active]="challenge.active">
                {{ challenge.active ? 'Đang diễn ra' : 'Đã kết thúc' }}
              </span>
            </div>
            <div class="challenge-actions">
              <button
                class="btn-icon"
                (click)="toggleActive(challenge)"
                [title]="challenge.active ? 'Tạm dừng' : 'Kích hoạt'"
              >
                <i
                  class="fas"
                  [class.fa-pause]="challenge.active"
                  [class.fa-play]="!challenge.active"
                ></i>
              </button>
              <button
                class="btn-icon"
                (click)="editChallenge(challenge)"
                title="Chỉnh sửa"
              >
                <i class="fas fa-edit"></i>
              </button>
              <button
                class="btn-icon danger"
                (click)="deleteChallenge(challenge.id!)"
                title="Xóa"
              >
                <i class="fas fa-trash"></i>
              </button>
            </div>
          </div>

          <p class="challenge-desc">{{ challenge.description }}</p>

          <div class="challenge-info">
            <div class="info-item">
              <i class="fas fa-calendar-alt"></i>
              <span
                >{{ formatDate(challenge.startDate) }} -
                {{ formatDate(challenge.endDate) }}</span
              >
            </div>
            <div class="info-item">
              <i class="fas fa-bullseye"></i>
              <span>Mục tiêu: {{ challenge.targetCount }} cuốn sách</span>
            </div>
            <div class="info-item">
              <i class="fas fa-trophy"></i>
              <span>Phần thưởng: {{ challenge.rewardPoints }} điểm</span>
            </div>
          </div>
        </div>
      </div>

      <ng-template #noChallenges>
        <div class="empty-state">
          <i class="fas fa-flag-checkered fa-4x"></i>
          <p>Chưa có thử thách nào. Hãy tạo thử thách đầu tiên!</p>
        </div>
      </ng-template>

      <!-- Modal Create/Edit Challenge -->
      <div class="modal" *ngIf="showModal" (click)="closeModal()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>{{ isEditMode ? 'Chỉnh Sửa' : 'Tạo Mới' }} Thử Thách</h3>
            <button class="btn-close" (click)="closeModal()">×</button>
          </div>
          <form (ngSubmit)="saveChallenge()">
            <div class="form-group">
              <label>Tên Thử Thách *</label>
              <input
                type="text"
                [(ngModel)]="currentChallenge.name"
                name="name"
                placeholder="VD: Đọc 5 cuốn sách lịch sử trong tháng này"
                required
              />
            </div>
            <div class="form-group">
              <label>Mô Tả *</label>
              <textarea
                [(ngModel)]="currentChallenge.description"
                name="description"
                placeholder="Mô tả chi tiết thử thách..."
                rows="3"
                required
              ></textarea>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>Ngày Bắt Đầu *</label>
                <input
                  type="date"
                  [(ngModel)]="currentChallenge.startDate"
                  name="startDate"
                  required
                />
              </div>
              <div class="form-group">
                <label>Ngày Kết Thúc *</label>
                <input
                  type="date"
                  [(ngModel)]="currentChallenge.endDate"
                  name="endDate"
                  required
                />
              </div>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>Số Sách Mục Tiêu *</label>
                <input
                  type="number"
                  [(ngModel)]="currentChallenge.targetCount"
                  name="targetCount"
                  min="1"
                  placeholder="VD: 5"
                  required
                />
              </div>
              <div class="form-group">
                <label>Điểm Thưởng *</label>
                <input
                  type="number"
                  [(ngModel)]="currentChallenge.rewardPoints"
                  name="rewardPoints"
                  min="0"
                  placeholder="VD: 500"
                  required
                />
              </div>
            </div>
            <div class="form-group">
              <label class="checkbox-label">
                <input
                  type="checkbox"
                  [(ngModel)]="currentChallenge.active"
                  name="active"
                />
                <span>Kích hoạt ngay</span>
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

      <!-- Delete Confirmation Toast -->
      <div class="confirm-toast" *ngIf="pendingDeleteId !== null">
        <div class="confirm-toast-content">
          <i class="fas fa-exclamation-triangle"></i>
          <span>Bạn có chắc muốn xóa thử thách này không?</span>
          <div class="confirm-toast-actions">
            <button class="btn-confirm-yes" (click)="confirmDelete()">
              <i class="fas fa-trash"></i> Xóa
            </button>
            <button class="btn-confirm-no" (click)="cancelDelete()">
              <i class="fas fa-times"></i> Hủy
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .manage-challenges {
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

      .challenges-grid {
        display: grid;
        gap: 1.5rem;
      }

      .challenge-card {
        background: #161b22;
        border: 1px solid #30363d;
        border-radius: 12px;
        padding: 1.5rem;
        transition: all 0.3s;
      }

      .challenge-card.inactive {
        opacity: 0.6;
        border-color: #21262d;
      }

      .challenge-card:hover {
        border-color: #58a6ff;
        box-shadow: 0 4px 12px rgba(88, 166, 255, 0.2);
      }

      .challenge-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 1rem;
      }

      .challenge-title {
        flex: 1;
      }

      .challenge-title h3 {
        color: #58a6ff;
        font-size: 1.2rem;
        margin-bottom: 0.5rem;
      }

      .status-badge {
        display: inline-block;
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

      .challenge-desc {
        color: #8b949e;
        margin-bottom: 1rem;
        line-height: 1.5;
      }

      .challenge-info {
        display: flex;
        gap: 1.5rem;
        flex-wrap: wrap;
      }

      .info-item {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        color: #f0f6fc;
        font-size: 0.9rem;
      }

      .info-item i {
        color: #58a6ff;
      }

      .challenge-actions {
        display: flex;
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
        max-width: 700px;
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

      /* Delete confirmation toast */
      .confirm-toast {
        position: fixed;
        bottom: 2rem;
        left: 50%;
        transform: translateX(-50%);
        z-index: 9999;
        animation: slideUpToast 0.25s ease;
      }

      .confirm-toast-content {
        background: #2d1f0e;
        border: 1px solid #f59e0b;
        border-radius: 10px;
        padding: 0.875rem 1.5rem;
        display: flex;
        align-items: center;
        gap: 1rem;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
        color: #fde68a;
        white-space: nowrap;
        font-size: 0.9375rem;
      }

      .confirm-toast-content .fa-exclamation-triangle {
        color: #f59e0b;
        font-size: 1.125rem;
      }

      .confirm-toast-actions {
        display: flex;
        gap: 0.5rem;
        margin-left: 0.5rem;
      }

      .btn-confirm-yes {
        background: #dc2626;
        color: white;
        border: none;
        border-radius: 6px;
        padding: 0.375rem 0.875rem;
        cursor: pointer;
        font-size: 0.8125rem;
        display: flex;
        align-items: center;
        gap: 0.35rem;
        transition: background 0.2s;
      }

      .btn-confirm-yes:hover {
        background: #b91c1c;
      }

      .btn-confirm-no {
        background: #21262d;
        color: #f0f6fc;
        border: 1px solid #30363d;
        border-radius: 6px;
        padding: 0.375rem 0.875rem;
        cursor: pointer;
        font-size: 0.8125rem;
        display: flex;
        align-items: center;
        gap: 0.35rem;
        transition: background 0.2s;
      }

      .btn-confirm-no:hover {
        background: #30363d;
      }

      @keyframes slideUpToast {
        from {
          opacity: 0;
          transform: translateX(-50%) translateY(20px);
        }
        to {
          opacity: 1;
          transform: translateX(-50%) translateY(0);
        }
      }
    `,
  ],
})
export class ManageChallengesComponent implements OnInit {
  challenges: Challenge[] = [];
  showModal = false;
  isEditMode = false;
  currentChallenge: Challenge = this.getEmptyChallenge();
  pendingDeleteId: number | null = null;
  private deleteTimer: any = null;

  constructor(
    private gamificationService: GamificationAdminService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.loadChallenges();
  }

  loadChallenges(): void {
    this.gamificationService.getAllChallenges().subscribe({
      next: (challenges) => {
        this.challenges = challenges;
      },
      error: (error) => {
        console.error('Error loading challenges:', error);
        this.toastr.error('Không thể tải danh sách thử thách', 'Lỗi');
      },
    });
  }

  openCreateModal(): void {
    this.isEditMode = false;
    this.currentChallenge = this.getEmptyChallenge();
    this.showModal = true;
  }

  editChallenge(challenge: Challenge): void {
    this.isEditMode = true;
    this.currentChallenge = { ...challenge };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.currentChallenge = this.getEmptyChallenge();
  }

  saveChallenge(): void {
    const request =
      this.isEditMode && this.currentChallenge.id
        ? this.gamificationService.updateChallenge(
            this.currentChallenge.id,
            this.currentChallenge,
          )
        : this.gamificationService.createChallenge(this.currentChallenge);

    request.subscribe({
      next: () => {
        this.toastr.success(
          this.isEditMode
            ? 'Cập nhật thử thách thành công!'
            : 'Tạo thử thách mới thành công!',
          'Thành công',
        );
        this.loadChallenges();
        this.closeModal();
      },
      error: (error) => {
        console.error('Error saving challenge:', error);
        this.toastr.error('Có lỗi xảy ra khi lưu thử thách', 'Lỗi');
      },
    });
  }

  toggleActive(challenge: Challenge): void {
    if (challenge.id) {
      this.gamificationService.toggleChallengeActive(challenge.id).subscribe({
        next: () => {
          challenge.active = !challenge.active;
          this.toastr.success(`Đã ${challenge.active ? 'kích hoạt' : 'tạm dừng'} thử thách!`, 'Thành công');
        },
        error: (error) => {
          console.error('Error toggling challenge:', error);
          this.toastr.error('Không thể thay đổi trạng thái thử thách', 'Lỗi');
        },
      });
    }
  }

  deleteChallenge(id: number): void {
    this.pendingDeleteId = id;
    clearTimeout(this.deleteTimer);
    this.deleteTimer = setTimeout(() => this.cancelDelete(), 5000);
  }

  cancelDelete(): void {
    clearTimeout(this.deleteTimer);
    this.pendingDeleteId = null;
  }

  confirmDelete(): void {
    if (this.pendingDeleteId === null) return;
    const id = this.pendingDeleteId;
    this.cancelDelete();
    this.gamificationService.deleteChallenge(id).subscribe({
      next: () => {
        this.toastr.success('Xóa thử thách thành công!', 'Thành công');
        this.loadChallenges();
      },
      error: (error) => {
        console.error('Error deleting challenge:', error);
        this.toastr.error('Không thể xóa thử thách này', 'Lỗi');
      },
    });
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
  }

  private getEmptyChallenge(): Challenge {
    return {
      name: '',
      description: '',
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
        .toISOString()
        .split('T')[0],
      targetCount: 5,
      rewardPoints: 500,
      active: true,
    };
  }
}
