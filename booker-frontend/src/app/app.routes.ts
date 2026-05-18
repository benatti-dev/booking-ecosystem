import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/home',
    pathMatch: 'full'
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'home',
    loadComponent: () =>
      import('./features/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
  },
  {
    path: 'business',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['BUSINESS_OWNER', 'ADMIN'] },
    loadComponent: () =>
      import('./features/business-admin/business-admin.component').then(m => m.BusinessAdminComponent),
    children: [
      {
        path: '',
        redirectTo: 'my-businesses',
        pathMatch: 'full'
      },
      {
        path: 'my-businesses',
        loadComponent: () =>
          import('./features/business-admin/my-businesses/my-businesses.component').then(m => m.MyBusinessesComponent)
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./features/business-admin/business-register/business-register.component').then(m => m.BusinessRegisterComponent)
      },
      {
        path: ':businessId/services',
        loadComponent: () =>
          import('./features/business-admin/service-list/service-list.component').then(m => m.ServiceListComponent)
      },
      {
        path: ':businessId/services/new',
        loadComponent: () =>
          import('./features/business-admin/service-form/service-form.component').then(m => m.ServiceFormComponent)
      }
    ]
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMIN'] },
    loadComponent: () =>
      import('./features/admin/admin.component').then(m => m.AdminComponent)
  },
  {
    path: 'business/:businessId',
    loadComponent: () =>
      import('./features/booking/business-detail/business-detail.component').then(m => m.BusinessDetailComponent)
  },
  {
    path: 'booking/confirm',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/booking/booking-confirm/booking-confirm.component').then(m => m.BookingConfirmComponent)
  },
  {
    path: 'booking/success',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/booking/booking-success/booking-success.component').then(m => m.BookingSuccessComponent)
  },
  {
    path: '**',
    redirectTo: '/home'
  }
];


