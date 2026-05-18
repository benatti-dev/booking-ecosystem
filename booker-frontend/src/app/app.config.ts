import { ApplicationConfig, APP_INITIALIZER, isDevMode, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideStore, provideState, Store } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { filter, take } from 'rxjs';

import { jwtInterceptor, authErrorInterceptor } from './core/auth/auth.interceptors';
import { authFeature } from './store/auth/auth.reducer';
import { businessFeature } from './store/business/business.reducer';
import { AuthEffects } from './store/auth/auth.effects';
import { BusinessEffects } from './store/business/business.effects';
import { AuthActions } from './store/auth/auth.actions';
import { selectInitialized } from './store/auth/auth.selectors';
import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideAnimationsAsync(),
    provideHttpClient(
      withInterceptors([jwtInterceptor, authErrorInterceptor])
    ),
    provideStore(),
    provideState(authFeature),
    provideState(businessFeature),
    provideEffects(AuthEffects, BusinessEffects),
    provideStoreDevtools({ maxAge: 25, logOnly: !isDevMode(), connectInZone: true }),
    {
      provide: APP_INITIALIZER,
      useFactory: (store: Store) => () => {
        store.dispatch(AuthActions.verifySession());
        return store.select(selectInitialized).pipe(
          filter(initialized => initialized),
          take(1),
        );
      },
      deps: [Store],
      multi: true,
    },
  ]
};
