import { adminFeature } from './admin.reducer';

export const {
  selectOverview,
  selectLoadingOverview,
  selectErrorOverview,
  selectUsers,
  selectUsersTotal,
  selectUsersTotalPages,
  selectLoadingUsers,
  selectErrorUsers,
  selectAuditLogs,
  selectAuditLogsTotal,
  selectAuditLogsTotalPages,
  selectLoadingAuditLogs,
  selectErrorAuditLogs,
} = adminFeature;
