import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { BusinessAdminRoutingModule } from './business-admin-routing.module';

import { BusinessAdminComponent } from './business-admin.component';
import { MyBusinessesComponent } from './my-businesses/my-businesses.component';
import { BusinessRegisterComponent } from './business-register/business-register.component';
import { ServiceListComponent } from './service-list/service-list.component';
import { ServiceFormComponent } from './service-form/service-form.component';
import { DynamicAttributeFormComponent } from './service-form/dynamic-attribute-form.component';
import { ScheduleManagementComponent } from './schedule-management/schedule-management.component';
import { BookingCalendarComponent } from './booking-calendar/booking-calendar.component';
import { EmployeeListComponent } from './employee-list/employee-list.component';
import { EmployeeFormComponent } from './employee-form/employee-form.component';
import { EmployeeScheduleComponent } from './employee-schedule/employee-schedule.component';
import { AnalyticsComponent } from './analytics/analytics.component';

@NgModule({
  declarations: [
    BusinessAdminComponent,
    MyBusinessesComponent,
    BusinessRegisterComponent,
    ServiceListComponent,
    ServiceFormComponent,
    DynamicAttributeFormComponent,
    ScheduleManagementComponent,
    BookingCalendarComponent,
    EmployeeListComponent,
    EmployeeFormComponent,
    EmployeeScheduleComponent,
    AnalyticsComponent,
  ],
  imports: [SharedModule, BusinessAdminRoutingModule],
})
export class BusinessAdminModule {}
