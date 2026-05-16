import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Redirects to /login when the user is not authenticated. */
export const authGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);
  return auth.isAuthenticated() ? true : router.createUrlTree(['/login']);
};

/** Checks that the authenticated user has at least one of the required roles.
 *  Expects `data.roles: string[]` on the route definition.
 *  Redirects to /home when the role check fails.
 */
export const roleGuard: CanActivateFn = (route) => {
  const auth     = inject(AuthService);
  const router   = inject(Router);
  const required = (route.data['roles'] as string[] | undefined) ?? [];
  if (!auth.isAuthenticated()) return router.createUrlTree(['/login']);
  if (required.length === 0 || auth.hasAnyRole(...required)) return true;
  return router.createUrlTree(['/home']);
};
