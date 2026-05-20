import { Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { AdminActions } from '../../../store/admin/admin.actions';
import {
  selectUsers,
  selectUsersTotal,
  selectUsersTotalPages,
  selectLoadingUsers,
  selectErrorUsers,
} from '../../../store/admin/admin.selectors';
import { AdminUserResponse, UserRole, UserStatus } from '../../../core/admin/admin.service';

@Component({
  selector: 'app-admin-users',
  templateUrl: './admin-users.component.html',
  standalone: false,
})
export class AdminUsersComponent implements OnInit, OnDestroy {

  users: AdminUserResponse[] = [];
  total = 0;
  totalPages = 0;
  loading = false;
  error: string | null = null;

  currentPage = 0;
  filterRole: UserRole | '' = '';
  filterStatus: UserStatus | '' = '';

  readonly roleOptions: Array<{ value: UserRole | ''; label: string }> = [
    { value: '',               label: 'All roles' },
    { value: 'CLIENT',         label: 'Client' },
    { value: 'BUSINESS_OWNER', label: 'Business Owner' },
    { value: 'EMPLOYEE',       label: 'Employee' },
    { value: 'ADMIN',          label: 'Admin' },
  ];

  readonly statusOptions: Array<{ value: UserStatus | ''; label: string }> = [
    { value: '',          label: 'All statuses' },
    { value: 'ACTIVE',    label: 'Active' },
    { value: 'SUSPENDED', label: 'Suspended' },
  ];

  private sub = new Subscription();

  constructor(private readonly store: Store) {}

  ngOnInit(): void {
    this.loadPage();
    this.sub.add(this.store.select(selectUsers).subscribe(v => (this.users = v)));
    this.sub.add(this.store.select(selectUsersTotal).subscribe(v => (this.total = v)));
    this.sub.add(this.store.select(selectUsersTotalPages).subscribe(v => (this.totalPages = v)));
    this.sub.add(this.store.select(selectLoadingUsers).subscribe(v => (this.loading = v)));
    this.sub.add(this.store.select(selectErrorUsers).subscribe(v => (this.error = v)));
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.loadPage();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadPage();
  }

  toggleStatus(user: AdminUserResponse): void {
    const newStatus: UserStatus = user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    this.store.dispatch(AdminActions.changeUserStatus({ userId: user.id, status: newStatus }));
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  private loadPage(): void {
    this.store.dispatch(AdminActions.loadUsers({
      page: this.currentPage,
      role:   this.filterRole   || undefined,
      status: this.filterStatus || undefined,
    }));
  }
}
