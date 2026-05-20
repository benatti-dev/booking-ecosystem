import { Component, Input } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { AttributeDefinition } from '../../../core/business/business.service';

@Component({
  selector: 'app-dynamic-attribute-form',
  templateUrl: './dynamic-attribute-form.component.html',
  standalone: false,
})
export class DynamicAttributeFormComponent {
  @Input({ required: true }) definitions!: AttributeDefinition[];
  @Input({ required: true }) form!: FormGroup;

  toggleMultiSelect(fieldKey: string, option: string): void {
    const current: string[] = this.form.get(fieldKey)?.value ?? [];
    const updated = current.includes(option)
      ? current.filter(v => v !== option)
      : [...current, option];
    this.form.patchValue({ [fieldKey]: updated });
  }

  isSelected(fieldKey: string, option: string): boolean {
    const val: string[] = this.form.get(fieldKey)?.value ?? [];
    return val.includes(option);
  }
}
