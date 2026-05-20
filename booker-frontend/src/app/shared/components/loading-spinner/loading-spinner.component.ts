import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-loading-spinner',
  templateUrl: './loading-spinner.component.html',
  standalone: false,
})
export class LoadingSpinnerComponent {
  @Input() label = 'Loading...';
}
