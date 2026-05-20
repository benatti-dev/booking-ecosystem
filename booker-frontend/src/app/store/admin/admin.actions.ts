import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { AdminUserResponse, AuditLogResponse, UserStatus } from '../../core/admin/admin.service';
import { PlatformOverviewResponse } from '../../core/analytics/analytics.service';

export const AdminActions = createActionGroup({
  source: 'Admin',
  events: {
    // Platform overview
    'Load Platform Overview':         emptyProps(),
    'Load Platform Overview Success': props<{ overview: PlatformOverviewResponse }>(),
    'Load Platform Overview Failure': props<{ error: string }>(),

    // Users
    'Load Users':         props<{ page?: number; role?: string; status?: string }>(),
    'Load Users Success': props<{ users: AdminUserResponse[]; total: number; totalPages: number }>(),
    'Load Users Failure': props<{ error: string }>(),

    'Change User Status':         props<{ userId: number; status: UserStatus }>(),
    'Change User Status Success': props<{ user: AdminUserResponse }>(),
    'Change User Status Failure': props<{ error: string }>(),

    // Audit logs
    'Load Audit Logs':         props<{ page?: number; action?: string; from?: string; to?: string }>(),
    'Load Audit Logs Success': props<{ logs: AuditLogResponse[]; total: number; totalPages: number }>(),
    'Load Audit Logs Failure': props<{ error: string }>(),
  },
});
