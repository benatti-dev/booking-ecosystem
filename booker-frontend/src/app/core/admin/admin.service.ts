import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '../business/business.service';

// ── Domain types ──────────────────────────────────────────────────────────────

export type UserRole   = 'ADMIN' | 'BUSINESS_OWNER' | 'EMPLOYEE' | 'CLIENT';
export type UserStatus = 'ACTIVE' | 'SUSPENDED';

export interface AdminUserResponse {
  id: number;
  email: string;
  fullName: string;
  phone?: string;
  role: UserRole;
  status: UserStatus;
  avatarUrl?: string;
  createdAt: string;
}

export interface AuditLogResponse {
  id: number;
  userId: number | null;
  action: string;
  ipAddress: string | null;
  details: string | null;
  createdAt: string;
}

// ── Service ───────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AdminService {

  private readonly api = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  // ── Users ──────────────────────────────────────────────────────────────────

  /** Lists users with optional role and status filters. */
  listUsers(
    page: number = 0,
    size: number = 20,
    role?: UserRole,
    status?: UserStatus,
  ): Observable<Page<AdminUserResponse>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (role)   params = params.set('role', role);
    if (status) params = params.set('status', status);

    return this.http.get<Page<AdminUserResponse>>(`${this.api}/admin/users`, { params });
  }

  /** Suspends or restores a user account. */
  changeUserStatus(userId: number, status: UserStatus): Observable<AdminUserResponse> {
    return this.http.patch<AdminUserResponse>(
      `${this.api}/admin/users/${userId}/status`,
      { status }
    );
  }

  // ── Audit Logs ─────────────────────────────────────────────────────────────

  /** Lists audit log entries with optional filters. */
  listAuditLogs(
    page: number = 0,
    size: number = 50,
    action?: string,
    from?: string,
    to?: string,
  ): Observable<Page<AuditLogResponse>> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (action) params = params.set('action', action);
    if (from)   params = params.set('from', from);
    if (to)     params = params.set('to', to);

    return this.http.get<Page<AuditLogResponse>>(`${this.api}/admin/audit-logs`, { params });
  }
}
