import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ── Domain types ──────────────────────────────────────────────────────────────

export interface DailyBookingStat {
  day: string;            // yyyy-MM-dd
  totalBookings: number;
  revenue: number;
}

export interface ServiceRevenueStat {
  serviceId: number;
  serviceName: string;
  bookingCount: number;
  totalRevenue: number;
}

export interface EmployeeUtilizationStat {
  employeeId: number;
  employeeName: string;
  completedBookings: number;
}

export interface BusinessAnalyticsResponse {
  businessId: number;
  businessName: string;
  dailyBookings: DailyBookingStat[];
  revenueByService: ServiceRevenueStat[];
  employeeUtilization: EmployeeUtilizationStat[];
  cancellationRate: number;
  totalCompletedBookings: number;
  totalRevenue: number;
}

export interface PlatformOverviewResponse {
  totalBusinessesActive: number;
  totalBusinessesPending: number;
  totalBusinessesSuspended: number;
  totalBusinessesRejected: number;
  totalClients: number;
  totalBusinessOwners: number;
  totalEmployees: number;
  totalAdmins: number;
  bookingsLast30Days: number;
  completedBookingsLast30Days: number;
  revenueLast30Days: number;
  newUsersLast7Days: number;
  newBusinessesLast7Days: number;
}

// ── Service ───────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AnalyticsService {

  private readonly api = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  /** Fetches business-level analytics for the last 30 days. */
  getBusinessAnalytics(businessId: number): Observable<BusinessAnalyticsResponse> {
    return this.http.get<BusinessAnalyticsResponse>(
      `${this.api}/analytics/business/${businessId}`
    );
  }

  /** Fetches platform-wide overview stats (ADMIN only). */
  getPlatformOverview(): Observable<PlatformOverviewResponse> {
    return this.http.get<PlatformOverviewResponse>(`${this.api}/admin/analytics/overview`);
  }
}
