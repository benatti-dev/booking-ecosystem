import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { AuthActions } from '../../../store/auth/auth.actions';
import { selectUser } from '../../../store/auth/auth.selectors';
import { NotificationService } from '../../../core/notification/notification.service';
import { AuthService } from '../../../core/auth/auth.service';
import { AuthUser } from '../../../core/auth/auth.service';
import { NAV_ITEMS, NavItem } from '../../config/nav-config';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent implements OnInit, OnDestroy {
  user: AuthUser | null = null;
  unreadCount = 0;
  mobileMenuOpen = false;
  /** Pre-computed list rebuilt only when `user` changes — avoids recreating the
   *  array on every change-detection cycle that the getter pattern causes. */
  navItems: NavItem[] = [];

  private sub = new Subscription();

  constructor(
    private readonly store: Store,
    private readonly notifService: NotificationService,
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  get isAuthenticated(): boolean {
    return this.user !== null;
  }

  private computeNavItems(): void {
    const roles = this.user?.roles ?? [];
    this.navItems = NAV_ITEMS.filter(item => {
      if (item.authRequired && !this.user) return false;
      if (item.roles?.length && !item.roles.some(r => roles.includes(r))) return false;
      return true;
    });
  }

  ngOnInit(): void {
    this.sub.add(
      this.store.select(selectUser).subscribe(user => {
        this.user = user;
        this.computeNavItems();
        if (user) {
          const token = this.authService.getAccessToken();
          if (token) this.notifService.connect(token);
        } else {
          this.notifService.disconnect();
        }
      }),
    );

    this.sub.add(
      this.notifService.unreadCount$.subscribe(count => {
        this.unreadCount = count;
        // With OnPush, manually mark for check since this subscription runs
        // from a BehaviorSubject that may be updated outside Angular's zone.
        this.cdr.markForCheck();
      }),
    );
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen = false;
  }

  logout(): void {
    this.mobileMenuOpen = false;
    this.store.dispatch(AuthActions.logout());
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}