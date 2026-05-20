import { Component, OnInit } from '@angular/core';
import { NotificationService, NotificationItem } from '../../core/notification/notification.service';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrl: './notifications.component.scss',
  standalone: false,
})
export class NotificationsComponent implements OnInit {
  notifications: NotificationItem[] = [];
  loading = false;
  page = 0;
  totalPages = 0;

  constructor(private readonly notifService: NotificationService) {}

  ngOnInit(): void {
    this.loadPage(0);
  }

  loadPage(p: number): void {
    this.loading = true;
    this.notifService.getNotifications(p, 20).subscribe({
      next: res => {
        this.notifications = res.content;
        this.page = res.number;
        this.totalPages = res.totalPages;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  markRead(n: NotificationItem): void {
    if (n.isRead) return;
    this.notifService.markAsRead(n.id).subscribe(() => {
      n.isRead = true;
      this.notifService.refreshUnreadCount();
    });
  }

  markAllRead(): void {
    this.notifService.markAllAsRead().subscribe(() => {
      this.notifications.forEach(n => (n.isRead = true));
      this.notifService.refreshUnreadCount();
    });
  }

  prev(): void { if (this.page > 0) this.loadPage(this.page - 1); }
  next(): void { if (this.page < this.totalPages - 1) this.loadPage(this.page + 1); }

  typeIcon(type: string): string {
    const map: Record<string, string> = {
      BOOKING_CREATED:   '📋',
      BOOKING_CONFIRMED: '✅',
      BOOKING_CANCELLED: '❌',
      BOOKING_REMINDER:  '⏰',
    };
    return map[type] ?? '🔔';
  }
}
