import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-gamification-admin',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="gamification-admin-container">
      <div class="admin-header">
        <h1><i class="fas fa-trophy"></i> Quản Lý Gamification</h1>
        <p class="subtitle">
          Quản lý huy hiệu, thử thách và cấu hình điểm thưởng
        </p>
      </div>

      <div class="gamification-nav">
        <a routerLink="badges" routerLinkActive="active" class="nav-card">
          <i class="fas fa-medal"></i>
          <h3>Quản Lý Huy Hiệu</h3>
          <p>Tạo và chỉnh sửa huy hiệu</p>
        </a>

        <a routerLink="challenges" routerLinkActive="active" class="nav-card">
          <i class="fas fa-flag-checkered"></i>
          <h3>Quản Lý Thử Thách</h3>
          <p>Tạo thử thách đọc sách</p>
        </a>

        <a routerLink="rewards" routerLinkActive="active" class="nav-card">
          <i class="fas fa-gift"></i>
          <h3>Quản Lý Phần Thưởng</h3>
          <p>Cấu hình đổi điểm lấy quà</p>
        </a>

        <a routerLink="leaderboard" routerLinkActive="active" class="nav-card">
          <i class="fas fa-chart-line"></i>
          <h3>Bảng Xếp Hạng</h3>
          <p>Xem thống kê người chơi</p>
        </a>
      </div>

      <div class="content-area">
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styles: [
    `
      .gamification-admin-container {
        padding: 2rem;
        max-width: 1400px;
        margin: 0 auto;
      }

      .admin-header {
        margin-bottom: 2rem;
      }

      .admin-header h1 {
        font-size: 2rem;
        color: #f0f6fc;
        margin-bottom: 0.5rem;
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }

      .admin-header h1 i {
        color: #fbbf24;
      }

      .subtitle {
        color: #8b949e;
        font-size: 1rem;
      }

      .gamification-nav {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 1.5rem;
        margin-bottom: 2rem;
      }

      .nav-card {
        background: #161b22;
        border: 1px solid #30363d;
        border-radius: 12px;
        padding: 1.5rem;
        text-decoration: none;
        color: #f0f6fc;
        transition: all 0.3s;
        cursor: pointer;
      }

      .nav-card:hover,
      .nav-card.active {
        background: #1c2128;
        border-color: #58a6ff;
        transform: translateY(-4px);
        box-shadow: 0 8px 24px rgba(88, 166, 255, 0.2);
      }

      .nav-card i {
        font-size: 2rem;
        margin-bottom: 1rem;
        display: block;
        color: #58a6ff;
      }

      .nav-card h3 {
        font-size: 1.1rem;
        margin-bottom: 0.5rem;
        color: #f0f6fc;
      }

      .nav-card p {
        font-size: 0.9rem;
        color: #8b949e;
        margin: 0;
      }

      .content-area {
        background: #0d1117;
        border: 1px solid #30363d;
        border-radius: 12px;
        padding: 2rem;
        min-height: 400px;
      }
    `,
  ],
})
export class GamificationAdminComponent implements OnInit {
  ngOnInit(): void {
    // Component initialized
  }
}
