import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router, NavigationEnd, ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { filter } from 'rxjs/operators';

interface NavItem { label: string; icon: string; path: string; }

@Component({
  selector: 'app-business-admin',
  templateUrl: './business-admin.component.html',
  styleUrl: './business-admin.component.scss',
  standalone: false,
})
export class BusinessAdminComponent implements OnInit, OnDestroy {
  currentBusinessId: number | null = null;
  navItems: NavItem[] = [];

  private sub = new Subscription();

  constructor(
    private readonly router: Router,
    private readonly route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.currentBusinessId = this.extractBusinessId();
    this.updateNavItems();

    this.sub.add(
      this.router.events
        .pipe(filter(e => e instanceof NavigationEnd))
        .subscribe(() => {
          const id = this.extractBusinessId();
          if (id !== this.currentBusinessId) {
            this.currentBusinessId = id;
            this.updateNavItems();
          }
        })
    );
  }

  private updateNavItems(): void {
    const id = this.currentBusinessId;
    if (id) {
      this.navItems = [
        { label: 'My Businesses', icon: '🏢', path: '/business/my-businesses' },
        { label: 'Services',      icon: '🛠️', path: `/business/${id}/services` },
        { label: 'Bookings',      icon: '📅', path: `/business/${id}/bookings` },
        { label: 'Employees',     icon: '👥', path: `/business/${id}/employees` },
      ];
    } else {
      this.navItems = [
        { label: 'My Businesses',     icon: '🏢', path: '/business/my-businesses' },
        { label: 'Register Business', icon: '➕', path: '/business/register' },
      ];
    }
  }

  private extractBusinessId(): number | null {
    let child = this.route.firstChild;
    while (child) {
      const id = child.snapshot.paramMap.get('businessId');
      if (id) return +id;
      child = child.firstChild;
    }
    return null;
  }

  ngOnDestroy(): void { this.sub.unsubscribe(); }
}