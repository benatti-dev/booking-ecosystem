import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { map, take, combineLatest } from 'rxjs';
import { selectIsAuthenticated, selectUserRoles } from '../../store/auth/auth.selectors';

/** Redirects authenticated users away from guest-only pages (login, register). */
export const guestGuard: CanActivateFn = () => {
  const store  = inject(Store);
  const router = inject(Router);
  return store.select(selectIsAuthenticated).pipe(
    take(1),
    map(isAuth => isAuth ? router.createUrlTree(['/dashboard']) : true),
  );
};

/** Redirects to /login when the user is not authenticated. */
export const authGuard: CanActivateFn = () => {
  const store  = inject(Store);
  const router = inject(Router);
  return store.select(selectIsAuthenticated).pipe(
    take(1),
    map(isAuth => isAuth ? true : router.createUrlTree(['/login'])),
  );
};

/** Checks that the authenticated user has at least one of the required roles. */
export const roleGuard: CanActivateFn = (route) => {
  const store    = inject(Store);
  const router   = inject(Router);
  const required = (route.data['roles'] as string[] | undefined) ?? [];

  return combineLatest([
    store.select(selectIsAuthenticated),
    store.select(selectUserRoles),
  ]).pipe(
    take(1),
    map(([isAuth, roles]) => {
      if (!isAuth) return router.createUrlTree(['/login']);
      if (required.length === 0 || required.some(r => roles.includes(r))) return true;
      return router.createUrlTree(['/home']);
    }),
  );
};

