import { Component, OnInit } from '@angular/core';
import { AdminService, AuditLog, Page } from 'src/app/services/admin.service';
import { ToastrService } from 'ngx-toastr';

@Component({
  selector: 'app-audit-logs',
  templateUrl: './audit-logs.component.html',
  styleUrls: ['./audit-logs.component.css'],
  standalone: false,
})
export class AuditLogsComponent implements OnInit {
  logs: AuditLog[] = [];
  loading = false;
  errorMessage = '';

  // Filters
  actor = '';
  action = '';
  status = '';
  from = '';
  to = '';

  // Pagination
  page = 0;
  size = 10;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private adminService: AdminService,
    private toastr: ToastrService,
  ) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.loading = true;
    this.errorMessage = '';
    this.adminService
      .getAuditLogs({
        actor: this.actor.trim() || undefined,
        action: this.action.trim() || undefined,
        status: this.status || undefined,
        from: this.from || undefined,
        to: this.to || undefined,
        page: this.page,
        size: this.size,
      })
      .subscribe({
        next: (res: Page<AuditLog>) => {
          this.logs = res.content;
          this.totalPages = res.totalPages;
          this.totalElements = res.totalElements;
        },
        error: (err) => {
          this.errorMessage = 'Không tải được audit logs.';
          this.toastr.error(this.errorMessage);
          console.error(err);
        },
        complete: () => {
          this.loading = false;
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

  onPageChange(pageIndex: number): void {
    if (pageIndex < 0 || pageIndex >= this.totalPages) return;
    this.page = pageIndex;
    this.loadLogs();
  }

  formatPayload(payload?: string): string {
    if (!payload) return '';
    return payload.length > 200 ? payload.slice(0, 200) + '…' : payload;
  }
}
