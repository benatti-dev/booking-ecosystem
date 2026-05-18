import { Component, Input, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Store } from '@ngrx/store';
import { BookingActions } from '../../../store/booking/booking.actions';
import { selectBusinessBookings, selectBusinessBookingsLoading } from '../../../store/booking/booking.selectors';
import { BookingResponse, BookingStatus } from '../../../core/booking/booking.service';

@Component({
  selector: 'app-booking-calendar',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './booking-calendar.component.html',
  styleUrl: './booking-calendar.component.scss',
})
export class BookingCalendarComponent implements OnInit {
  @Input() businessId!: number;

  private readonly store = inject(Store);
  readonly bookings$ = this.store.select(selectBusinessBookings);
  readonly loading$ = this.store.select(selectBusinessBookingsLoading);

  filterStatus: BookingStatus | null = null;
  private bookings: BookingResponse[] = [];

  ngOnInit(): void {
    this.store.dispatch(BookingActions.loadBusinessBookings({ businessId: this.businessId }));
    this.bookings$.subscribe(b => { this.bookings = b; });
  }

  filteredBookings(): BookingResponse[] {
    if (!this.filterStatus) return this.bookings;
    return this.bookings.filter(b => b.status === this.filterStatus);
  }

  confirm(booking: BookingResponse): void {
    this.store.dispatch(BookingActions.confirmBooking({ id: booking.id }));
  }

  complete(booking: BookingResponse): void {
    this.store.dispatch(BookingActions.completeBooking({ id: booking.id }));
  }

  cancel(booking: BookingResponse): void {
    if (confirm('Cancel this client booking?')) {
      this.store.dispatch(BookingActions.cancelBooking({ id: booking.id, reason: 'Cancelled by business' }));
    }
  }

  statusLabel(status: BookingStatus): string {
    const map: Record<BookingStatus, string> = {
      PENDING: 'Pending', CONFIRMED: 'Confirmed',
      COMPLETED: 'Completed', CANCELLED: 'Cancelled', NO_SHOW: 'No-show'
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
}
