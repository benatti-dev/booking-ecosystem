import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { BookingActions } from '../../../store/booking/booking.actions';
import { selectSlots, selectSlotsLoading, selectSelectedSlot } from '../../../store/booking/booking.selectors';

@Component({
  selector: 'app-slot-picker',
  templateUrl: './slot-picker.component.html',
  styleUrl: './slot-picker.component.scss',
  standalone: false,
})
export class SlotPickerComponent implements OnChanges, OnInit, OnDestroy {
  @Input() serviceId!: number;
  @Input() employeeId?: number;
  @Input() resourceId?: number;
  @Output() slotSelected = new EventEmitter<{ date: string; slot: string }>();

  currentDate = new Date();
  slots: { availableSlots: string[] } | null = null;
  loading = false;
  selectedSlot: string | null = null;

  private sub = new Subscription();

  constructor(private readonly store: Store) {}

  ngOnInit(): void {
    this.sub.add(this.store.select(selectSlots).subscribe(s => (this.slots = s)));
    this.sub.add(this.store.select(selectSlotsLoading).subscribe(l => (this.loading = l)));
    this.sub.add(this.store.select(selectSelectedSlot).subscribe(s => (this.selectedSlot = s)));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['serviceId'] || changes['employeeId'] || changes['resourceId']) {
      this.loadSlots();
    }
  }

  changeDate(delta: number): void {
    const d = new Date(this.currentDate);
    d.setDate(d.getDate() + delta);
    this.currentDate = d;
    this.loadSlots();
  }

  selectSlot(slot: string): void {
    this.store.dispatch(BookingActions.selectSlot({ slot }));
    this.slotSelected.emit({ date: this.toIsoDate(this.currentDate), slot });
  }

  isToday(): boolean {
    const today = new Date();
    return this.toIsoDate(this.currentDate) === this.toIsoDate(today);
  }

  formatDate(d: Date): string {
    return d.toLocaleDateString('en-US', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  formatTime(slot: string): string {
    return slot.substring(0, 5); // "HH:mm"
  }

  private loadSlots(): void {
    if (!this.serviceId) return;
    this.store.dispatch(BookingActions.loadSlots({
      serviceId: this.serviceId,
      date: this.toIsoDate(this.currentDate),
      employeeId: this.employeeId,
      resourceId: this.resourceId,
    }));
  }

  private toIsoDate(d: Date): string {
    return d.toISOString().split('T')[0];
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}
