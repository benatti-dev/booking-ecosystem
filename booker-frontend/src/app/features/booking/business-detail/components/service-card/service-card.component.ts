import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { ServiceResponse } from '../../../../../core/business/business.service';

@Component({
  selector: 'app-booking-service-card',
  standalone: false,
  templateUrl: './service-card.component.html',
  styleUrl: './service-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingServiceCardComponent {
  @Input({ required: true }) service!: ServiceResponse;
  @Input() isSelected = false;
  @Output() selected = new EventEmitter<ServiceResponse>();

  get formattedPrice(): string {
    if (this.service.price == null) return 'Free';
    return `${Math.round(this.service.price)} ${this.service.currency}`;
  }

  onSelect(): void {
    this.selected.emit(this.service);
  }
}
