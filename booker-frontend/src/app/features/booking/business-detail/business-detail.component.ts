import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';

import { BusinessService, ServiceResponse, BranchResponse } from '../../../core/business/business.service';
import { BookingService } from '../../../core/booking/booking.service';
import { BookingActions } from '../../../store/booking/booking.actions';
import { EmployeeVm } from './booking-page.models';

@Component({
  selector: 'app-business-detail',
  standalone: false,
  templateUrl: './business-detail.component.html',
  styleUrl: './business-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BusinessDetailComponent implements OnInit {
  // в”Ђв”Ђ Server data (signals) в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
  readonly business = signal<any>(null);
  readonly branch = signal<BranchResponse | null>(null);
  readonly services = signal<ServiceResponse[]>([]);
  readonly employees = signal<EmployeeVm[]>([]);

  // в”Ђв”Ђ Loading states в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
  readonly loadingBusiness = signal(true);
  readonly loadingServices = signal(true);
  readonly loadingEmployees = signal(true);

  // в”Ђв”Ђ Selection state в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
  readonly selectedService = signal<ServiceResponse | null>(null);
  readonly selectedEmployee = signal<EmployeeVm | null>(null);
  readonly selectedDate = signal<Date | null>(null);
  readonly selectedSlot = signal<string | null>(null);
  readonly showAllSpecialists = signal(false);

  // в”Ђв”Ђ Derived в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
  readonly displayedEmployees = computed(() => {
    const all = this.employees();
    return this.showAllSpecialists() ? all : all.slice(0, 3);
  });

  readonly canProceed = computed(
    () => this.selectedService() !== null && this.selectedSlot() !== null
  );

  /** Skeleton rows for service list */
  readonly serviceSkeletons = [1, 2, 3];
  readonly specialistSkeletons = [1, 2, 3];

  // в”Ђв”Ђ Private в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ
  private businessId!: number;

  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly store = inject(Store);
  private readonly businessService = inject(BusinessService);
  private readonly bookingService = inject(BookingService);

  // в”Ђв”Ђ Lifecycle в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

  ngOnInit(): void {
    this.businessId = Number(this.route.snapshot.paramMap.get('businessId'));
    this.store.dispatch(BookingActions.resetBookingFlow());
    this.loadData();
  }

  // в”Ђв”Ђ Event handlers в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

  selectService(svc: ServiceResponse): void {
    this.selectedService.set(svc);
    this.selectedSlot.set(null);
  }

  selectEmployee(emp: EmployeeVm): void {
    this.selectedEmployee.set(emp);
    this.selectedSlot.set(null);
    this.store.dispatch(BookingActions.selectEmployee({ employeeId: emp.id }));
  }

  onSlotChosen(event: { date: string; slot: string }): void {
    this.selectedDate.set(new Date(event.date + 'T00:00:00'));
    this.selectedSlot.set(event.slot);
  }

  toggleShowAllSpecialists(): void {
    this.showAllSpecialists.update(v => !v);
  }

  proceedToConfirm(): void {
    if (!this.canProceed()) return;
    this.router.navigate(['/booking/confirm'], {
      queryParams: {
        serviceId: this.selectedService()!.id,
        employeeId: this.selectedEmployee()?.id,
        branchId: this.branch()?.id,
        date: this.toIsoDate(this.selectedDate()!),
        slot: this.selectedSlot(),
      },
    });
  }

  // в”Ђв”Ђ Track functions в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

  trackService(_: number, s: ServiceResponse): number { return s.id; }
  trackEmployee(_: number, e: EmployeeVm): number { return e.id; }

  // в”Ђв”Ђ Private helpers в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

  private loadData(): void {
    this.businessService
      .getBusiness(this.businessId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: b => { this.business.set(b); this.loadingBusiness.set(false); },
        error: ()  => this.loadingBusiness.set(false),
      });

    this.businessService
      .getBranches(this.businessId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: branches => {
          this.branch.set(branches.find(br => br.isPrimary) ?? branches[0] ?? null);
        },
      });

    this.businessService
      .getServices(this.businessId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => { this.services.set(page.content); this.loadingServices.set(false); },
        error: ()  => this.loadingServices.set(false),
      });

    this.bookingService
      .getEmployees(this.businessId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          // Map to EmployeeVm; rating will be populated from reviews API when available
          const vms: EmployeeVm[] = page.content
            .filter(e => e.isActive)
            .map(e => ({ ...e, rating: null }));
          this.employees.set(vms);
          this.loadingEmployees.set(false);
        },
        error: () => this.loadingEmployees.set(false),
      });
  }

  private toIsoDate(d: Date): string {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}
