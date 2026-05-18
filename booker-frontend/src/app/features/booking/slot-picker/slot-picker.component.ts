import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Store } from '@ngrx/store';
import { BookingActions } from '../../../store/booking/booking.actions';
import { selectSlots, selectSlotsLoading, selectSelectedSlot } from '../../../store/booking/booking.selectors';

@Component({
  selector: 'app-slot-picker',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './slot-picker.component.html',
  styleUrl: './slot-picker.component.scss',
})
export class SlotPickerComponent implements OnChanges {
  @Input() serviceId!: number;
  @Input() employeeId?: number;
  @Input() resourceId?: number;
  @Output() slotSelected = new EventEmitter<{ date: string; slot: string }>();

  private readonly store = inject(Store);

  currentDate = new Date();
  readonly slots$ = this.store.select(selectSlots);
  readonly loading$ = this.store.select(selectSlotsLoading);
  readonly selectedSlot$ = this.store.select(selectSelectedSlot);

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
}
