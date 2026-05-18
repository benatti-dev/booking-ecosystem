import { businessFeature } from './business.reducer';

export const {
  selectMyBusinesses,
  selectAllBusinesses,
  selectActiveTab,
  selectLoadingMy,
  selectLoadingAll,
  selectErrorMy,
  selectErrorAll,
  selectPendingApprovals,
  selectLoadingPending,
  selectErrorPending,
} = businessFeature;
