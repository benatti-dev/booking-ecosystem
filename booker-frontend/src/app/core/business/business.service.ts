import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// ── Domain types ──────────────────────────────────────────────────────────────

export interface BusinessCategory {
  id: number;
  name: string;
  label: string;
  iconUrl?: string;
  resourceType: 'EMPLOYEE' | 'RESOURCE' | 'NONE';
}

export interface AttributeDefinition {
  id: number;
  categoryId: number;
  fieldKey: string;
  fieldLabel: string;
  fieldType: 'TEXT' | 'NUMBER' | 'SELECT' | 'BOOLEAN' | 'MULTI_SELECT';
  options?: string[];
  isRequired: boolean;
  sortOrder: number;
}

export interface BusinessResponse {
  id: number;
  ownerId: number;
  ownerName: string;
  category: { id: number; name: string; label: string; resourceType: string };
  name: string;
  description?: string;
  logoUrl?: string;
  status: 'PENDING' | 'ACTIVE' | 'SUSPENDED' | 'REJECTED';
  meta: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface BranchResponse {
  id: number;
  businessId: number;
  name: string;
  address: string;
  city: string;
  country: string;
  postalCode?: string;
  latitude?: number;
  longitude?: number;
  phone?: string;
  email?: string;
  timezone: string;
  isPrimary: boolean;
  status: string;
  createdAt: string;
}

export interface ServiceResponse {
  id: number;
  businessId: number;
  categoryId: number;
  categoryName: string;
  name: string;
  description?: string;
  durationMin: number;
  price?: number;
  currency: string;
  attributes: Record<string, unknown>;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
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
export class BusinessService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  // ── Categories ──────────────────────────────────────────────────

  getCategories(): Observable<BusinessCategory[]> {
    return this.http.get<BusinessCategory[]>(`${this.api}/categories`);
  }

  getAttributeDefinitions(categoryId: number): Observable<AttributeDefinition[]> {
    return this.http.get<AttributeDefinition[]>(
      `${this.api}/categories/${categoryId}/attribute-definitions`
    );
  }

  createCategory(payload: { name: string; label: string; iconUrl?: string; resourceType: string }): Observable<BusinessCategory> {
    return this.http.post<BusinessCategory>(`${this.api}/categories`, payload);
  }

  // ── Businesses ──────────────────────────────────────────────────

  createBusiness(payload: {
    categoryId: number;
    name: string;
    description?: string;
    meta?: Record<string, unknown>;
  }): Observable<BusinessResponse> {
    return this.http.post<BusinessResponse>(`${this.api}/businesses`, payload);
  }

  getMyBusinesses(page = 0, size = 20): Observable<Page<BusinessResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<BusinessResponse>>(`${this.api}/businesses/my`, { params });
  }

  getBusiness(id: number): Observable<BusinessResponse> {
    return this.http.get<BusinessResponse>(`${this.api}/businesses/${id}`);
  }

  getBusinessesByStatus(status: string, page = 0, size = 20): Observable<Page<BusinessResponse>> {
    const params = new HttpParams().set('status', status).set('page', page).set('size', size);
    return this.http.get<Page<BusinessResponse>>(`${this.api}/businesses`, { params });
  }

  changeBusinessStatus(id: number, status: string, reason?: string): Observable<BusinessResponse> {
    return this.http.patch<BusinessResponse>(`${this.api}/businesses/${id}/status`, { status, reason });
  }

  // ── Branches ────────────────────────────────────────────────────

  createBranch(businessId: number, payload: {
    name: string;
    address: string;
    city: string;
    country?: string;
    latitude?: number;
    longitude?: number;
    timezone?: string;
    isPrimary?: boolean;
  }): Observable<BranchResponse> {
    return this.http.post<BranchResponse>(`${this.api}/businesses/${businessId}/branches`, payload);
  }

  getBranches(businessId: number): Observable<BranchResponse[]> {
    return this.http.get<BranchResponse[]>(`${this.api}/businesses/${businessId}/branches`);
  }

  // ── Services ────────────────────────────────────────────────────

  getServices(businessId: number, page = 0, size = 20): Observable<Page<ServiceResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<ServiceResponse>>(
      `${this.api}/businesses/${businessId}/services`, { params });
  }

  createService(businessId: number, payload: {
    name: string;
    description?: string;
    durationMin: number;
    price?: number;
    currency?: string;
    attributes?: Record<string, unknown>;
  }): Observable<ServiceResponse> {
    return this.http.post<ServiceResponse>(
      `${this.api}/businesses/${businessId}/services`, payload);
  }

  deactivateService(businessId: number, serviceId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.api}/businesses/${businessId}/services/${serviceId}`);
  }
}
