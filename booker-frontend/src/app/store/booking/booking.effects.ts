import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';
import { BookingActions } from './booking.actions';
import { BookingService } from '../../core/booking/booking.service';

@Injectable()
export class BookingEffects {
  private readonly actions$ = inject(Actions);
  private readonly bookingService = inject(BookingService);

  loadSlots$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BookingActions.loadSlots),
      switchMap(({ serviceId, date, employeeId, resourceId }) =>
        this.bookingService.getSlots(serviceId, date, employeeId, resourceId).pipe(
          map(slots => BookingActions.loadSlotsSuccess({ slots })),
          catchError(err => of(BookingActions.loadSlotsFailure({ error: err.message })))
        )
      )
    )
  );

  createBooking$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BookingActions.createBooking),
      switchMap(({ request }) =>
        this.bookingService.createBooking(request).pipe(
          map(booking => BookingActions.createBookingSuccess({ booking })),
          catchError(err => of(BookingActions.createBookingFailure({
            error: err.error?.message ?? 'Failed to create booking'
          })))
        )
      )
    )
  );

  loadMyBookings$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BookingActions.loadMyBookings),
      switchMap(() =>
        this.bookingService.getMyBookings().pipe(
          map(page => BookingActions.loadMyBookingsSuccess({
            bookings: page.content,
            total: page.totalElements
          })),
          catchError(err => of(BookingActions.loadMyBookingsFailure({ error: err.message })))
        )
      )
    )
  );

  cancelBooking$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BookingActions.cancelBooking),
      switchMap(({ id, reason }) =>
        this.bookingService.cancelBooking(id, reason).pipe(
          map(booking => BookingActions.cancelBookingSuccess({ booking })),
          catchError(err => of(BookingActions.cancelBookingFailure({ error: err.message })))
        )
      )
    )
  );

  confirmBooking$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BookingActions.confirmBooking),
      switchMap(({ id }) =>
        this.bookingService.confirmBooking(id).pipe(
          map(booking => BookingActions.confirmBookingSuccess({ booking })),
          catchError(err => of(BookingActions.confirmBookingFailure({ error: err.message })))
        )
      )
    )
  );

  completeBooking$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BookingActions.completeBooking),
      switchMap(({ id }) =>
        this.bookingService.completeBooking(id).pipe(
          map(booking => BookingActions.completeBookingSuccess({ booking })),
          catchError(err => of(BookingActions.completeBookingFailure({ error: err.message })))
        )
      )
    )
  );

  loadBusinessBookings$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BookingActions.loadBusinessBookings),
      switchMap(({ businessId }) =>
        this.bookingService.getBusinessBookings(businessId).pipe(
          map(page => BookingActions.loadBusinessBookingsSuccess({ bookings: page.content })),
          catchError(err => of(BookingActions.loadBusinessBookingsFailure({ error: err.message })))
        )
      )
    )
  );
}
