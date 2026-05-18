import { createSelector } from '@ngrx/store';
import { authFeature } from './auth.reducer';

export const {
  selectUser,
  selectInitialized,
  selectLoading:     selectAuthLoading,
  selectError:       selectAuthError,
} = authFeature;

export const selectIsAuthenticated = createSelector(
  selectUser, user => user !== null
);

export const selectUserRoles = createSelector(
  selectUser, user => user?.roles ?? []
);

export const selectIsAdmin = createSelector(
  selectUserRoles, roles => roles.includes('ADMIN')
);

export const selectUserEmail = createSelector(
  selectUser, user => user?.email ?? null
);
