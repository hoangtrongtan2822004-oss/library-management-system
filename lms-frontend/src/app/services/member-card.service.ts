import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { ApiService } from './api.service';

export interface MemberCard {
  id?: number;
  cardNumber: string;
  barcodeType: 'CODE128' | 'QR';
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED';
  issuedAt?: string;
  expiredAt?: string;
  metadata?: string;
  userId?: number;
  username?: string;
  fullName?: string;
  email?: string;
  studentClass?: string;
}

interface ApiResponse<T> {
  data: T;
  message?: string;
  success?: boolean;
}

@Injectable({ providedIn: 'root' })
export class MemberCardService {
  constructor(private http: HttpClient, private api: ApiService) {}

  getMyCard(): Observable<MemberCard> {
    return this.http
      .get<ApiResponse<MemberCard>>(this.api.buildUrl('/user/member-card'))
      .pipe(map((res) => (res && res.data ? res.data : (res as any))));
  }

  getMyCardBarcode(width?: number, height?: number): Observable<Blob> {
    let params = new HttpParams();
    if (width) params = params.set('width', width);
    if (height) params = params.set('height', height);

    return this.http.get(this.api.buildUrl('/user/member-card/barcode'), {
      params,
      responseType: 'blob',
    });
  }
}
