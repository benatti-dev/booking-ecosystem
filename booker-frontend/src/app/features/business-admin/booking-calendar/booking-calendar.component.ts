import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Actions, ofType } from '@ngrx/effects';
import { Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { BookingActions } from '../../../store/booking/booking.actions';
import { BookingService, BookingResponse, BookingStatus, EmployeeResponse } from '../../../core/booking/booking.service';

@Component({
  selector: 'app-booking-calendar',
  templateUrl: './booking-calendar.component.html',
  styleUrl: './booking-calendar.component.scss',
  standalone: false,
})
export class BookingCalendarComponent implements OnInit, OnDestroy {
  businessId!: number;

  loading = false;
  error: string | null = null;

  selectedDate = new Date().toISOString().substring(0, 10); // YYYY-MM-DD
  filterStatus: BookingStatus | null = null;
  filterEmployeeId: number | null = null;

  employees: EmployeeResponse[] = [];
  private bookings: BookingResponse[] = [];
  filteredBookingsList: BookingResponse[] = [];

  private sub = new Subscription();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly store: Store,
    private readonly actions$: Actions,
    private readonly bookingService: BookingService,
  ) {}

  ngOnInit(): void {
    this.businessId = +this.route.snapshot.paramMap.get('businessId')!;
    this.loadEmployees();
    this.loadBookings();

    // Listen for status-change success actions emitted by NgRx effects
    // and update the local booking in-place without a full reload.
    this.sub.add(
      this.actions$.pipe(
        ofType(
          BookingActions.confirmBookingSuccess,
          BookingActions.completeBookingSuccess,
          BookingActions.cancelBookingSuccess,
        ),
      ).subscribe(({ booking }) => {
        this.replaceBooking(booking);
        this.applyFilters();
      }),
    );
  }

  loadBookings(): void {
    this.loading = true;
    this.error = null;
    const from = new Date(this.selectedDate + 'T00:00:00').toISOString();
    const to   = new Date(this.selectedDate + 'T23:59:59').toISOString();
    this.sub.add(
      this.bookingService.getBusinessBookings(this.businessId, from, to).subscribe({
        next: page => {
          this.bookings = page.content;
          this.loading = false;
          this.applyFilters();
        },
        error: () => { this.error = 'Failed to load bookings'; this.loading = false; },
      })
    );
  }

  private loadEmployees(): void {
    this.sub.add(
      this.bookingService.getEmployees(this.businessId).subscribe({
        next: page => (this.employees = page.content),
        error: () => {},
      })
    );
  }

  prevDay(): void { this.shiftDate(-1); }
  nextDay(): void { this.shiftDate(1); }
  today(): void {
    this.selectedDate = new Date().toISOString().substring(0, 10);
    this.loadBookings();
  }

  onDateChange(value: string): void {
    this.selectedDate = value;
    this.loadBookings();
  }

  private shiftDate(delta: number): void {
    const d = new Date(this.selectedDate + 'T12:00:00');
    d.setDate(d.getDate() + delta);
    this.selectedDate = d.toISOString().substring(0, 10);
    this.loadBookings();
  }

  applyFilters(): void {
    let list = this.bookings;
    if (this.filterStatus) list = list.filter(b => b.status === this.filterStatus);
    if (this.filterEmployeeId) list = list.filter(b => b.employeeId === this.filterEmployeeId);
    this.filteredBookingsList = list;
  }

  /**
   * Dispatch to the store ONLY. The NgRx effect handles the API call and
   * emits a success/failure action.  Direct bookingService calls are removed
   * to prevent double HTTP requests.
   */
  confirm(booking: BookingResponse): void {
    this.store.dispatch(BookingActions.confirmBooking({ id: booking.id }));
  }

  complete(booking: BookingResponse): void {
    this.store.dispatch(BookingActions.completeBooking({ id: booking.id }));
  }

  cancel(booking: BookingResponse): void {
    if (!confirm('Cancel this client booking?')) return;
    this.store.dispatch(BookingActions.cancelBooking({ id: booking.id, reason: 'Cancelled by business' }));
  }

  private replaceBooking(updated: BookingResponse): void {
    this.bookings = this.bookings.map(b => b.id === updated.id ? updated : b);
  }

  ngOnDestroy(): void { this.sub.unsubscribe(); }
}
