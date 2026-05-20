import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { EmployeeVm } from '../../booking-page.models';

@Component({
  selector: 'app-booking-specialist-card',
  standalone: false,
  templateUrl: './specialist-card.component.html',
  styleUrl: './specialist-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingSpecialistCardComponent {
  @Input({ required: true }) specialist!: EmployeeVm;
  @Input() isSelected = false;
  @Output() selected = new EventEmitter<EmployeeVm>();

  /** First two initials for the avatar fallback. */
  get initials(): string {
    return this.specialist.displayName
      .split(' ')
      .slice(0, 2)
      .map(n => n[0] ?? '')
      .join('')
      .toUpperCase();
  }

  get ratingText(): string | null {
    return this.specialist.rating != null
      ? this.specialist.rating.toFixed(1)
      : null;
  }

  onSelect(): void {
    this.selected.emit(this.specialist);
  }
}
