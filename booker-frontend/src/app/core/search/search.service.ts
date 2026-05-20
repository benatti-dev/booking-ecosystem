import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface BusinessSearchResult {
  id: number;
  name: string;
  description: string | null;
  logoUrl: string | null;
  categoryId: number;
  categoryName: string;
  categoryLabel: string;
  city: string;
  address: string;
  timezone: string;
  distanceMeters: number | null;
}

export interface BusinessCategory {
  id: number;
  name: string;
  label: string;
  iconUrl: string | null;
}

export interface SearchParams {
  lat?: number;
  lng?: number;
  radiusKm?: number;
  categoryId?: number;
  query?: string;
  page?: number;
  size?: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class SearchService {
  private readonly api = environment.apiUrl;

  constructor(private readonly http: HttpClient) {}

  searchBusinesses(params: SearchParams): Observable<Page<BusinessSearchResult>> {
    let p = new HttpParams();
    if (params.lat != null)        p = p.set('lat', params.lat);
    if (params.lng != null)        p = p.set('lng', params.lng);
    if (params.radiusKm != null)   p = p.set('radiusKm', params.radiusKm);
    if (params.categoryId != null) p = p.set('categoryId', params.categoryId);
    if (params.query?.trim())      p = p.set('query', params.query.trim());
    p = p.set('page',  params.page  ?? 0);
    p = p.set('size',  params.size  ?? 20);
    return this.http.get<Page<BusinessSearchResult>>(`${this.api}/search/businesses`, { params: p });
  }

  getCategories(): Observable<BusinessCategory[]> {
    return this.http.get<BusinessCategory[]>(`${this.api}/categories`);
  }
}
