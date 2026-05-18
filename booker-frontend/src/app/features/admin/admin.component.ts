import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Store } from '@ngrx/store';
import { BusinessActions } from '../../store/business/business.actions';
import {
  selectPendingApprovals,
  selectLoadingPending,
  selectErrorPending,
} from '../../store/business/business.selectors';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent implements OnInit {
  private readonly store = inject(Store);

  readonly pending$  = this.store.select(selectPendingApprovals);
  readonly loading$  = this.store.select(selectLoadingPending);
  readonly error$    = this.store.select(selectErrorPending);

  ngOnInit(): void {
    this.store.dispatch(BusinessActions.loadPendingBusinesses());
  }

  approve(id: number): void {
    this.store.dispatch(BusinessActions.changeBusinessStatus({ id, status: 'ACTIVE' }));
  }

  reject(id: number): void {
    const reason = prompt('Reason for rejection (optional):') ?? undefined;
    this.store.dispatch(BusinessActions.changeBusinessStatus({ id, status: 'REJECTED', reason }));
  }
}
