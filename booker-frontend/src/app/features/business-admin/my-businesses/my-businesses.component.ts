import { Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subscription, take } from 'rxjs';
import { BusinessActions } from '../../../store/business/business.actions';
import {
  selectMyBusinesses, selectAllBusinesses, selectActiveTab,
  selectLoadingMy, selectLoadingAll, selectErrorMy, selectErrorAll,
} from '../../../store/business/business.selectors';
import { selectIsAdmin } from '../../../store/auth/auth.selectors';
import { BusinessResponse } from '../../../core/business/business.service';

@Component({
  selector: 'app-my-businesses',
  templateUrl: './my-businesses.component.html',
  standalone: false,
})
export class MyBusinessesComponent implements OnInit, OnDestroy {
  isAdmin = false;
  myList: BusinessResponse[] = [];
  allList: BusinessResponse[] = [];
  activeTab: 'my' | 'all' = 'my';
  loadingMy = false;
  loadingAll = false;
  errorMy: string | null = null;
  errorAll: string | null = null;

  private sub = new Subscription();

  constructor(private readonly store: Store) {}

  ngOnInit(): void {
    this.sub.add(this.store.select(selectIsAdmin).subscribe(v => (this.isAdmin = v)));
    this.sub.add(this.store.select(selectMyBusinesses).subscribe(v => (this.myList = v ?? [])));
    this.sub.add(this.store.select(selectAllBusinesses).subscribe(v => (this.allList = v ?? [])));
    this.sub.add(this.store.select(selectActiveTab).subscribe(v => (this.activeTab = v)));
    this.sub.add(this.store.select(selectLoadingMy).subscribe(v => (this.loadingMy = v)));
    this.sub.add(this.store.select(selectLoadingAll).subscribe(v => (this.loadingAll = v)));
    this.sub.add(this.store.select(selectErrorMy).subscribe(v => (this.errorMy = v)));
    this.sub.add(this.store.select(selectErrorAll).subscribe(v => (this.errorAll = v)));

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

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}

