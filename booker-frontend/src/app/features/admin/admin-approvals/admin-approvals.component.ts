import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { BusinessResponse } from '../../../core/business/business.service';
import { BusinessActions } from '../../../store/business/business.actions';
import {
  selectPendingApprovals,
  selectLoadingPending,
  selectErrorPending,
} from '../../../store/business/business.selectors';

@Component({
  selector: 'app-admin-approvals',
  templateUrl: './admin-approvals.component.html',
  standalone: false,
})
export class AdminApprovalsComponent implements OnInit, OnDestroy {

  pending: BusinessResponse[] = [];
  loading = false;
  error: string | null = null;

  rejectingId = signal<number | null>(null);
  rejectReason = signal('');

  private sub = new Subscription();

  constructor(private readonly store: Store) {}

  ngOnInit(): void {
    this.store.dispatch(BusinessActions.loadPendingBusinesses());
    this.sub.add(this.store.select(selectPendingApprovals).subscribe(v => (this.pending = v ?? [])));
    this.sub.add(this.store.select(selectLoadingPending).subscribe(v => (this.loading = v)));
    this.sub.add(this.store.select(selectErrorPending).subscribe(v => (this.error = v)));
  }

  approve(id: number): void {
    this.store.dispatch(BusinessActions.changeBusinessStatus({ id, status: 'ACTIVE' }));
  }

  startReject(id: number): void {
    this.rejectingId.set(id);
    this.rejectReason.set('');
  }

  cancelReject(): void {
    this.rejectingId.set(null);
  }

  confirmReject(id: number): void {
    this.store.dispatch(BusinessActions.changeBusinessStatus({
      id,
      status: 'REJECTED',
      reason: this.rejectReason() || undefined,
    }));
    this.rejectingId.set(null);
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}
