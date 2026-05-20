import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { RxStomp, RxStompConfig, RxStompState } from '@stomp/rx-stomp';
import { environment } from '../../../environments/environment';

export interface NotificationItem {
  id: number;
  type: string;
  title: string;
  body: string;
  referenceId: number | null;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationPage {
  content: NotificationItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private readonly api = environment.apiUrl;

  constructor(
    private readonly http: HttpClient,
    private readonly ngZone: NgZone,
  ) {}

  private readonly stomp = new RxStomp();
  private wsSub?: Subscription;
  private stateSub?: Subscription;

  private readonly _unreadCount = new BehaviorSubject<number>(0);
  readonly unreadCount$ = this._unreadCount.asObservable();

  /** Call after the user has logged in and a JWT token is available. */
  connect(token: string): void {
    // Prevent multiple connections — disconnect any existing one first
    this.disconnect();

    const config: RxStompConfig = {
      brokerURL: environment.wsUrl,
      connectHeaders: { Authorization: `Bearer ${token}` },
      heartbeatIncoming: 0,
      heartbeatOutgoing: 20_000,
      reconnectDelay: 5_000,
    };
    this.stomp.configure(config);
    this.stomp.activate();

    // Log connection state changes for diagnostics
    this.stateSub = this.stomp.connectionState$.subscribe(state => {
      console.log('[WS] STOMP state:', RxStompState[state]);
    });

    // Log STOMP-level errors (e.g. rejected CONNECT frame)
    this.stomp.stompErrors$.subscribe(frame => {
      console.error('[WS] STOMP ERROR frame:', frame.headers, frame.body);
    });

    // Subscribe to per-user notification queue
    this.wsSub = this.stomp
      .watch('/user/queue/notifications')
      .subscribe(msg => {
        // STOMP callbacks run outside Angular's zone — re-enter it so that
        // OnPush components and other zone-dependent code detect the change.
        console.log('Received notification:', msg);
        this.ngZone.run(() => {
          const notif: NotificationItem = JSON.parse(msg.body);
          if (!notif.isRead) {
            this._unreadCount.next(this._unreadCount.value + 1);
          }
        });
      });

    // Fetch current unread count on connect
    this.refreshUnreadCount();
  }

  disconnect(): void {
    this.wsSub?.unsubscribe();
    this.stateSub?.unsubscribe();
    this.stomp.deactivate();
    this._unreadCount.next(0);
  }

  refreshUnreadCount(): void {
    this.getUnreadCount().subscribe(res => this._unreadCount.next(res.count));
  }

  getNotifications(page = 0, size = 20): Observable<NotificationPage> {
    return this.http.get<NotificationPage>(`${this.api}/notifications`, {
      params: { page, size }
    });
  }

  getUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.api}/notifications/unread-count`);
  }

  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.api}/notifications/${id}/read`, {});
  }

  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.api}/notifications/read-all`, {});
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}
