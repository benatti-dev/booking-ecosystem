import { Component } from '@angular/core';

/**
 * Admin shell component — renders the side-tab navigation and a <router-outlet>
 * for child routes (overview, approvals, users, audit-logs).
 */
@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
  standalone: false,
})
export class AdminComponent {

  readonly tabs = [
    { label: 'Overview',       path: 'overview',   icon: '📊' },
    { label: 'Approvals',      path: 'approvals',  icon: '✅' },
    { label: 'Users',          path: 'users',       icon: '👥' },
    { label: 'Audit Logs',     path: 'audit-logs', icon: '🗒️' },
  ];
}

