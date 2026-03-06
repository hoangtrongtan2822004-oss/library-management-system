import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  GamificationAdminService,
  LeaderboardEntry,
} from '../gamification-admin.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-manage-leaderboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="manage-leaderboard">
      <div class="header">
        <h2><i class="fas fa-chart-line"></i> Bảng Xếp Hạng</h2>
        <div class="header-actions">
          <div class="limit-select">
            <label>Hiển thị top:</label>
            <select [(ngModel)]="limit" (change)="loadLeaderboard()">
              <option [value]="10">10</option>
              <option [value]="20">20</option>
              <option [value]="50">50</option>
              <option [value]="100">100</option>
            </select>
          </div>
          <button class="btn btn-secondary" (click)="loadLeaderboard()">
            <i class="fas fa-sync-alt" [class.fa-spin]="loading"></i> Làm mới
          </button>
        </div>
      </div>

      <!-- Stats summary -->
      <div class="stats-row" *ngIf="entries.length > 0">
        <div class="stat-card">
          <i class="fas fa-users"></i>
          <div>
            <span class="stat-value">{{ entries.length }}</span>
            <span class="stat-label">Người dùng</span>
          </div>
        </div>
        <div class="stat-card">
          <i class="fas fa-star"></i>
          <div>
            <span class="stat-value">{{ topPoints }}</span>
            <span class="stat-label">Điểm cao nhất</span>
          </div>
        </div>
        <div class="stat-card">
          <i class="fas fa-medal"></i>
          <div>
            <span class="stat-value">{{ topBadges }}</span>
            <span class="stat-label">Huy hiệu nhiều nhất</span>
          </div>
        </div>
        <div class="stat-card">
          <i class="fas fa-layer-group"></i>
          <div>
            <span class="stat-value">{{ avgLevel | number: '1.1-1' }}</span>
            <span class="stat-label">Level trung bình</span>
          </div>
        </div>
      </div>

      <!-- Loading -->
      <div class="loading-state" *ngIf="loading">
        <i class="fas fa-spinner fa-spin fa-2x"></i>
        <p>Đang tải bảng xếp hạng...</p>
      </div>

      <!-- Leaderboard Table -->
      <div class="table-container" *ngIf="!loading && entries.length > 0">
        <table class="leaderboard-table">
          <thead>
            <tr>
              <th class="col-rank">Hạng</th>
              <th class="col-user">Người Dùng</th>
              <th class="col-level">Level</th>
              <th class="col-points">Điểm</th>
              <th class="col-badges">Huy Hiệu</th>
            </tr>
          </thead>
          <tbody>
            <tr
              *ngFor="let entry of entries; let i = index"
              [class.top1]="i === 0"
              [class.top2]="i === 1"
              [class.top3]="i === 2"
            >
              <td class="col-rank">
                <div class="rank-badge" [class]="getRankClass(i)">
                  <i *ngIf="i === 0" class="fas fa-crown"></i>
                  <i *ngIf="i === 1" class="fas fa-medal"></i>
                  <i *ngIf="i === 2" class="fas fa-award"></i>
                  <span *ngIf="i > 2">{{ i + 1 }}</span>
                </div>
              </td>
              <td class="col-user">
                <div class="user-info">
                  <div class="avatar">
                    {{ getInitials(entry.userName) }}
                  </div>
                  <span class="user-name">{{ entry.userName }}</span>
                </div>
              </td>
              <td class="col-level">
                <span class="level-badge" [class]="getLevelClass(entry.level)">
                  Lv.{{ entry.level }}
                </span>
              </td>
              <td class="col-points">
                <div class="points-display">
                  <i class="fas fa-star"></i>
                  <span>{{ entry.totalPoints | number }}</span>
                </div>
              </td>
              <td class="col-badges">
                <div class="badges-display">
                  <i class="fas fa-medal"></i>
                  <span>{{ entry.badgeCount }}</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Empty state -->
      <div class="empty-state" *ngIf="!loading && entries.length === 0">
        <i class="fas fa-chart-line fa-4x"></i>
        <p>Chưa có dữ liệu bảng xếp hạng.</p>
      </div>
    </div>
  `,
  styles: [
    `
      .manage-leaderboard {
        padding: 1rem;
      }

      .header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1.5rem;
        flex-wrap: wrap;
        gap: 1rem;
      }

      .header h2 {
        font-size: 1.5rem;
        color: #f0f6fc;
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin: 0;
      }

      .header h2 i {
        color: #58a6ff;
      }

      .header-actions {
        display: flex;
        align-items: center;
        gap: 1rem;
      }

      .limit-select {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        color: #8b949e;
        font-size: 0.875rem;
      }

      .limit-select select {
        background: #161b22;
        border: 1px solid #30363d;
        color: #f0f6fc;
        padding: 0.375rem 0.625rem;
        border-radius: 6px;
        font-size: 0.875rem;
        cursor: pointer;
      }

      .btn {
        padding: 0.5rem 1rem;
        border-radius: 8px;
        border: none;
        cursor: pointer;
        font-size: 0.875rem;
        display: flex;
        align-items: center;
        gap: 0.5rem;
        transition: all 0.2s;
      }

      .btn-secondary {
        background: #21262d;
        color: #f0f6fc;
        border: 1px solid #30363d;
      }

      .btn-secondary:hover {
        background: #30363d;
      }

      /* Stats row */
      .stats-row {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
        gap: 1rem;
        margin-bottom: 1.5rem;
      }

      .stat-card {
        background: #161b22;
        border: 1px solid #30363d;
        border-radius: 10px;
        padding: 1rem 1.25rem;
        display: flex;
        align-items: center;
        gap: 0.875rem;
      }

      .stat-card > i {
        font-size: 1.5rem;
        color: #58a6ff;
        width: 2rem;
        text-align: center;
      }

      .stat-value {
        display: block;
        font-size: 1.375rem;
        font-weight: 700;
        color: #f0f6fc;
        line-height: 1.2;
      }

      .stat-label {
        display: block;
        font-size: 0.75rem;
        color: #8b949e;
        margin-top: 0.125rem;
      }

      /* Loading */
      .loading-state {
        text-align: center;
        padding: 3rem;
        color: #8b949e;
      }

      .loading-state p {
        margin-top: 1rem;
      }

      /* Table */
      .table-container {
        background: #161b22;
        border: 1px solid #30363d;
        border-radius: 12px;
        overflow: hidden;
      }

      .leaderboard-table {
        width: 100%;
        border-collapse: collapse;
      }

      .leaderboard-table thead tr {
        background: #21262d;
        border-bottom: 1px solid #30363d;
      }

      .leaderboard-table th {
        padding: 0.875rem 1.25rem;
        text-align: left;
        font-size: 0.8125rem;
        font-weight: 600;
        color: #8b949e;
        text-transform: uppercase;
        letter-spacing: 0.04em;
      }

      .leaderboard-table td {
        padding: 0.875rem 1.25rem;
        border-bottom: 1px solid #21262d;
        vertical-align: middle;
      }

      .leaderboard-table tbody tr:last-child td {
        border-bottom: none;
      }

      .leaderboard-table tbody tr {
        transition: background 0.15s;
      }

      .leaderboard-table tbody tr:hover {
        background: #1c2128;
      }

      /* Top 3 highlights */
      .leaderboard-table tbody tr.top1 {
        background: rgba(251, 191, 36, 0.06);
      }

      .leaderboard-table tbody tr.top1:hover {
        background: rgba(251, 191, 36, 0.1);
      }

      .leaderboard-table tbody tr.top2 {
        background: rgba(148, 163, 184, 0.05);
      }

      .leaderboard-table tbody tr.top3 {
        background: rgba(180, 120, 60, 0.05);
      }

      /* Rank badge */
      .rank-badge {
        width: 2.25rem;
        height: 2.25rem;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 0.875rem;
        font-weight: 700;
      }

      .rank-badge.rank-1 {
        background: rgba(251, 191, 36, 0.15);
        color: #fbbf24;
        font-size: 1rem;
      }

      .rank-badge.rank-2 {
        background: rgba(148, 163, 184, 0.15);
        color: #94a3b8;
        font-size: 0.95rem;
      }

      .rank-badge.rank-3 {
        background: rgba(180, 120, 60, 0.15);
        color: #cd7f32;
        font-size: 0.95rem;
      }

      .rank-badge.rank-other {
        background: #21262d;
        color: #8b949e;
      }

      /* User info */
      .user-info {
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }

      .avatar {
        width: 2rem;
        height: 2rem;
        border-radius: 50%;
        background: linear-gradient(135deg, #58a6ff, #388bfd);
        color: white;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 0.75rem;
        font-weight: 700;
        flex-shrink: 0;
      }

      .user-name {
        color: #f0f6fc;
        font-weight: 500;
      }

      /* Level badge */
      .level-badge {
        padding: 0.25rem 0.625rem;
        border-radius: 20px;
        font-size: 0.75rem;
        font-weight: 700;
      }

      .level-bronze {
        background: rgba(180, 120, 60, 0.2);
        color: #cd7f32;
      }

      .level-silver {
        background: rgba(148, 163, 184, 0.2);
        color: #94a3b8;
      }

      .level-gold {
        background: rgba(251, 191, 36, 0.2);
        color: #fbbf24;
      }

      .level-platinum {
        background: rgba(88, 166, 255, 0.2);
        color: #58a6ff;
      }

      .level-diamond {
        background: rgba(139, 92, 246, 0.2);
        color: #a78bfa;
      }

      /* Points & badges display */
      .points-display,
      .badges-display {
        display: flex;
        align-items: center;
        gap: 0.4rem;
        font-weight: 600;
        color: #f0f6fc;
      }

      .points-display i {
        color: #fbbf24;
      }

      .badges-display i {
        color: #58a6ff;
      }

      /* Column widths */
      .col-rank {
        width: 80px;
      }

      .col-level {
        width: 100px;
      }

      .col-points {
        width: 130px;
      }

      .col-badges {
        width: 120px;
      }

      /* Empty state */
      .empty-state {
        text-align: center;
        padding: 3rem;
        color: #8b949e;
      }

      .empty-state i {
        margin-bottom: 1rem;
        opacity: 0.4;
      }
    `,
  ],
})
export class ManageLeaderboardComponent implements OnInit {
  entries: LeaderboardEntry[] = [];
  loading = false;
  limit = 50;

  constructor(
    private gamificationService: GamificationAdminService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.loadLeaderboard();
  }

  loadLeaderboard(): void {
    this.loading = true;
    this.gamificationService.getLeaderboard(this.limit).subscribe({
      next: (data) => {
        this.entries = data.map((e, i) => ({ ...e, rank: i + 1 }));
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading leaderboard:', err);
        this.toastr.error('Không thể tải bảng xếp hạng', 'Lỗi');
        this.loading = false;
      },
    });
  }

  get topPoints(): number {
    return this.entries[0]?.totalPoints ?? 0;
  }

  get topBadges(): number {
    return Math.max(...this.entries.map((e) => e.badgeCount), 0);
  }

  get avgLevel(): number {
    if (!this.entries.length) return 0;
    return (
      this.entries.reduce((sum, e) => sum + e.level, 0) / this.entries.length
    );
  }

  getInitials(name: string): string {
    if (!name) return '?';
    return name
      .split(' ')
      .map((w) => w[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }

  getRankClass(index: number): string {
    if (index === 0) return 'rank-badge rank-1';
    if (index === 1) return 'rank-badge rank-2';
    if (index === 2) return 'rank-badge rank-3';
    return 'rank-badge rank-other';
  }

  getLevelClass(level: number): string {
    if (level <= 3) return 'level-badge level-bronze';
    if (level <= 6) return 'level-badge level-silver';
    if (level <= 10) return 'level-badge level-gold';
    if (level <= 15) return 'level-badge level-platinum';
    return 'level-badge level-diamond';
  }
}
