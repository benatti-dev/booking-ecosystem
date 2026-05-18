import { createReducer, on } from '@ngrx/store';
import { BookingResponse, SlotResponse } from '../../core/booking/booking.service';
import { BookingActions } from './booking.actions';

export interface BookingState {
  // Slot selection
  slots: SlotResponse | null;
  slotsLoading: boolean;
  selectedDate: string | null;
  selectedEmployeeId: number | null;
  selectedSlot: string | null;

  // Booking creation
  pendingBooking: BookingResponse | null;
  creating: boolean;
  createError: string | null;

  // My bookings
  myBookings: BookingResponse[];
  myBookingsTotal: number;
  myBookingsLoading: boolean;

  // Business bookings
  businessBookings: BookingResponse[];
  businessBookingsLoading: boolean;

  error: string | null;
}

const initialState: BookingState = {
  slots: null,
  slotsLoading: false,
  selectedDate: null,
  selectedEmployeeId: null,
  selectedSlot: null,
  pendingBooking: null,
  creating: false,
  createError: null,
  myBookings: [],
  myBookingsTotal: 0,
  myBookingsLoading: false,
  businessBookings: [],
  businessBookingsLoading: false,
  error: null,
};

export const bookingReducer = createReducer(
  initialState,

  // Slots
  on(BookingActions.loadSlots, state => ({ ...state, slotsLoading: true, slots: null })),
  on(BookingActions.loadSlotsSuccess, (state, { slots }) => ({
    ...state, slotsLoading: false, slots
  })),
  on(BookingActions.loadSlotsFailure, (state, { error }) => ({
    ...state, slotsLoading: false, error
  })),

  // Create booking
  on(BookingActions.createBooking, state => ({
    ...state, creating: true, createError: null, pendingBooking: null
  })),
  on(BookingActions.createBookingSuccess, (state, { booking }) => ({
    ...state, creating: false, pendingBooking: booking
  })),
  on(BookingActions.createBookingFailure, (state, { error }) => ({
    ...state, creating: false, createError: error
  })),

  // My bookings
  on(BookingActions.loadMyBookings, state => ({ ...state, myBookingsLoading: true })),
  on(BookingActions.loadMyBookingsSuccess, (state, { bookings, total }) => ({
    ...state, myBookingsLoading: false, myBookings: bookings, myBookingsTotal: total
  })),
  on(BookingActions.loadMyBookingsFailure, (state, { error }) => ({
    ...state, myBookingsLoading: false, error
  })),

  // Cancel
  on(BookingActions.cancelBookingSuccess, (state, { booking }) => ({
    ...state,
    myBookings: state.myBookings.map(b => b.id === booking.id ? booking : b),
    businessBookings: state.businessBookings.map(b => b.id === booking.id ? booking : b),
  })),

  // Confirm
  on(BookingActions.confirmBookingSuccess, (state, { booking }) => ({
    ...state,
    businessBookings: state.businessBookings.map(b => b.id === booking.id ? booking : b),
  })),

  // Complete
  on(BookingActions.completeBookingSuccess, (state, { booking }) => ({
    ...state,
    businessBookings: state.businessBookings.map(b => b.id === booking.id ? booking : b),
  })),

  // Business bookings
  on(BookingActions.loadBusinessBookings, state => ({ ...state, businessBookingsLoading: true })),
  on(BookingActions.loadBusinessBookingsSuccess, (state, { bookings }) => ({
    ...state, businessBookingsLoading: false, businessBookings: bookings
  })),
  on(BookingActions.loadBusinessBookingsFailure, (state, { error }) => ({
    ...state, businessBookingsLoading: false, error
  })),

  // UI state
  on(BookingActions.selectDate, (state, { date }) => ({
    ...state, selectedDate: date, slots: null, selectedSlot: null
  })),
  on(BookingActions.selectEmployee, (state, { employeeId }) => ({
    ...state, selectedEmployeeId: employeeId, slots: null, selectedSlot: null
  })),
  on(BookingActions.selectSlot, (state, { slot }) => ({ ...state, selectedSlot: slot })),
  on(BookingActions.resetBookingFlow, state => ({
    ...state,
    slots: null,
    selectedDate: null,
    selectedEmployeeId: null,
    selectedSlot: null,
    pendingBooking: null,
    createError: null,
  })),
);
