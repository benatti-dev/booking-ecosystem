import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, map, Observable, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';

// ── Domain types ──────────────────────────────────────────────────────────────

export interface AuthUser {
  id: number;
  email: string;
  fullName: string;
  roles: string[];   // e.g. ['ROLE_CLIENT']
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

// ── JWT payload as issued by our backend ─────────────────────────────────────

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

// ── Service ───────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly ACCESS_KEY  = 'booker_token';
  private readonly REFRESH_KEY = 'booker_refresh';
  private readonly api         = environment.apiUrl;

  private readonly http   = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _user$ = new BehaviorSubject<AuthUser | null>(null);

  /** Emits the currently authenticated user, or null when logged out. */
  readonly user$            = this._user$.asObservable();
  readonly isAuthenticated$ = this._user$.pipe(map(u => u !== null));

  constructor() {
    // Restore state from a stored access token (e.g. after page refresh)
    this.hydrateFromStorage();
  }

  // ── Auth operations ─────────────────────────────────────────────────────────

  login(email: string, password: string): Observable<void> {
    return this.http
      .post<AuthResponse>(`${this.api}/auth/login`, { username: email, password })
      .pipe(
        tap(res => this.storeTokens(res)),
        map(() => void 0)
      );
  }

  register(req: RegisterRequest): Observable<void> {
    return this.http
      .post<AuthResponse>(`${this.api}/auth/register`, req)
      .pipe(
        tap(res => this.storeTokens(res)),
        map(() => void 0)
      );
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      // Fire-and-forget: revoke on backend; don't block local cleanup on error
      this.http
        .post(`${this.api}/auth/logout`, { refreshToken })
        .pipe(catchError(() => throwError(() => null)))
        .subscribe({ error: () => {} });
    }
    this.clearSession();
    this.router.navigateByUrl('/login');
  }

  refreshToken(): Observable<void> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      this.clearSession();
      return throwError(() => new Error('No refresh token'));
    }
    return this.http
      .post<AuthResponse>(`${this.api}/auth/refresh`, { refreshToken })
      .pipe(
        tap(res => this.storeTokens(res)),
        map(() => void 0)
      );
  }

  // ── Token access ────────────────────────────────────────────────────────────

  getAccessToken(): string | null {
    return localStorage.getItem(this.ACCESS_KEY);
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_KEY);
  }

  isAuthenticated(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;
    const payload = this.decodePayload(token);
    return !!payload && payload.exp * 1000 > Date.now();
  }

  // ── Role helpers ────────────────────────────────────────────────────────────

  hasRole(role: string): boolean {
    return this._user$.value?.roles.includes(role) ?? false;
  }

  hasAnyRole(...roles: string[]): boolean {
    return roles.some(r => this.hasRole(r));
  }

  // ── Private helpers ─────────────────────────────────────────────────────────

  private storeTokens(res: AuthResponse): void {
    localStorage.setItem(this.ACCESS_KEY, res.accessToken);
    localStorage.setItem(this.REFRESH_KEY, res.refreshToken);
    this.hydrateFromStorage();
  }

  private clearSession(): void {
    localStorage.removeItem(this.ACCESS_KEY);
    localStorage.removeItem(this.REFRESH_KEY);
    this._user$.next(null);
  }

  private hydrateFromStorage(): void {
    const token = this.getAccessToken();
    if (!token) { this._user$.next(null); return; }
    const payload = this.decodePayload(token);
    if (!payload || payload.exp * 1000 <= Date.now()) {
      this.clearSession();
      return;
    }
    this._user$.next({
      id:       payload.userId,
      email:    payload.email ?? payload.sub,
      fullName: payload.fullName ?? '',
      roles:    payload.roles ?? [],
    });
  }

  private decodePayload(token: string): JwtPayload | null {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(atob(base64)) as JwtPayload;
    } catch {
      return null;
    }
  }
}
