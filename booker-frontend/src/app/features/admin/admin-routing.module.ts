import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AdminComponent } from './admin.component';
import { AdminOverviewComponent } from './admin-overview/admin-overview.component';
import { AdminApprovalsComponent } from './admin-approvals/admin-approvals.component';
import { AdminUsersComponent } from './admin-users/admin-users.component';
import { AdminAuditLogsComponent } from './admin-audit-logs/admin-audit-logs.component';

const routes: Routes = [
  {
    path: '',
    component: AdminComponent,
    children: [
      { path: '',            redirectTo: 'overview', pathMatch: 'full' },
      { path: 'overview',   component: AdminOverviewComponent },
      { path: 'approvals',  component: AdminApprovalsComponent },
      { path: 'users',      component: AdminUsersComponent },
      { path: 'audit-logs', component: AdminAuditLogsComponent },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class AdminRoutingModule {}
