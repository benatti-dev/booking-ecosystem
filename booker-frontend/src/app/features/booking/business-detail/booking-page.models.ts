import { EmployeeResponse } from '../../../core/booking/booking.service';

/** A single day cell in the calendar grid. */
export interface CalendarDay {
  date: Date;
  dayNum: number;
  isToday: boolean;
  isCurrentMonth: boolean;
  isPast: boolean;
  isSelected: boolean;
}

/** A displayable time slot built from the API's availableSlots array. */
export interface TimeSlotVm {
  /** Display string shown in the UI, e.g. "09:00" */
  display: string;
  /** Raw value as returned by the API, e.g. "09:00:00" */
  raw: string;
  /** True when this slot matches the currently selected slot. */
  isSelected: boolean;
}

/**
 * Employee enriched with UI-only fields not yet present in the core API.
 * The `rating` field will be sourced from the reviews service once integrated.
 */
export interface EmployeeVm extends EmployeeResponse {
  rating: number | null;
}
