import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { AuthActions } from './auth.actions';

@Injectable()
export class AuthEffects {
  private readonly actions$ = inject(Actions);
  private readonly auth     = inject(AuthService);
  private readonly router   = inject(Router);

  verifySession$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.verifySession),
      switchMap(() =>
        this.auth.verifySession().pipe(
          map(user => AuthActions.verifySessionSuccess({ user })),
          catchError(()  => of(AuthActions.verifySessionFailure())),
        )
      ),
    )
  );

  login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.login),
      switchMap(({ email, password }) =>
        this.auth.login(email, password).pipe(
          map(user  => AuthActions.loginSuccess({ user })),
          catchError(err => of(AuthActions.loginFailure({
            error: err.status === 401
              ? 'Invalid email or password'
              : 'Login failed. Please try again.',
          }))),
        )
      ),
    )
  );

  register$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.register),
      switchMap(({ req }) =>
        this.auth.register(req).pipe(
          map(user  => AuthActions.registerSuccess({ user })),
          catchError(err => of(AuthActions.registerFailure({
            error: err.status === 409
              ? 'An account with this email already exists'
              : (err.error?.fieldErrors?.[0]?.message ?? 'Registration failed. Please try again.'),
          }))),
        )
      ),
    )
  );

  navigateAfterAuth$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.loginSuccess, AuthActions.registerSuccess),
        tap(() => this.router.navigateByUrl('/dashboard')),
      ),
    { dispatch: false },
  );

  logout$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.logout),
        tap(() => this.auth.logout()),
      ),
    { dispatch: false },
  );
}
