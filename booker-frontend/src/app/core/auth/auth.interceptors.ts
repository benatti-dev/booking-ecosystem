import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { BehaviorSubject, Observable, filter, take } from 'rxjs';
import { catchError, switchMap, throwError } from 'rxjs';
import { Store } from '@ngrx/store';
import { AuthService } from './auth.service';
import { AuthActions } from '../../store/auth/auth.actions';
import { environment } from '../../../environments/environment';

/** Paths that must NOT receive the Authorization header. */
const AUTH_PATHS = [
  '/auth/login',
  '/auth/register',
  '/auth/refresh',
  '/auth/forgot-password',
  '/auth/reset-password',
];

const isAuthPath = (url: string): boolean => AUTH_PATHS.some(p => url.includes(p));

/** Attaches Bearer token to every request that goes to our API (except auth endpoints). */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(private readonly auth: AuthService) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!req.url.startsWith(environment.apiUrl) || isAuthPath(req.url)) {
      return next.handle(req);
    }

    const token = this.auth.getAccessToken();
    if (!token) return next.handle(req);

    return next.handle(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }
}

/** On 401: attempt a silent token refresh once, then replay the original request.
 *  On second 401 (refresh also failed): dispatch logout action.
 *
 *  The `isRefreshing` flag + `refreshDone$` subject ensure that concurrent
 *  requests that all receive a 401 share a single refresh call instead of
 *  each triggering their own, which would cause a loop or multiple revocations.
 */
@Injectable()
export class AuthErrorInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private readonly refreshDone$ = new BehaviorSubject<boolean>(false);

  constructor(
    private readonly auth: AuthService,
    private readonly store: Store,
  ) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError(err => {
        if (
          err.status !== 401 ||
          !req.url.startsWith(environment.apiUrl) ||
          isAuthPath(req.url)
        ) {
          return throwError(() => err);
        }

        if (this.isRefreshing) {
          // Another refresh is already in-flight; queue behind it
          return this.refreshDone$.pipe(
            filter(done => done),
            take(1),
            switchMap(() => {
              const token = this.auth.getAccessToken();
              const retried = token
                ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
                : req;
              return next.handle(retried);
            }),
          );
        }

        this.isRefreshing = true;
        this.refreshDone$.next(false);

        return this.auth.refreshToken().pipe(
          switchMap(() => {
            this.isRefreshing = false;
            this.refreshDone$.next(true);
            const token = this.auth.getAccessToken();
            const retried = token
              ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
              : req;
            return next.handle(retried);
          }),
          catchError(refreshErr => {
            this.isRefreshing = false;
            this.refreshDone$.next(false);
            this.store.dispatch(AuthActions.logout());
            return throwError(() => refreshErr);
          }),
        );
      }),
    );
  }
}

/** Global HTTP error interceptor.
 *  Logs server errors (5xx), forbidden (403) and network failures.
 *  401 handling is covered by AuthErrorInterceptor. */
@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(req).pipe(
      catchError(err => {
        if (req.url.startsWith(environment.apiUrl) && err.status !== 401) {
          if (err.status === 0) {
            console.error('[HTTP] Network error – server unreachable', req.url);
          } else if (err.status === 403) {
            console.warn('[HTTP] 403 Forbidden:', req.url);
          } else if (err.status >= 500) {
            console.error(`[HTTP] ${err.status} Server error:`, req.url);
          }
        }
        return throwError(() => err);
      }),
    );
  }
}
