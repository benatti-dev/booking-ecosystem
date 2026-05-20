import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { AdminRoutingModule } from './admin-routing.module';
import { AdminComponent } from './admin.component';
import { AdminOverviewComponent } from './admin-overview/admin-overview.component';
import { AdminApprovalsComponent } from './admin-approvals/admin-approvals.component';
import { AdminUsersComponent } from './admin-users/admin-users.component';
import { AdminAuditLogsComponent } from './admin-audit-logs/admin-audit-logs.component';

@NgModule({
  declarations: [
    AdminComponent,
    AdminOverviewComponent,
    AdminApprovalsComponent,
    AdminUsersComponent,
    AdminAuditLogsComponent,
  ],
  imports: [SharedModule, AdminRoutingModule],
})
export class AdminModule {}
