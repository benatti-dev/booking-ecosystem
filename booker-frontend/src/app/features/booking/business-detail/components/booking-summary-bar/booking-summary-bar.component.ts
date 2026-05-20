import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { ServiceResponse } from '../../../../../core/business/business.service';
import { EmployeeVm } from '../../booking-page.models';

@Component({
  selector: 'app-booking-summary-bar',
  standalone: false,
  templateUrl: './booking-summary-bar.component.html',
  styleUrl: './booking-summary-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingSummaryBarComponent {
  @Input() service: ServiceResponse | null = null;
  @Input() specialist: EmployeeVm | null = null;
  @Input() selectedDate: Date | null = null;
  @Input() selectedSlot: string | null = null;
  @Input() loading = false;

  @Output() proceed = new EventEmitter<void>();

  get isVisible(): boolean {
    return this.service !== null;
  }

  get canProceed(): boolean {
    return this.service !== null && this.selectedSlot !== null;
  }

  get formattedDate(): string {
    if (!this.selectedDate) return '—';
    return this.selectedDate.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
    });
  }

  get formattedTime(): string {
    return this.selectedSlot ? this.selectedSlot.substring(0, 5) : '—';
  }

  get formattedPrice(): string {
    if (!this.service?.price) return '';
    return `${Math.round(this.service.price)} ${this.service.currency}`;
  }

  onProceed(): void {
    if (this.canProceed && !this.loading) {
      this.proceed.emit();
    }
  }
}
