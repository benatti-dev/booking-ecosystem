import { Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { AdminActions } from '../../../store/admin/admin.actions';
import {
  selectOverview,
  selectLoadingOverview,
  selectErrorOverview,
} from '../../../store/admin/admin.selectors';
import { PlatformOverviewResponse } from '../../../core/analytics/analytics.service';

@Component({
  selector: 'app-admin-overview',
  templateUrl: './admin-overview.component.html',
  standalone: false,
})
export class AdminOverviewComponent implements OnInit, OnDestroy {

  overview: PlatformOverviewResponse | null = null;
  loading = false;
  error: string | null = null;

  private sub = new Subscription();

  constructor(private readonly store: Store) {}

  ngOnInit(): void {
    this.store.dispatch(AdminActions.loadPlatformOverview());
    this.sub.add(this.store.select(selectOverview).subscribe(v => (this.overview = v)));
    this.sub.add(this.store.select(selectLoadingOverview).subscribe(v => (this.loading = v)));
    this.sub.add(this.store.select(selectErrorOverview).subscribe(v => (this.error = v)));
  }

  get totalBusinesses(): number {
    if (!this.overview) return 0;
    return (
      this.overview.totalBusinessesActive +
      this.overview.totalBusinessesPending +
      this.overview.totalBusinessesSuspended +
      this.overview.totalBusinessesRejected
    );
  }

  get totalUsers(): number {
    if (!this.overview) return 0;
    return (
      this.overview.totalClients +
      this.overview.totalBusinessOwners +
      this.overview.totalEmployees +
      this.overview.totalAdmins
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}
