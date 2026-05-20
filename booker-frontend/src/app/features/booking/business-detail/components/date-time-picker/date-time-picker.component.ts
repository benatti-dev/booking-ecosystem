import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  OnInit,
  Output,
  signal,
  SimpleChanges,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import { BookingActions } from '../../../../../store/booking/booking.actions';
import { selectSlots, selectSlotsLoading } from '../../../../../store/booking/booking.selectors';
import { SlotResponse } from '../../../../../core/booking/booking.service';
import { CalendarDay, TimeSlotVm } from '../../booking-page.models';

export const WEEK_DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'] as const;

@Component({
  selector: 'app-booking-date-time-picker',
  standalone: false,
  templateUrl: './date-time-picker.component.html',
  styleUrl: './date-time-picker.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingDateTimePickerComponent implements OnInit, OnChanges {
  // ── Inputs ──────────────────────────────────────────────────────────────────
  @Input() serviceId: number | null = null;
  @Input() employeeId: number | null = null;
  @Input() resourceId: number | null = null;

  // ── Outputs ─────────────────────────────────────────────────────────────────
  /** Emits { date: 'YYYY-MM-DD', slot: 'HH:mm:ss' } when user picks a slot. */
  @Output() slotChosen = new EventEmitter<{ date: string; slot: string }>();

  // ── Constants ────────────────────────────────────────────────────────────────
  readonly weekDays = WEEK_DAYS;

  // ── Internal signals ────────────────────────────────────────────────────────
  readonly displayMonth = signal<Date>(this.startOfMonth(new Date()));
  readonly selectedDate = signal<Date>(this.startOfDay(new Date()));
  readonly selectedSlot = signal<string | null>(null);
  readonly slotsData = signal<SlotResponse | null>(null);
  readonly slotsLoading = signal(false);

  // ── Derived state ───────────────────────────────────────────────────────────

  /** Human-readable month + year heading. */
  readonly monthLabel = computed(() =>
    this.displayMonth().toLocaleDateString('en-US', { month: 'long', year: 'numeric' })
  );

  /** True when going to the previous month would go before today's month. */
  readonly canGoBack = computed(() => {
    const now = this.startOfMonth(new Date());
    return this.displayMonth().getTime() > now.getTime();
  });

  /** 6 rows × 7 cols calendar grid for the current display month. */
  readonly calendarWeeks = computed(() =>
    this.buildCalendar(this.displayMonth(), this.selectedDate())
  );

  /** Time slots for the currently selected date. */
  readonly timeSlots = computed((): TimeSlotVm[] => {
    const raw = this.selectedDate();
    const slots = this.slotsData();

    // Only show slots when the loaded data matches the selected date
    if (!slots || slots.date !== this.toIsoDate(raw)) return [];

    const selected = this.selectedSlot();
    return slots.availableSlots.map(s => ({
      display: s.substring(0, 5),
      raw: s,
      isSelected: s === selected,
    }));
  });

  // ── Skeleton placeholders ────────────────────────────────────────────────────
  readonly skeletonSlots = Array.from({ length: 12 });

  // ── DI ──────────────────────────────────────────────────────────────────────
  private readonly store = inject(Store);
  private readonly destroyRef = inject(DestroyRef);

  // ── Lifecycle ────────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.store
      .select(selectSlots)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(slots => this.slotsData.set(slots));

    this.store
      .select(selectSlotsLoading)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(loading => this.slotsLoading.set(loading));

    this.loadSlots();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['serviceId'] || changes['employeeId'] || changes['resourceId']) {
      this.selectedSlot.set(null);
      this.loadSlots();
    }
  }

  // ── Calendar navigation ──────────────────────────────────────────────────────

  prevMonth(): void {
    if (!this.canGoBack()) return;
    this.displayMonth.update(d => new Date(d.getFullYear(), d.getMonth() - 1, 1));
    // Keep selected date in sync if it was in the old month
    this.ensureDateInMonth();
  }

  nextMonth(): void {
    this.displayMonth.update(d => new Date(d.getFullYear(), d.getMonth() + 1, 1));
    this.ensureDateInMonth();
  }

  selectDate(day: CalendarDay): void {
    if (day.isPast || !day.isCurrentMonth) return;
    this.selectedDate.set(day.date);
    this.selectedSlot.set(null);
    this.loadSlots();
  }

  selectSlot(slot: TimeSlotVm): void {
    this.selectedSlot.set(slot.raw);
    this.store.dispatch(BookingActions.selectSlot({ slot: slot.raw }));
    this.slotChosen.emit({ date: this.toIsoDate(this.selectedDate()), slot: slot.raw });
  }

  // ── Track functions ───────────────────────────────────────────────────────────

  trackByDay(_: number, day: CalendarDay): string {
    return day.date.toISOString();
  }

  trackByWeek(i: number): number {
    return i;
  }

  trackBySlot(_: number, slot: TimeSlotVm): string {
    return slot.raw;
  }

  // ── Private helpers ───────────────────────────────────────────────────────────

  private loadSlots(): void {
    const serviceId = this.serviceId;
    if (!serviceId) return;
    this.store.dispatch(
      BookingActions.loadSlots({
        serviceId,
        date: this.toIsoDate(this.selectedDate()),
        employeeId: this.employeeId ?? undefined,
        resourceId: this.resourceId ?? undefined,
      })
    );
  }

  /**
   * When the display month changes, move the selected date to the first
   * non-past day in the new month if the selected date is no longer in it.
   */
  private ensureDateInMonth(): void {
    const sel = this.selectedDate();
    const dm = this.displayMonth();
    if (sel.getFullYear() === dm.getFullYear() && sel.getMonth() === dm.getMonth()) return;

    // Move to 1st of new month (or today if same month as today)
    const today = this.startOfDay(new Date());
    const firstOfMonth = new Date(dm.getFullYear(), dm.getMonth(), 1);
    const candidate = firstOfMonth < today ? today : firstOfMonth;
    this.selectedDate.set(candidate);
    this.loadSlots();
  }

  private buildCalendar(displayMonth: Date, selectedDate: Date): CalendarDay[][] {
    const year = displayMonth.getFullYear();
    const month = displayMonth.getMonth();
    const today = this.startOfDay(new Date());

    // First day of the month; shift so week starts on Monday (0=Mon … 6=Sun)
    const firstDayOfMonth = new Date(year, month, 1);
    const startOffset = (firstDayOfMonth.getDay() + 6) % 7;

    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const daysInPrevMonth = new Date(year, month, 0).getDate();

    const days: CalendarDay[] = [];

    // Trailing days from previous month
    for (let i = startOffset - 1; i >= 0; i--) {
      days.push(this.mkDay(new Date(year, month - 1, daysInPrevMonth - i), false, today, selectedDate));
    }

    // Current month
    for (let d = 1; d <= daysInMonth; d++) {
      days.push(this.mkDay(new Date(year, month, d), true, today, selectedDate));
    }

    // Leading days from next month to fill up to 6 rows
    const targetLen = Math.ceil(days.length / 7) * 7;
    for (let d = 1; days.length < targetLen; d++) {
      days.push(this.mkDay(new Date(year, month + 1, d), false, today, selectedDate));
    }

    const weeks: CalendarDay[][] = [];
    for (let i = 0; i < days.length; i += 7) {
      weeks.push(days.slice(i, i + 7));
    }
    return weeks;
  }

  private mkDay(
    date: Date,
    isCurrentMonth: boolean,
    today: Date,
    selectedDate: Date
  ): CalendarDay {
    const norm = this.startOfDay(date);
    return {
      date: norm,
      dayNum: norm.getDate(),
      isToday: norm.getTime() === today.getTime(),
      isCurrentMonth,
      isPast: norm < today,
      isSelected: norm.getTime() === selectedDate.getTime(),
    };
  }

  private startOfDay(d: Date): Date {
    const copy = new Date(d);
    copy.setHours(0, 0, 0, 0);
    return copy;
  }

  private startOfMonth(d: Date): Date {
    return new Date(d.getFullYear(), d.getMonth(), 1);
  }

  private toIsoDate(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}
