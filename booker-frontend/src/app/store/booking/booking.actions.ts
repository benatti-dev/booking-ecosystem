import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { BookingResponse, CreateBookingRequest, SlotResponse } from '../../core/booking/booking.service';

export const BookingActions = createActionGroup({
  source: 'Booking',
  events: {
    // Slots
    'Load Slots': props<{ serviceId: number; date: string; employeeId?: number; resourceId?: number }>(),
    'Load Slots Success': props<{ slots: SlotResponse }>(),
    'Load Slots Failure': props<{ error: string }>(),

    // Create booking
    'Create Booking': props<{ request: CreateBookingRequest }>(),
    'Create Booking Success': props<{ booking: BookingResponse }>(),
    'Create Booking Failure': props<{ error: string }>(),

    // My bookings
    'Load My Bookings': emptyProps(),
    'Load My Bookings Success': props<{ bookings: BookingResponse[]; total: number }>(),
    'Load My Bookings Failure': props<{ error: string }>(),

    // Cancel
    'Cancel Booking': props<{ id: number; reason?: string }>(),
    'Cancel Booking Success': props<{ booking: BookingResponse }>(),
    'Cancel Booking Failure': props<{ error: string }>(),

    // Confirm / Complete (business owner)
    'Confirm Booking': props<{ id: number }>(),
    'Confirm Booking Success': props<{ booking: BookingResponse }>(),
    'Confirm Booking Failure': props<{ error: string }>(),

    'Complete Booking': props<{ id: number }>(),
    'Complete Booking Success': props<{ booking: BookingResponse }>(),
    'Complete Booking Failure': props<{ error: string }>(),

    // Business bookings
    'Load Business Bookings': props<{ businessId: number }>(),
    'Load Business Bookings Success': props<{ bookings: BookingResponse[] }>(),
    'Load Business Bookings Failure': props<{ error: string }>(),

    // UI state
    'Select Date': props<{ date: string }>(),
    'Select Employee': props<{ employeeId: number | null }>(),
    'Select Slot': props<{ slot: string | null }>(),
    'Reset Booking Flow': emptyProps(),
  },
});
