import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { BookingActions } from '../../../store/booking/booking.actions';
import {
  selectPendingBooking, selectCreating, selectCreateError
} from '../../../store/booking/booking.selectors';
import { BusinessService } from '../../../core/business/business.service';
import { BookingService } from '../../../core/booking/booking.service';

@Component({
  selector: 'app-booking-confirm',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './booking-confirm.component.html',
  styleUrl: './booking-confirm.component.scss',
})
export class BookingConfirmComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly store = inject(Store);
  private readonly businessService = inject(BusinessService);
  private readonly bookingService = inject(BookingService);

  clientNoteCtrl = new FormControl('');

  readonly creating$ = this.store.select(selectCreating);
  readonly error$ = this.store.select(selectCreateError);

  serviceId!: number;
  employeeId?: number;
  branchId!: number;
  date!: string;
  slot!: string;

  serviceName = '';
  employeeName = '';
  price = '';

  ngOnInit(): void {
    const p = this.route.snapshot.queryParams;
    this.serviceId = Number(p['serviceId']);
    this.employeeId = p['employeeId'] ? Number(p['employeeId']) : undefined;
    this.branchId = Number(p['branchId']);
    this.date = p['date'];
    this.slot = p['slot'];

    // Listen for successful booking creation
    this.store.select(selectPendingBooking).subscribe(booking => {
      if (booking) {
        this.router.navigate(['/booking/success'], { queryParams: { bookingId: booking.id } });
      }
    });
  }

  confirm(): void {
    // Build ISO 8601 startTime from date + slot
    const startTime = `${this.date}T${this.slot}+00:00`;

    this.store.dispatch(BookingActions.createBooking({
      request: {
        serviceId: this.serviceId,
        employeeId: this.employeeId,
        branchId: this.branchId,
        startTime,
        clientNote: this.clientNoteCtrl.value || undefined,
        selectedAttributes: {},
      }
    }));
  }

  formatDateTime(date: string, slot: string): string {
    if (!date || !slot) return '';
    const d = new Date(`${date}T${slot}`);
    return d.toLocaleString('en-US', {
      weekday: 'long', day: 'numeric', month: 'long',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
