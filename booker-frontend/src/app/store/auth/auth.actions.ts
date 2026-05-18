import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { AuthUser, RegisterRequest } from '../../core/auth/auth.service';

export const AuthActions = createActionGroup({
  source: 'Auth',
  events: {
    'Verify Session':           emptyProps(),
    'Verify Session Success':   props<{ user: AuthUser | null }>(),
    'Verify Session Failure':   emptyProps(),

    'Login':            props<{ email: string; password: string }>(),
    'Login Success':    props<{ user: AuthUser }>(),
    'Login Failure':    props<{ error: string }>(),

    'Register':           props<{ req: RegisterRequest }>(),
    'Register Success':   props<{ user: AuthUser }>(),
    'Register Failure':   props<{ error: string }>(),

    'Logout':       emptyProps(),
    'Clear Error':  emptyProps(),
  },
});
