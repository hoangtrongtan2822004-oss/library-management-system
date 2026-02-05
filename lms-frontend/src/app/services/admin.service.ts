import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from 'src/environments/environment';
// SỬA: Import đúng tên và đường dẫn
import { Book } from '../models/book';
import { User } from '../models/user';
import { GroupedSettingsResponse } from '../models/setting';
import { HttpParams } from '@angular/common/http';

export interface DashboardStats {
  totalBooks: number;
  totalUsers: number;
  activeLoans: number;
  overdueLoans: number;
  totalFines?: number;
  totalUnpaidFines?: number;
}

export interface DashboardDetails {
  stats: DashboardStats;
  mostLoanedBooks: {
    bookName: string;
    bookId: number;
    loanCount: number;
  }[];
  topBorrowers: { memberId: number; loanCount: number }[];
  recentActivities: any[];
  overdueLoans: any[];
}

export interface LoanDetails {
  loanId: number;
  bookId?: number;
  bookName: string;
  userName: string;
  loanDate: string;
  dueDate: string;
  returnDate?: string;
  status: 'ACTIVE' | 'RETURNED' | 'OVERDUE';
  fineAmount?: number;
  overdueDays?: number;
}

export interface FineDetails {
  loanId: number;
  bookName: string;
  userName: string;
  userId?: number;
  dueDate: string;
  returnDate: string;
  fineAmount: number;
  overdueDays?: number;
  // Payment tracking
  isPaid?: boolean;
  paymentMethod?: 'CASH' | 'TRANSFER' | 'WAIVED' | 'OTHER';
  paymentNote?: string;
  paymentDate?: string;
  paidBy?: string; // Admin username
}

export interface ReportSummary {
  loansByMonth: { month: string; count: number }[];
  mostLoanedBooks: { bookName: string; loanCount: number }[];
  finesByMonth: { month: string; totalFines: number }[];

  // NEW: Deep Analytics
  totalLoansCurrentPeriod?: number;
  totalLoansPreviousPeriod?: number;
  loansGrowthPercent?: number; // % change vs previous period

  totalFinesCurrentPeriod?: number;
  totalFinesPreviousPeriod?: number;
  finesGrowthPercent?: number;

  // Dead stock (books with 0 loans)
  deadStockBooks?: {
    bookId: number;
    bookName: string;
    lastLoanDate: string | null;
  }[];

  // Category distribution
  loansByCategory?: {
    categoryName: string;
    loanCount: number;
    percentage: number;
  }[];

  // Turnover rate (high-performing books)
  highTurnoverBooks?: {
    bookName: string;
    copyCount: number;
    loanCount: number;
    turnoverRate: number;
  }[];
}

export interface RenewalRequestDto {
  id: number;
  loanId: number;
  memberId: number;
  extraDays: number;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  createdAt: string;
  decidedAt?: string;
  adminNote?: string;

  // NEW: Context Enhancement
  memberName?: string; // User full name
  memberClass?: string; // Class/Department
  lateReturnCount?: number; // How many times user returned books late

  bookTitle?: string; // Book name
  bookCoverUrl?: string; // Thumbnail
  bookWaitlistCount?: number; // Number of users waiting for this book

  reason?: string; // User's reason for renewal request
}

export interface AuditLog {
  id: number;
  actor: string;
  actorRoles?: string;
  action: string;
  resource?: string;
  targetId?: string;
  httpMethod?: string;
  path?: string;
  ip?: string;
  userAgent?: string;
  status: string;
  errorMessage?: string;
  requestPayload?: string;
  responsePayload?: string;
  createdAt?: string;
}

