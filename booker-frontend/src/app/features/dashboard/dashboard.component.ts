import { Component, inject, OnInit } from '@angular/core';
import { AsyncPipe, NgIf, NgFor, NgClass, DatePipe, CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Store } from '@ngrx/store';
import { selectUser } from '../../store/auth/auth.selectors';
import { BookingActions } from '../../store/booking/booking.actions';
import { selectMyBookings, selectMyBookingsLoading } from '../../store/booking/booking.selectors';
import { BookingResponse, BookingStatus } from '../../core/booking/booking.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [AsyncPipe, NgIf, NgFor, NgClass, DatePipe, CurrencyPipe, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly store = inject(Store);
  readonly user$ = this.store.select(selectUser);
  readonly bookings$ = this.store.select(selectMyBookings);
  readonly loading$ = this.store.select(selectMyBookingsLoading);

  ngOnInit(): void {
    this.store.dispatch(BookingActions.loadMyBookings());
  }

  cancelBooking(booking: BookingResponse): void {
    if (confirm('Cancel this booking?')) {
      this.store.dispatch(BookingActions.cancelBooking({ id: booking.id, reason: 'Cancelled by client' }));
    }
  }

  statusLabel(status: BookingStatus): string {
    const map: Record<BookingStatus, string> = {
      PENDING: 'Pending',
      CONFIRMED: 'Confirmed',
      COMPLETED: 'Completed',
      CANCELLED: 'Cancelled',
      NO_SHOW: 'No-show',
    };
    return map[status] ?? status;
  }

  statusClass(status: BookingStatus): string {
    const map: Record<BookingStatus, string> = {
      PENDING: 'bg-yellow-100 text-yellow-700',
      CONFIRMED: 'bg-green-100 text-green-700',
      COMPLETED: 'bg-blue-100 text-blue-700',
      CANCELLED: 'bg-red-100 text-red-600',
      NO_SHOW: 'bg-gray-100 text-gray-600',
    };
    return map[status] ?? '';
  }

  canCancel(booking: BookingResponse): boolean {
    return booking.status === 'PENDING' || booking.status === 'CONFIRMED';
  }
}