import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { BookingService } from '../../../core/booking/booking.service';

@Component({
  selector: 'app-employee-form',
  templateUrl: './employee-form.component.html',
  styleUrl: './employee-form.component.scss',
  standalone: false,
})
export class EmployeeFormComponent implements OnInit, OnDestroy {
  businessId!: number;
  saving = false;
  error: string | null = null;

  form: FormGroup = this.fb.group({
    displayName: ['', [Validators.required, Validators.maxLength(255)]],
    position:    ['', Validators.maxLength(100)],
    bio:         [''],
    userId:      [null as number | null],
  });

  private sub = new Subscription();

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly bookingService: BookingService,
  ) {}

  ngOnInit(): void {
    this.businessId = +this.route.snapshot.paramMap.get('businessId')!;
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.error = null;

    const raw = this.form.value;
    const req = {
      displayName: raw.displayName,
      position: raw.position || undefined,
      bio: raw.bio || undefined,
      userId: raw.userId ?? undefined,
    };

    this.sub.add(
      this.bookingService.createEmployee(this.businessId, req).subscribe({
        next: () => this.router.navigate([`/business/${this.businessId}/employees`]),
        error: err => {
          this.error = err.error?.message ?? 'Failed to add employee';
          this.saving = false;
        },
      })
    );
  }

  cancel(): void {
    this.router.navigate([`/business/${this.businessId}/employees`]);
  }

  get displayNameCtrl() { return this.form.get('displayName')!; }
  get positionCtrl()    { return this.form.get('position')!; }

  ngOnDestroy(): void { this.sub.unsubscribe(); }
}
