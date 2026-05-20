import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { switchMap, catchError, of } from 'rxjs';
import { BusinessService, BusinessCategory } from '../../../core/business/business.service';

@Component({
  selector: 'app-business-register',
  templateUrl: './business-register.component.html',
  standalone: false,
})
export class BusinessRegisterComponent implements OnInit {
  step = signal(1);
  totalSteps = 3;

  categories = signal<BusinessCategory[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  createdBusinessId = signal<number | null>(null);

  // Step 1 — Basic info
  step1 = this.fb.group({
    categoryId: [null as number | null, Validators.required],
    name:        ['', [Validators.required, Validators.maxLength(255)]],
    description: ['']
  });

  // Step 2 — Branch info
  step2 = this.fb.group({
    branchName: ['', Validators.required],
    address:    ['', Validators.required],
    city:       ['', Validators.required],
    country:    ['UA'],
    latitude:   [null as number | null],
    longitude:  [null as number | null],
    timezone:   ['Europe/Kiev'],
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly businessSvc: BusinessService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.businessSvc.getCategories().subscribe({
      next: cats => this.categories.set(cats),
      error: () => this.error.set('Failed to load categories'),
    });
  }

  next(): void {
    if (this.step() === 1 && this.step1.invalid) {
      this.step1.markAllAsTouched();
      return;
    }
    if (this.step() === 2 && this.step2.invalid) {
      this.step2.markAllAsTouched();
      return;
    }
    if (this.step() < this.totalSteps) {
      this.step.update(s => s + 1);
    }
  }

  back(): void {
    if (this.step() > 1) this.step.update(s => s - 1);
  }

  submit(): void {
    this.loading.set(true);
    this.error.set(null);

    const s1 = this.step1.value;
    this.businessSvc.createBusiness({
      categoryId: s1.categoryId!,
      name: s1.name!,
      description: s1.description ?? undefined
    }).pipe(
      switchMap(business => {
        this.createdBusinessId.set(business.id);
        const s2 = this.step2.value;
        return this.businessSvc.createBranch(business.id, {
          name: s2.branchName!,
          address: s2.address!,
          city: s2.city!,
          country: s2.country ?? 'UA',
          latitude: s2.latitude ?? undefined,
          longitude: s2.longitude ?? undefined,
          timezone: s2.timezone ?? 'Europe/Kiev',
          isPrimary: true
        }).pipe(
          catchError(() => {
            this.error.set('Business created but failed to add branch. You can add a branch later.');
            return of(null);
          })
        );
      })
    ).subscribe({
      next: () => {
        this.step.set(3);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Failed to create business.');
        this.loading.set(false);
      }
    });
  }

  goToServices(): void {
    const id = this.createdBusinessId();
    if (id) {
      this.router.navigate(['/business', id, 'services']);
    }
  }

  goToMyBusinesses(): void {
    this.router.navigate(['/business', 'my-businesses']);
  }

  selectedCategoryLabel(): string {
    const id = this.step1.value.categoryId;
    return this.categories().find(c => c.id === id)?.label ?? '';
  }
}
