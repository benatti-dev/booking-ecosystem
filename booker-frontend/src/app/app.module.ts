import { APP_INITIALIZER, isDevMode, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { filter, take } from 'rxjs';
import { Store, StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';

import { App } from './app';
import { AppRoutingModule } from './app-routing.module';
import { SharedModule } from './shared/shared.module';

import { JwtInterceptor, AuthErrorInterceptor, HttpErrorInterceptor } from './core/auth/auth.interceptors';

import { authFeature } from './store/auth/auth.reducer';
import { AuthEffects } from './store/auth/auth.effects';
import { AuthActions } from './store/auth/auth.actions';
import { selectInitialized } from './store/auth/auth.selectors';

import { businessFeature } from './store/business/business.reducer';
import { BusinessEffects } from './store/business/business.effects';

import { bookingReducer } from './store/booking/booking.reducer';
import { BookingEffects } from './store/booking/booking.effects';

@NgModule({
  declarations: [App],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    AppRoutingModule,
    SharedModule,
    StoreModule.forRoot({}),
    EffectsModule.forRoot([]),
    StoreModule.forFeature(authFeature),
    StoreModule.forFeature(businessFeature),
    StoreModule.forFeature('booking', bookingReducer),
    EffectsModule.forFeature([AuthEffects, BusinessEffects, BookingEffects]),
    StoreDevtoolsModule.instrument({ maxAge: 25, logOnly: !isDevMode() }),
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: AuthErrorInterceptor, multi: true },
    { provide: HTTP_INTERCEPTORS, useClass: HttpErrorInterceptor, multi: true },
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
  ],
  bootstrap: [App],
})
export class AppModule {}
