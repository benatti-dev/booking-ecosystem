export interface NavItem {
  label: string;
  path: string;
  /** Require the user to be logged in */
  authRequired?: boolean;
  /** If set, only users with at least one of these roles see this item */
  roles?: string[];
}

/** Ordered list of navigation items.
 *  Items with no constraints are always visible.
 *  Items with authRequired=true are hidden for guests.
 *  Items with roles are additionally filtered by the current user's role list.
 */
export const NAV_ITEMS: NavItem[] = [
  { label: 'Browse',        path: '/home' },
  { label: 'My Bookings',   path: '/dashboard',    authRequired: true },
  { label: 'My Business',   path: '/business',     authRequired: true, roles: ['BUSINESS_OWNER', 'ADMIN'] },
  { label: 'Admin Panel',   path: '/admin',        authRequired: true, roles: ['ADMIN'] },
];
