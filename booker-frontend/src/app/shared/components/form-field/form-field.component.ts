import { Component, Input } from '@angular/core';
import { AbstractControl } from '@angular/forms';

@Component({
  selector: 'app-form-field',
  templateUrl: './form-field.component.html',
  standalone: false,
})
export class FormFieldComponent {
  @Input({ required: true }) label!: string;
  @Input({ required: true }) control!: AbstractControl;
  @Input() errorMessage = 'This field is invalid';

  get showError(): boolean {
    return this.control.invalid && this.control.touched;
  }
}
