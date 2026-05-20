import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ── Types ─────────────────────────────────────────────────────────────────────

export interface EmployeeResponse {
  id: number;
  userId?: number;
  businessId: number;
  branchId?: number;
  displayName: string;
  bio?: string;
  avatarUrl?: string;
  position?: string;
  isActive: boolean;
  createdAt: string;
}

export interface CreateEmployeeRequest {
  userId?: number;
  branchId?: number;
  displayName: string;
  bio?: string;
  avatarUrl?: string;
  position?: string;
}

export interface SlotResponse {
  date: string;
  employeeId?: number;
  resourceId?: number;
  serviceId: number;
  durationMin: number;
  availableSlots: string[]; // "HH:mm:ss" strings
}

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface BookingResponse {
  id: number;
  clientId: number;
  clientName: string;
  serviceId: number;
  serviceName: string;
  businessId: number;
  businessName: string;
  branchId: number;
  branchName: string;
  employeeId?: number;
  employeeName?: string;
  resourceId?: number;
  resourceName?: string;
  startTime: string;
  endTime: string;
  status: BookingStatus;
  clientNote?: string;
  businessNote?: string;
  priceSnapshot?: number;
  durationMin: number;
  selectedAttributes: Record<string, unknown>;
  createdAt: string;
}

export interface CreateBookingRequest {
  serviceId: number;
  employeeId?: number;
  resourceId?: number;
  branchId: number;
  startTime: string; // ISO 8601 instant
  clientNote?: string;
  selectedAttributes?: Record<string, unknown>;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ── Service ───────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class BookingService {
  private readonly base = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  // ── Employees ──────────────────────────────────────────────────

  getEmployees(businessId: number): Observable<Page<EmployeeResponse>> {
    return this.http.get<Page<EmployeeResponse>>(
      `${this.base}/businesses/${businessId}/employees`
    );
  }

  getEmployee(businessId: number, employeeId: number): Observable<EmployeeResponse> {
    return this.http.get<EmployeeResponse>(
      `${this.base}/businesses/${businessId}/employees/${employeeId}`
    );
  }

  createEmployee(businessId: number, req: CreateEmployeeRequest): Observable<EmployeeResponse> {
    return this.http.post<EmployeeResponse>(
      `${this.base}/businesses/${businessId}/employees`, req
    );
  }

  deactivateEmployee(businessId: number, employeeId: number): Observable<EmployeeResponse> {
    return this.http.delete<EmployeeResponse>(
      `${this.base}/businesses/${businessId}/employees/${employeeId}`
    );
  }

  // ── Slots ──────────────────────────────────────────────────────

  getSlots(
    serviceId: number,
    date: string,
    employeeId?: number,
    resourceId?: number
  ): Observable<SlotResponse> {
    let params = new HttpParams()
      .set('serviceId', serviceId)
      .set('date', date);
    if (employeeId != null) params = params.set('employeeId', employeeId);
    if (resourceId != null) params = params.set('resourceId', resourceId);
    return this.http.get<SlotResponse>(`${this.base}/slots`, { params });
  }

  // ── Bookings ───────────────────────────────────────────────────

  createBooking(req: CreateBookingRequest): Observable<BookingResponse> {
    return this.http.post<BookingResponse>(`${this.base}/bookings`, req);
  }

  getMyBookings(page = 0, size = 20): Observable<Page<BookingResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<BookingResponse>>(`${this.base}/bookings/my`, { params });
  }

  getBookingById(id: number): Observable<BookingResponse> {
    return this.http.get<BookingResponse>(`${this.base}/bookings/${id}`);
  }

  cancelBooking(id: number, reason?: string): Observable<BookingResponse> {
    return this.http.patch<BookingResponse>(`${this.base}/bookings/${id}/cancel`, { reason });
  }

  confirmBooking(id: number): Observable<BookingResponse> {
    return this.http.patch<BookingResponse>(`${this.base}/bookings/${id}/confirm`, {});
  }

  completeBooking(id: number): Observable<BookingResponse> {
    return this.http.patch<BookingResponse>(`${this.base}/bookings/${id}/complete`, {});
  }

  getBusinessBookings(businessId: number, from?: string, to?: string, page = 0, size = 100): Observable<Page<BookingResponse>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<Page<BookingResponse>>(
      `${this.base}/bookings/business/${businessId}`, { params }
    );
  }
}
