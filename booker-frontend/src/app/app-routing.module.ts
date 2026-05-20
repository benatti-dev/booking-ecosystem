import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guard';

const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  {
    path: 'home',
    loadChildren: () => import('./features/home/home.module').then(m => m.HomeModule),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadChildren: () => import('./features/dashboard/dashboard.module').then(m => m.DashboardModule),
  },
  {
    path: 'business',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['BUSINESS_OWNER', 'ADMIN'] },
    loadChildren: () =>
      import('./features/business-admin/business-admin.module').then(m => m.BusinessAdminModule),
  },
  {
    path: 'business/:businessId',
    loadChildren: () =>
      import('./features/booking/booking-detail.module').then(m => m.BookingDetailModule),
  },
  {
    path: 'booking',
    canActivate: [authGuard],
    loadChildren: () => import('./features/booking/booking.module').then(m => m.BookingModule),
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadChildren: () => import('./features/admin/admin.module').then(m => m.AdminModule),
  },
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/notifications/notifications.module').then(m => m.NotificationsModule),
  },
  // Auth routes (login / register) — empty prefix resolved by AuthRoutingModule
  {
    path: '',
    loadChildren: () => import('./features/auth/auth.module').then(m => m.AuthModule),
  },
  { path: '**', redirectTo: '/home' },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
