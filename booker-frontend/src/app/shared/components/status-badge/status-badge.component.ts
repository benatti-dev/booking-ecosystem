import { Component, Input } from '@angular/core';

const STATUS_LABELS: Record<string, string> = {
  PENDING:    'Pending',
  CONFIRMED:  'Confirmed',
  COMPLETED:  'Completed',
  CANCELLED:  'Cancelled',
  NO_SHOW:    'No-show',
  ACTIVE:     'Active',
  REJECTED:   'Rejected',
  SUSPENDED:  'Suspended',
};

const STATUS_CLASSES: Record<string, string> = {
  PENDING:   'bg-yellow-100 text-yellow-700',
  CONFIRMED: 'bg-green-100 text-green-700',
  COMPLETED: 'bg-blue-100 text-blue-700',
  CANCELLED: 'bg-red-100 text-red-600',
  NO_SHOW:   'bg-gray-100 text-gray-600',
  ACTIVE:    'bg-emerald-100 text-emerald-700',
  REJECTED:  'bg-red-100 text-red-600',
  SUSPENDED: 'bg-orange-100 text-orange-700',
};

@Component({
  selector: 'app-status-badge',
  templateUrl: './status-badge.component.html',
  standalone: false,
})
export class StatusBadgeComponent {
  @Input({ required: true }) status!: string;

  get label(): string { return STATUS_LABELS[this.status] ?? this.status; }
  get cssClass(): string { return STATUS_CLASSES[this.status] ?? 'bg-gray-100 text-gray-700'; }
}
