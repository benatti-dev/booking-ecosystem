import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, switchMap, throwError } from 'rxjs';
import { Store } from '@ngrx/store';
import { AuthService } from './auth.service';
import { AuthActions } from '../../store/auth/auth.actions';
import { environment } from '../../../environments/environment';

/** Paths that must NOT receive the Authorization header. */
const AUTH_PATHS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/forgot-password', '/auth/reset-password'];

const isAuthPath = (url: string) =>
  AUTH_PATHS.some(p => url.includes(p));

/** Attaches Bearer token to every request that goes to our API (except auth endpoints). */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  if (!req.url.startsWith(environment.apiUrl) || isAuthPath(req.url)) {
    return next(req);
  }

  const token = auth.getAccessToken();
  if (!token) return next(req);

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};

/** On 401: attempt a silent token refresh once, then replay the original request.
 *  On second 401 (refresh also failed): dispatch logout action. */
export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth  = inject(AuthService);
  const store = inject(Store);

  return next(req).pipe(
    catchError(err => {
      if (err.status !== 401 || !req.url.startsWith(environment.apiUrl) || isAuthPath(req.url)) {
        return throwError(() => err);
      }

      return auth.refreshToken().pipe(
        switchMap(() => {
          const token = auth.getAccessToken();
          const retried = token
            ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
            : req;
          return next(retried);
        }),
        catchError(refreshErr => {
          store.dispatch(AuthActions.logout());
          return throwError(() => refreshErr);
        }),
      );
    }),
  );
};
