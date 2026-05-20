import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { switchMap } from 'rxjs';
import {
  BusinessService,
  AttributeDefinition,
} from '../../../core/business/business.service';

@Component({
  selector: 'app-service-form',
  templateUrl: './service-form.component.html',
  standalone: false,
})
export class ServiceFormComponent implements OnInit {
  businessId   = signal(0);
  categoryId   = signal(0);
  definitions  = signal<AttributeDefinition[]>([]);
  loading      = signal(false);
  error        = signal<string | null>(null);

  // Main service form
  form = this.fb.group({
    name:        ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    durationMin: [null as number | null, [Validators.required, Validators.min(1)]],
    price:       [null as number | null],
    currency:    ['UAH'],
  });

  // Dynamic attributes FormGroup (built after definitions load)
  attrsForm: FormGroup = this.fb.group({});

  constructor(
    private readonly fb: FormBuilder,
    private readonly businessSvc: BusinessService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('businessId'));
    this.businessId.set(id);

    this.businessSvc.getBusiness(id).pipe(
      switchMap(business => {
        this.categoryId.set(business.category.id);
        return this.businessSvc.getAttributeDefinitions(business.category.id);
      })
    ).subscribe({
      next: defs => {
        this.definitions.set(defs);
        this.buildAttrsForm(defs);
      },
      error: () => this.error.set('Failed to load business details.')
    });
  }

  private buildAttrsForm(defs: AttributeDefinition[]): void {
    const controls: Record<string, unknown[]> = {};
    for (const def of defs) {
      const validators = def.isRequired ? [Validators.required] : [];
      if (def.fieldType === 'BOOLEAN') {
        controls[def.fieldKey] = [false, validators];
      } else if (def.fieldType === 'MULTI_SELECT') {
        controls[def.fieldKey] = [[] as string[], validators];
      } else {
        controls[def.fieldKey] = ['', validators];
      }
    }
    this.attrsForm = this.fb.group(controls);
  }

  cancel(): void {
    this.router.navigate(['/business', this.businessId(), 'services']);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const v = this.form.value;
    const attrs: Record<string, unknown> = {};
    for (const def of this.definitions()) {
      attrs[def.fieldKey] = this.attrsForm.value[def.fieldKey];
    }

    this.businessSvc.createService(this.businessId(), {
      name:        v.name!,
      description: v.description ?? undefined,
      durationMin: v.durationMin!,
      price:       v.price ?? undefined,
      currency:    v.currency ?? 'UAH',
      attributes:  attrs
    }).subscribe({
      next: () => {
        this.router.navigate(['/business', this.businessId(), 'services']);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Failed to create service.');
        this.loading.set(false);
      }
    });
  }
}
