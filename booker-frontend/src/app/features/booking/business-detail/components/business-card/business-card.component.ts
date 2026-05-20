import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { BusinessResponse, BranchResponse } from '../../../../../core/business/business.service';

@Component({
  selector: 'app-booking-business-card',
  standalone: false,
  templateUrl: './business-card.component.html',
  styleUrl: './business-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingBusinessCardComponent {
  @Input({ required: true }) business!: BusinessResponse;
  @Input() branch: BranchResponse | null = null;

  get fullAddress(): string {
    if (!this.branch) return '';
    return [this.branch.address, this.branch.city].filter(Boolean).join(', ');
  }
}
