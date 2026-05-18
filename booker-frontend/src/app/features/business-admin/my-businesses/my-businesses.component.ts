import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';
import { take } from 'rxjs';
import { BusinessActions } from '../../../store/business/business.actions';
import {
  selectMyBusinesses, selectAllBusinesses, selectActiveTab,
  selectLoadingMy, selectLoadingAll, selectErrorMy, selectErrorAll,
} from '../../../store/business/business.selectors';
import { selectIsAdmin } from '../../../store/auth/auth.selectors';
import { BusinessResponse } from '../../../core/business/business.service';

@Component({
  selector: 'app-my-businesses',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-businesses.component.html'
})
export class MyBusinessesComponent implements OnInit {
  private readonly store = inject(Store);

  readonly isAdmin$    = this.store.select(selectIsAdmin);
  readonly myList$     = this.store.select(selectMyBusinesses);
  readonly allList$    = this.store.select(selectAllBusinesses);
  readonly activeTab$  = this.store.select(selectActiveTab);
  readonly loadingMy$  = this.store.select(selectLoadingMy);
  readonly loadingAll$ = this.store.select(selectLoadingAll);
  readonly errorMy$    = this.store.select(selectErrorMy);
  readonly errorAll$   = this.store.select(selectErrorAll);

  ngOnInit(): void {
    this.store.select(selectIsAdmin).pipe(take(1)).subscribe(isAdmin => {
      if (isAdmin) {
        this.store.dispatch(BusinessActions.switchTab({ tab: 'all' }));
        this.store.dispatch(BusinessActions.loadAllBusinesses());
      } else {
        this.store.dispatch(BusinessActions.loadMyBusinesses());
      }
    });
  }

  switchTab(tab: 'my' | 'all'): void {
    this.store.dispatch(BusinessActions.switchTab({ tab }));
    if (tab === 'all') {
      this.store.dispatch(BusinessActions.loadAllBusinesses());
    } else {
      this.store.dispatch(BusinessActions.loadMyBusinesses());
    }
  }

  statusClass(status: string): string {
    return ({
      ACTIVE:    'bg-green-100 text-green-700',
      PENDING:   'bg-yellow-100 text-yellow-700',
      SUSPENDED: 'bg-orange-100 text-orange-700',
      REJECTED:  'bg-red-100 text-red-700',
    })[status] ?? 'bg-gray-100 text-gray-700';
  }

  trackById(_: number, b: BusinessResponse): number { return b.id; }
}


