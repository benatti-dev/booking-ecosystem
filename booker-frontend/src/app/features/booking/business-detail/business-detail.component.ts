import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { switchMap, of } from 'rxjs';
import { BusinessService, ServiceResponse } from '../../../core/business/business.service';
import { BookingService, EmployeeResponse } from '../../../core/booking/booking.service';
import { BookingActions } from '../../../store/booking/booking.actions';
import { SlotPickerComponent } from '../slot-picker/slot-picker.component';

@Component({
  selector: 'app-business-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, SlotPickerComponent],
  templateUrl: './business-detail.component.html',
  styleUrl: './business-detail.component.scss',
})
export class BusinessDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly businessService = inject(BusinessService);
  private readonly bookingService = inject(BookingService);
  private readonly store = inject(Store);

  business: any = null;
  services: ServiceResponse[] = [];
  employees: EmployeeResponse[] = [];
  selectedService: ServiceResponse | null = null;
  selectedEmployee: EmployeeResponse | null = null;
  selectedSlot: string | null = null;
  selectedDate: string | null = null;
  loading = true;
  hasEmployeeResource = false;

  ngOnInit(): void {
    const businessId = Number(this.route.snapshot.paramMap.get('businessId'));
    this.store.dispatch(BookingActions.resetBookingFlow());

    this.businessService.getBusiness(businessId).subscribe({
      next: b => {
        this.business = b;
        this.hasEmployeeResource = b.category?.resourceType === 'EMPLOYEE';
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });

    this.businessService.getServices(businessId).subscribe({
      next: page => { this.services = page.content; }
    });

    this.bookingService.getEmployees(businessId).subscribe({
      next: page => { this.employees = page.content.filter(e => e.isActive); }
    });
  }

  selectService(svc: ServiceResponse): void {
    this.selectedService = svc;
    this.selectedSlot = null;
    this.selectedEmployee = null;
  }

  selectEmployee(emp: EmployeeResponse): void {
    this.selectedEmployee = emp;
    this.selectedSlot = null;
    this.store.dispatch(BookingActions.selectEmployee({ employeeId: emp.id }));
  }

  onSlotSelected(event: { date: string; slot: string }): void {
    this.selectedDate = event.date;
    this.selectedSlot = event.slot;
  }

  proceedToConfirm(): void {
    const businessId = this.business?.id;
    this.router.navigate(['/booking/confirm'], {
      queryParams: {
        serviceId: this.selectedService?.id,
        employeeId: this.selectedEmployee?.id,
        branchId: this.business?.primaryBranchId,
        date: this.selectedDate,
        slot: this.selectedSlot,
      }
    });
  }
}
