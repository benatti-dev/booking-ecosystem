import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-alert',
  templateUrl: './alert.component.html',
  standalone: false,
})
export class AlertComponent {
  @Input({ required: true }) message!: string | null;
  @Input() type: 'error' | 'success' | 'warning' = 'error';

  get classes(): string {
    switch (this.type) {
      case 'success': return 'text-green-700 bg-green-50 border-green-200';
      case 'warning': return 'text-yellow-700 bg-yellow-50 border-yellow-200';
      default:        return 'text-red-600 bg-red-50 border-red-200';
    }
  }
}
