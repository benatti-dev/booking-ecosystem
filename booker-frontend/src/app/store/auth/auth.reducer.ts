import { createFeature, createReducer, on } from '@ngrx/store';
import { AuthUser } from '../../core/auth/auth.service';
import { AuthActions } from './auth.actions';

export interface AuthState {
  user:        AuthUser | null;
  initialized: boolean;
  loading:     boolean;
  error:       string | null;
}

const initialState: AuthState = {
  user:        null,
  initialized: false,
  loading:     false,
  error:       null,
};

export const authFeature = createFeature({
  name: 'auth',
  reducer: createReducer(
    initialState,

    on(AuthActions.verifySession, state => ({ ...state, loading: true })),
    on(AuthActions.verifySessionSuccess, (state, { user }) => ({
      ...state, user, loading: false, initialized: true,
    })),
    on(AuthActions.verifySessionFailure, state => ({
      ...state, user: null, loading: false, initialized: true,
    })),

    on(AuthActions.login, state => ({ ...state, loading: true, error: null })),
    on(AuthActions.loginSuccess, (state, { user }) => ({
      ...state, user, loading: false, error: null,
    })),
    on(AuthActions.loginFailure, (state, { error }) => ({
      ...state, loading: false, error,
    })),

    on(AuthActions.register, state => ({ ...state, loading: true, error: null })),
    on(AuthActions.registerSuccess, (state, { user }) => ({
      ...state, user, loading: false, error: null,
    })),
    on(AuthActions.registerFailure, (state, { error }) => ({
      ...state, loading: false, error,
    })),

    on(AuthActions.logout,     state => ({ ...state, user: null })),
    on(AuthActions.clearError, state => ({ ...state, error: null })),
  ),
});
