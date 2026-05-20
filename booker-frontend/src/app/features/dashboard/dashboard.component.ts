import { Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { selectUser } from '../../store/auth/auth.selectors';
import { BookingActions } from '../../store/booking/booking.actions';
import { selectMyBookings, selectMyBookingsLoading } from '../../store/booking/booking.selectors';
import { BookingResponse } from '../../core/booking/booking.service';
import { AuthUser } from '../../core/auth/auth.service';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  standalone: false,
})
export class DashboardComponent implements OnInit, OnDestroy {
  user: AuthUser | null = null;
  bookings: BookingResponse[] = [];
  loading = false;

  private sub = new Subscription();

  constructor(private readonly store: Store) {}

  ngOnInit(): void {
    this.store.dispatch(BookingActions.loadMyBookings());
    this.sub.add(this.store.select(selectUser).subscribe(u => (this.user = u)));
    this.sub.add(this.store.select(selectMyBookings).subscribe(b => (this.bookings = b ?? [])));
    this.sub.add(this.store.select(selectMyBookingsLoading).subscribe(l => (this.loading = l)));
  }

  cancelBooking(booking: BookingResponse): void {
    if (confirm('Cancel this booking?')) {
      this.store.dispatch(BookingActions.cancelBooking({ id: booking.id, reason: 'Cancelled by client' }));
    }
  }

  canCancel(booking: BookingResponse): boolean {
    return booking.status === 'PENDING' || booking.status === 'CONFIRMED';
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}