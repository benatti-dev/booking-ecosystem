import { createFeature, createReducer, on } from '@ngrx/store';
import { AdminUserResponse, AuditLogResponse } from '../../core/admin/admin.service';
import { PlatformOverviewResponse } from '../../core/analytics/analytics.service';
import { AdminActions } from './admin.actions';

export interface AdminState {
  // Platform overview
  overview: PlatformOverviewResponse | null;
  loadingOverview: boolean;
  errorOverview: string | null;

  // Users
  users: AdminUserResponse[];
  usersTotal: number;
  usersTotalPages: number;
  loadingUsers: boolean;
  errorUsers: string | null;

  // Audit logs
  auditLogs: AuditLogResponse[];
  auditLogsTotal: number;
  auditLogsTotalPages: number;
  loadingAuditLogs: boolean;
  errorAuditLogs: string | null;
}

const initialState: AdminState = {
  overview: null,
  loadingOverview: false,
  errorOverview: null,

  users: [],
  usersTotal: 0,
  usersTotalPages: 0,
  loadingUsers: false,
  errorUsers: null,

  auditLogs: [],
  auditLogsTotal: 0,
  auditLogsTotalPages: 0,
  loadingAuditLogs: false,
  errorAuditLogs: null,
};

export const adminFeature = createFeature({
  name: 'admin',
  reducer: createReducer(
    initialState,

    // Platform overview
    on(AdminActions.loadPlatformOverview, state => ({
      ...state, loadingOverview: true, errorOverview: null,
    })),
    on(AdminActions.loadPlatformOverviewSuccess, (state, { overview }) => ({
      ...state, overview, loadingOverview: false,
    })),
    on(AdminActions.loadPlatformOverviewFailure, (state, { error }) => ({
      ...state, loadingOverview: false, errorOverview: error,
    })),

    // Users
    on(AdminActions.loadUsers, state => ({
      ...state, loadingUsers: true, errorUsers: null,
    })),
    on(AdminActions.loadUsersSuccess, (state, { users, total, totalPages }) => ({
      ...state, users, usersTotal: total, usersTotalPages: totalPages, loadingUsers: false,
    })),
    on(AdminActions.loadUsersFailure, (state, { error }) => ({
      ...state, loadingUsers: false, errorUsers: error,
    })),

    // Optimistic update: reflect status change immediately in the list
    on(AdminActions.changeUserStatusSuccess, (state, { user }) => ({
      ...state,
      users: state.users.map(u => u.id === user.id ? user : u),
    })),

    // Audit logs
    on(AdminActions.loadAuditLogs, state => ({
      ...state, loadingAuditLogs: true, errorAuditLogs: null,
    })),
    on(AdminActions.loadAuditLogsSuccess, (state, { logs, total, totalPages }) => ({
      ...state, auditLogs: logs, auditLogsTotal: total, auditLogsTotalPages: totalPages, loadingAuditLogs: false,
    })),
    on(AdminActions.loadAuditLogsFailure, (state, { error }) => ({
      ...state, loadingAuditLogs: false, errorAuditLogs: error,
    })),
  ),
});
