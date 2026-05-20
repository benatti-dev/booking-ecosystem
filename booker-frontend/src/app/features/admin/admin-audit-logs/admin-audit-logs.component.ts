import { Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { AdminActions } from '../../../store/admin/admin.actions';
import {
  selectAuditLogs,
  selectAuditLogsTotal,
  selectAuditLogsTotalPages,
  selectLoadingAuditLogs,
  selectErrorAuditLogs,
} from '../../../store/admin/admin.selectors';
import { AuditLogResponse } from '../../../core/admin/admin.service';

/** Well-known audit action values matching the backend AuditLogService constants. */
const KNOWN_ACTIONS = [
  'LOGIN',
  'FAILED_LOGIN',
  'LOGOUT',
  'REGISTER',
  'PASSWORD_RESET_REQUEST',
  'PASSWORD_RESET',
] as const;

@Component({
  selector: 'app-admin-audit-logs',
  templateUrl: './admin-audit-logs.component.html',
  standalone: false,
})
export class AdminAuditLogsComponent implements OnInit, OnDestroy {

  logs: AuditLogResponse[] = [];
  total = 0;
  totalPages = 0;
  loading = false;
  error: string | null = null;

  currentPage = 0;
  filterAction = '';
  filterFrom = '';
  filterTo = '';

  readonly knownActions = ['', ...KNOWN_ACTIONS];

  private sub = new Subscription();

  constructor(private readonly store: Store) {}

  ngOnInit(): void {
    this.loadPage();
    this.sub.add(this.store.select(selectAuditLogs).subscribe(v => (this.logs = v)));
    this.sub.add(this.store.select(selectAuditLogsTotal).subscribe(v => (this.total = v)));
    this.sub.add(this.store.select(selectAuditLogsTotalPages).subscribe(v => (this.totalPages = v)));
    this.sub.add(this.store.select(selectLoadingAuditLogs).subscribe(v => (this.loading = v)));
    this.sub.add(this.store.select(selectErrorAuditLogs).subscribe(v => (this.error = v)));
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadPage();
  }

  resetFilters(): void {
    this.filterAction = '';
    this.filterFrom = '';
    this.filterTo = '';
    this.applyFilters();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadPage();
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  private loadPage(): void {
    // Convert local date string to ISO 8601 instant for the backend
    const from = this.filterFrom ? new Date(this.filterFrom).toISOString() : undefined;
    const to   = this.filterTo   ? new Date(this.filterTo + 'T23:59:59').toISOString() : undefined;

    this.store.dispatch(AdminActions.loadAuditLogs({
      page:   this.currentPage,
      action: this.filterAction || undefined,
      from,
      to,
    }));
  }

  actionBadgeClass(action: string): string {
    if (action === 'FAILED_LOGIN') return 'px-2 py-0.5 rounded bg-red-100 text-red-600';
    if (action === 'LOGIN')        return 'px-2 py-0.5 rounded bg-green-100 text-green-700';
    if (action === 'LOGOUT')       return 'px-2 py-0.5 rounded bg-gray-100 text-gray-600';
    return 'px-2 py-0.5 rounded bg-indigo-50 text-indigo-700';
  }
}
