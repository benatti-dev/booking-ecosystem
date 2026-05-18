import { createFeature, createReducer, on } from '@ngrx/store';
import { BusinessResponse } from '../../core/business/business.service';
import { BusinessActions } from './business.actions';

export interface BusinessState {
  myBusinesses:    BusinessResponse[];
  allBusinesses:   BusinessResponse[];
  activeTab:       'my' | 'all';
  loadingMy:       boolean;
  loadingAll:      boolean;
  errorMy:         string | null;
  errorAll:        string | null;
  pendingApprovals: BusinessResponse[];
  loadingPending:  boolean;
  errorPending:    string | null;
}

const initialState: BusinessState = {
  myBusinesses:    [],
  allBusinesses:   [],
  activeTab:       'my',
  loadingMy:       false,
  loadingAll:      false,
  errorMy:         null,
  errorAll:        null,
  pendingApprovals: [],
  loadingPending:  false,
  errorPending:    null,
};

export const businessFeature = createFeature({
  name: 'business',
  reducer: createReducer(
    initialState,

    on(BusinessActions.loadMyBusinesses, state => ({ ...state, loadingMy: true, errorMy: null })),
    on(BusinessActions.loadMyBusinessesSuccess, (state, { businesses }) => ({
      ...state, myBusinesses: businesses, loadingMy: false,
    })),
    on(BusinessActions.loadMyBusinessesFailure, (state, { error }) => ({
      ...state, loadingMy: false, errorMy: error,
    })),

    on(BusinessActions.loadAllBusinesses, state => ({ ...state, loadingAll: true, errorAll: null })),
    on(BusinessActions.loadAllBusinessesSuccess, (state, { businesses }) => ({
      ...state, allBusinesses: businesses, loadingAll: false,
    })),
    on(BusinessActions.loadAllBusinessesFailure, (state, { error }) => ({
      ...state, loadingAll: false, errorAll: error,
    })),

    on(BusinessActions.switchTab, (state, { tab }) => ({ ...state, activeTab: tab })),

    on(BusinessActions.loadPendingBusinesses, state => ({
      ...state, loadingPending: true, errorPending: null,
    })),
    on(BusinessActions.loadPendingBusinessesSuccess, (state, { businesses }) => ({
      ...state, pendingApprovals: businesses, loadingPending: false,
    })),
    on(BusinessActions.loadPendingBusinessesFailure, (state, { error }) => ({
      ...state, loadingPending: false, errorPending: error,
    })),

    on(BusinessActions.changeBusinessStatusSuccess, (state, { id }) => ({
      ...state,
      pendingApprovals: state.pendingApprovals.filter(b => b.id !== id),
    })),
  ),
});
