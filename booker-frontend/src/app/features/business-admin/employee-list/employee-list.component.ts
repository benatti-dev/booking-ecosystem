import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { BookingService, EmployeeResponse } from '../../../core/booking/booking.service';

@Component({
  selector: 'app-employee-list',
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.scss',
  standalone: false,
})
export class EmployeeListComponent implements OnInit, OnDestroy {
  businessId!: number;
  employees: EmployeeResponse[] = [];
  loading = false;
  error: string | null = null;
  deactivatingId: number | null = null;

  private sub = new Subscription();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly bookingService: BookingService,
  ) {}

  ngOnInit(): void {
    this.businessId = +this.route.snapshot.paramMap.get('businessId')!;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.sub.add(
      this.bookingService.getEmployees(this.businessId).subscribe({
        next: page => { this.employees = page.content; this.loading = false; },
        error: () => { this.error = 'Failed to load employees'; this.loading = false; },
      })
    );
  }

  openSchedule(employeeId: number): void {
    this.router.navigate([`/business/${this.businessId}/employees/${employeeId}/schedule`]);
  }

  addEmployee(): void {
    this.router.navigate([`/business/${this.businessId}/employees/new`]);
  }

  deactivate(employee: EmployeeResponse): void {
    if (!confirm(`Deactivate ${employee.displayName}?`)) return;
    this.deactivatingId = employee.id;
    this.sub.add(
      this.bookingService.deactivateEmployee(this.businessId, employee.id).subscribe({
        next: updated => {
          this.employees = this.employees.map(e => e.id === updated.id ? updated : e);
          this.deactivatingId = null;
        },
        error: () => {
          this.error = 'Failed to deactivate employee';
          this.deactivatingId = null;
        },
      })
    );
  }

  ngOnDestroy(): void { this.sub.unsubscribe(); }
}