export interface MemberCard {
  id: number;
  cardNumber: string;
  barcodeType: 'CODE128' | 'QR';
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED';
  issuedAt?: string;
  expiredAt?: string;
  metadata?: string;
  userId: number;
  username: string;
  fullName?: string;
  email?: string;
  studentClass?: string;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

interface ApiResponse<T> {
  data: T;
  message?: string;
  success?: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AdminService {
  // environment.apiBaseUrl already includes /api
  private API_URL = `${environment.apiBaseUrl}/admin`;

  constructor(private http: HttpClient) {}

  public getDashboardDetails(): Observable<DashboardDetails> {
    return this.http.get<DashboardDetails>(`${this.API_URL}/dashboard/details`);
  }

  public getAllLoans(): Observable<LoanDetails[]> {
    return this.http.get<LoanDetails[]>(`${this.API_URL}/loans`);
  }

  public getUnpaidFines(): Observable<FineDetails[]> {
    return this.http.get<FineDetails[]>(`${this.API_URL}/fines`);
  }

  public markFineAsPaid(
    loanId: number,
    paymentMethod: string,
    note?: string,
  ): Observable<any> {
    return this.http.post(`${this.API_URL}/fines/${loanId}/pay`, {
      paymentMethod,
      note,
    });
  }

  // Get paid fines (transaction history)
  public getPaidFines(
    startDate?: string,
    endDate?: string,
  ): Observable<FineDetails[]> {
    let params: any = {};
    if (startDate) params.startDate = startDate;
    if (endDate) params.endDate = endDate;
    return this.http.get<FineDetails[]>(`${this.API_URL}/fines/paid`, {
      params,
    });
  }

  // Waive fine (forgive debt)
  public waiveFine(loanId: number, reason: string): Observable<any> {
    return this.http.post(`${this.API_URL}/fines/${loanId}/waive`, { reason });
  }

  // Bulk payment for multiple fines
  public bulkPayFines(
    loanIds: number[],
    paymentMethod: string,
    note?: string,
  ): Observable<any> {
    return this.http.post(`${this.API_URL}/fines/bulk-pay`, {
      loanIds,
      paymentMethod,
      note,
    });
  }

  // Get daily payment summary
  public getDailySummary(date: string): Observable<{
    totalAmount: number;
    totalCount: number;
    byMethod: { method: string; amount: number; count: number }[];
  }> {
    return this.http.get<any>(`${this.API_URL}/fines/daily-summary`, {
      params: { date },
    });
  }

  public getReportSummary(
    start: string,
    end: string,
  ): Observable<ReportSummary> {
    const params = { start, end };
    return this.http.get<ReportSummary>(`${this.API_URL}/reports/summary`, {
      params,
    });
  }

  public exportLoansExcel(
    startDate: string,
    endDate: string,
  ): Observable<Blob> {
    const params = { startDate, endDate };
    return this.http.get(`${this.API_URL}/reports/export/loans/excel`, {
      params,
      responseType: 'blob',
    });
  }

  public exportBooksExcel(): Observable<Blob> {
    return this.http.get(`${this.API_URL}/reports/export/books/excel`, {
      responseType: 'blob',
    });
  }

  public exportUsersExcel(): Observable<Blob> {
    return this.http.get(`${this.API_URL}/reports/export/users/excel`, {
      responseType: 'blob',
    });
  }

  // ---------- SETTINGS ----------
  public getSettings(): Observable<
    Array<{ id: number; key: string; value: string }>
  > {
    return this.http.get<Array<{ id: number; key: string; value: string }>>(
      `${this.API_URL}/settings`,
    );
  }

  public updateSetting(
    key: string,
    value: string,
  ): Observable<{ id: number; key: string; value: string }> {
    return this.http.put<{ id: number; key: string; value: string }>(
      `${this.API_URL}/settings/${encodeURIComponent(key)}`,
      { value },
    );
  }

  // ---------- RENEWALS ----------
  public listRenewals(
    status?: 'PENDING' | 'APPROVED' | 'REJECTED',
  ): Observable<RenewalRequestDto[]> {
    const url = status
      ? `${environment.apiBaseUrl}/admin/renewals?status=${status}`
      : `${environment.apiBaseUrl}/admin/renewals`;
    return this.http.get<RenewalRequestDto[]>(url);
  }

  public approveRenewal(
    id: number,
    note?: string,
  ): Observable<RenewalRequestDto> {
    return this.http.post<RenewalRequestDto>(
      `${environment.apiBaseUrl}/admin/renewals/${id}/approve`,
      { note },
    );
  }

  public rejectRenewal(
    id: number,
    note?: string,
  ): Observable<RenewalRequestDto> {
    return this.http.post<RenewalRequestDto>(
      `${environment.apiBaseUrl}/admin/renewals/${id}/reject`,
      { note },
    );
  }

  // NEW: Bulk Actions for Renewals
  public bulkApproveRenewals(renewalIds: number[]): Observable<void> {
    return this.http.post<void>(
      `${environment.apiBaseUrl}/admin/renewals/bulk-approve`,
      { renewalIds },
    );
  }

  public bulkRejectRenewals(renewalIds: number[]): Observable<void> {
    return this.http.post<void>(
      `${environment.apiBaseUrl}/admin/renewals/bulk-reject`,
      { renewalIds },
    );
  }

  public getGroupedSettings(): Observable<GroupedSettingsResponse> {
    return this.http.get<GroupedSettingsResponse>(
      `${this.API_URL}/settings/grouped`,
    );
  }

  public resetSettingToDefault(key: string): Observable<any> {
    return this.http.post(`${this.API_URL}/settings/${key}/reset`, {});
  }

  public resetCategoryToDefaults(category: string): Observable<any> {
    return this.http.post(
      `${this.API_URL}/settings/reset-category/${category}`,
      {},
    );
  }

  // ---------- AUDIT LOGS ----------
  public getAuditLogs(params: {
    actor?: string;
    action?: string;
    status?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Observable<Page<AuditLog>> {
    let httpParams = new HttpParams();
    if (params.actor) httpParams = httpParams.set('actor', params.actor);
    if (params.action) httpParams = httpParams.set('action', params.action);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.from) httpParams = httpParams.set('from', params.from);
    if (params.to) httpParams = httpParams.set('to', params.to);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http.get<Page<AuditLog>>(`${this.API_URL}/audit-logs`, {
      params: httpParams,
    });
  }

  // ---------- MEMBER CARDS ----------
  public searchMemberCards(params: {
    keyword?: string;
    status?: string;
    barcodeType?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Observable<Page<MemberCard>> {
    let httpParams = new HttpParams();
    if (params.keyword) httpParams = httpParams.set('keyword', params.keyword);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.barcodeType)
      httpParams = httpParams.set('barcodeType', params.barcodeType);
    if (params.from) httpParams = httpParams.set('from', params.from);
    if (params.to) httpParams = httpParams.set('to', params.to);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size);

    return this.http
      .get<ApiResponse<Page<MemberCard>> | Page<MemberCard>>(
        `${this.API_URL}/member-cards`,
        { params: httpParams },
      )
      .pipe(this.unwrapApiResponse<Page<MemberCard>>());
  }

  public getMemberCard(id: number): Observable<MemberCard> {
    return this.http
      .get<ApiResponse<MemberCard> | MemberCard>(`${this.API_URL}/member-cards/${id}`)
      .pipe(this.unwrapApiResponse<MemberCard>());
  }

  public createMemberCard(payload: {
    userId: number;
    barcodeType: 'CODE128' | 'QR';
    expiredAt?: string;
    metadata?: string;
  }): Observable<MemberCard> {
    return this.http
      .post<ApiResponse<MemberCard> | MemberCard>(
        `${this.API_URL}/member-cards`,
        payload,
      )
      .pipe(this.unwrapApiResponse<MemberCard>());
  }

  public updateMemberCard(
    id: number,
    payload: {
      userId: number;
      barcodeType: 'CODE128' | 'QR';
      expiredAt?: string;
      metadata?: string;
    },
  ): Observable<MemberCard> {
    return this.http
      .put<ApiResponse<MemberCard> | MemberCard>(
        `${this.API_URL}/member-cards/${id}`,
        payload,
      )
      .pipe(this.unwrapApiResponse<MemberCard>());
  }

  public revokeMemberCard(id: number, reason?: string): Observable<MemberCard> {
    let params = new HttpParams();
    if (reason) {
      params = params.set('reason', reason);
    }
    return this.http
      .post<ApiResponse<MemberCard> | MemberCard>(
        `${this.API_URL}/member-cards/${id}/revoke`,
        null,
        { params },
      )
      .pipe(this.unwrapApiResponse<MemberCard>());
  }

  public downloadMemberCardPdf(id: number): Observable<Blob> {
    return this.http.get(`${this.API_URL}/member-cards/${id}/pdf`, {
      responseType: 'blob',
    });
  }

  public downloadMemberCardBarcode(
    id: number,
    width?: number,
    height?: number,
  ): Observable<Blob> {
    let params = new HttpParams();
    if (width) params = params.set('width', width);
    if (height) params = params.set('height', height);
    return this.http.get(`${this.API_URL}/member-cards/${id}/barcode`, {
      params,
      responseType: 'blob',
    });
  }

  private unwrapApiResponse<T>() {
    return map((res: ApiResponse<T> | T) =>
      (res as ApiResponse<T>).data !== undefined
        ? (res as ApiResponse<T>).data
        : (res as T),
    );
  }
}
