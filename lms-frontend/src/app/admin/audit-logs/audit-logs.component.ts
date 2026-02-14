import { Component, OnInit } from '@angular/core';
import { AdminService, AuditLog, Page } from '../../services/admin.service';

@Component({
  selector: 'app-audit-logs',
  templateUrl: './audit-logs.component.html',
  styleUrls: ['./audit-logs.component.css'],
  standalone: false,
})
export class AuditLogsComponent implements OnInit {
  logsPage?: Page<AuditLog>;
  isLoading = false;

  actor = '';
  action = '';
  status = '';
  from = '';
  to = '';

  page = 0;
  size = 20;

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.isLoading = true;
    this.adminService
      .getAuditLogs({
        actor: this.actor || undefined,
        action: this.action || undefined,
        status: this.status || undefined,
        from: this.from || undefined,
        to: this.to || undefined,
        page: this.page,
        size: this.size,
      })
      .subscribe({
        next: (page) => {
          this.logsPage = page;
          this.isLoading = false;
        },
        error: () => {
          this.isLoading = false;
        },
      });
  }

  applyFilters(): void {
    this.page = 0;
    this.loadLogs();
  }

  resetFilters(): void {
    this.actor = '';
    this.action = '';
    this.status = '';
    this.from = '';
    this.to = '';
    this.page = 0;
    this.loadLogs();
  }

  nextPage(): void {
    if (!this.logsPage || this.page + 1 >= this.logsPage.totalPages) return;
    this.page += 1;
    this.loadLogs();
  }

  prevPage(): void {
    if (this.page === 0) return;
    this.page -= 1;
    this.loadLogs();
  }

  getStatusClass(status?: string): string {
    if (status === 'SUCCESS') return 'badge bg-success';
    if (status === 'FAIL') return 'badge bg-danger';
    return 'badge bg-secondary';
  }

  buildMessage(log: AuditLog): string {
    const action = log.action || '';
    const resource = log.resource ? log.resource.toLowerCase() : 'action';
    const target = log.targetId ? `#${log.targetId}` : '';
    if (action.startsWith('DELETE')) return `Xoa ${resource} ${target}`.trim();
    if (action.startsWith('POST')) return `Tao ${resource} ${target}`.trim();
    if (action.startsWith('PUT') || action.startsWith('PATCH'))
      return `Cap nhat ${resource} ${target}`.trim();
    return `${action} ${resource} ${target}`.trim();
  }
}
