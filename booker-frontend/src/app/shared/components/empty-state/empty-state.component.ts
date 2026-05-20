import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  templateUrl: './empty-state.component.html',
  standalone: false,
})
export class EmptyStateComponent {
  @Input() icon = '📭';
  @Input() title = 'Nothing here yet';
  @Input() subtitle = '';
}
