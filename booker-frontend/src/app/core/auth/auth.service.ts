import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, map, Observable, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

// -- Domain types

export interface AuthUser {
  id: number;
  email: string;
  fullName: string;
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: { id: number; email: string; fullName: string; role: string };
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  phone?: string;
  password: string;
}

// -- JWT payload

interface JwtPayload {
  sub: string;
  email: string;
  username: string;
  userId: number;
  fullName: string;
  roles: string[];
  exp: number;
  iat: number;
}

// -- Service

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly ACCESS_KEY  = 'booker_token';
  private readonly REFRESH_KEY = 'booker_refresh';
  private readonly USER_KEY    = 'booker_user';
  private readonly api         = environment.apiUrl;

  constructor(
    private readonly http: HttpClient,
    private readonly router: Router,
  ) {}

  verifySession(): Observable<AuthUser | null> {
    const token = this.getAccessToken();
    if (!token) return of(null);

    const payload = this.decodePayload(token);
    if (!payload || payload.exp * 1000 <= Date.now()) {
      this.clearSession();
      return of(null);
    }

    return this.http
      .get<{ id: number; email: string; fullName: string; role: string }>(`${this.api}/auth/me`)
      .pipe(
        map(u => {
          const user: AuthUser = { id: u.id, email: u.email, fullName: u.fullName, roles: [u.role] };
          localStorage.setItem(this.USER_KEY, JSON.stringify(user));
          return user;
        }),
        catchError(() => { this.clearSession(); return of(null); }),
      );
  }

  login(email: string, password: string): Observable<AuthUser> {
    return this.http
      .post<AuthResponse>(`${this.api}/auth/login`, { username: email, password })
      .pipe(map(res => this.storeTokens(res)));
  }

  register(req: RegisterRequest): Observable<AuthUser> {
    return this.http
      .post<AuthResponse>(`${this.api}/auth/register`, req)
      .pipe(map(res => this.storeTokens(res)));
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      this.http.post(`${this.api}/auth/logout`, { refreshToken })
        .pipe(catchError(() => throwError(() => null)))
        .subscribe({ error: () => {} });
    }
    this.clearSession();
    this.router.navigateByUrl('/login');
  }

  refreshToken(): Observable<void> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) { this.clearSession(); return throwError(() => new Error('No refresh token')); }
    return this.http
      .post<AuthResponse>(`${this.api}/auth/refresh`, { refreshToken })
      .pipe(map(res => { this.storeTokens(res); }));
  }

  getAccessToken(): string | null { return localStorage.getItem(this.ACCESS_KEY); }
  getRefreshToken(): string | null { return localStorage.getItem(this.REFRESH_KEY); }

  isAuthenticated(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;
    const payload = this.decodePayload(token);
    return !!payload && payload.exp * 1000 > Date.now();
  }

  clearSession(): void {
    localStorage.removeItem(this.ACCESS_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  private storeTokens(res: AuthResponse): AuthUser {
    localStorage.setItem(this.ACCESS_KEY, res.accessToken);
    localStorage.setItem(this.REFRESH_KEY, res.refreshToken);
    const user: AuthUser = { id: res.user.id, email: res.user.email, fullName: res.user.fullName, roles: [res.user.role] };
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    return user;
  }

  private decodePayload(token: string): JwtPayload | null {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(atob(base64)) as JwtPayload;
    } catch { return null; }
  }
}