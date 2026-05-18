import { createFeatureSelector, createSelector } from '@ngrx/store';
import { BookingState } from './booking.reducer';

export const selectBookingState = createFeatureSelector<BookingState>('booking');

export const selectSlots = createSelector(selectBookingState, s => s.slots);
export const selectSlotsLoading = createSelector(selectBookingState, s => s.slotsLoading);
export const selectSelectedDate = createSelector(selectBookingState, s => s.selectedDate);
export const selectSelectedSlot = createSelector(selectBookingState, s => s.selectedSlot);
export const selectSelectedEmployeeId = createSelector(selectBookingState, s => s.selectedEmployeeId);

export const selectPendingBooking = createSelector(selectBookingState, s => s.pendingBooking);
export const selectCreating = createSelector(selectBookingState, s => s.creating);
export const selectCreateError = createSelector(selectBookingState, s => s.createError);

export const selectMyBookings = createSelector(selectBookingState, s => s.myBookings);
export const selectMyBookingsLoading = createSelector(selectBookingState, s => s.myBookingsLoading);
export const selectMyBookingsTotal = createSelector(selectBookingState, s => s.myBookingsTotal);

export const selectBusinessBookings = createSelector(selectBookingState, s => s.businessBookings);
export const selectBusinessBookingsLoading = createSelector(selectBookingState, s => s.businessBookingsLoading);

export const selectBookingError = createSelector(selectBookingState, s => s.error);
