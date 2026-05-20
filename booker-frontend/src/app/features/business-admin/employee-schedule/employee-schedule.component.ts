import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { BookingService, EmployeeResponse } from '../../../core/booking/booking.service';

@Component({
  selector: 'app-employee-schedule',
  templateUrl: './employee-schedule.component.html',
  styleUrl: './employee-schedule.component.scss',
  standalone: false,
})
export class EmployeeScheduleComponent implements OnInit {
  businessId!: number;
  employeeId!: number;
  employee: EmployeeResponse | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly bookingService: BookingService,
  ) {}

  ngOnInit(): void {
    this.businessId = +this.route.snapshot.paramMap.get('businessId')!;
    this.employeeId = +this.route.snapshot.paramMap.get('employeeId')!;
    this.bookingService.getEmployee(this.businessId, this.employeeId).subscribe({
      next: emp => (this.employee = emp),
      error: () => {},
    });
  }

  back(): void {
    this.router.navigate([`/business/${this.businessId}/employees`]);
  }
}
