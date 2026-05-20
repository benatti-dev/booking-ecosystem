import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BusinessAdminComponent } from './business-admin.component';
import { MyBusinessesComponent } from './my-businesses/my-businesses.component';
import { BusinessRegisterComponent } from './business-register/business-register.component';
import { ServiceListComponent } from './service-list/service-list.component';
import { ServiceFormComponent } from './service-form/service-form.component';
import { BookingCalendarComponent } from './booking-calendar/booking-calendar.component';
import { EmployeeListComponent } from './employee-list/employee-list.component';
import { EmployeeFormComponent } from './employee-form/employee-form.component';
import { EmployeeScheduleComponent } from './employee-schedule/employee-schedule.component';
import { AnalyticsComponent } from './analytics/analytics.component';

const routes: Routes = [
  {
    path: '',
    component: BusinessAdminComponent,
    children: [
      { path: '', redirectTo: 'my-businesses', pathMatch: 'full' },
      { path: 'my-businesses', component: MyBusinessesComponent },
      { path: 'register', component: BusinessRegisterComponent },
      { path: ':businessId/services', component: ServiceListComponent },
      { path: ':businessId/services/new', component: ServiceFormComponent },
      { path: ':businessId/bookings', component: BookingCalendarComponent },
      { path: ':businessId/employees', component: EmployeeListComponent },
      { path: ':businessId/employees/new', component: EmployeeFormComponent },
      { path: ':businessId/employees/:employeeId/schedule', component: EmployeeScheduleComponent },
      { path: ':businessId/analytics', component: AnalyticsComponent },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BusinessAdminRoutingModule {}
