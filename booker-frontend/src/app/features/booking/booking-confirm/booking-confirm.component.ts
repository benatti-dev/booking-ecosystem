import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { BookingActions } from '../../../store/booking/booking.actions';
import {
  selectPendingBooking, selectCreating, selectCreateError,
} from '../../../store/booking/booking.selectors';
import { BusinessService } from '../../../core/business/business.service';
import { BookingService } from '../../../core/booking/booking.service';

@Component({
  selector: 'app-booking-confirm',
  templateUrl: './booking-confirm.component.html',
  styleUrl: './booking-confirm.component.scss',
  standalone: false,
})
export class BookingConfirmComponent implements OnInit, OnDestroy {
  clientNoteCtrl = new FormControl('');

  creating = false;
  createError: string | null = null;

  serviceId!: number;
  employeeId?: number;
  branchId!: number;
  date!: string;
  slot!: string;

  serviceName = '';
  employeeName = '';
  price = '';

  private sub = new Subscription();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly store: Store,
    private readonly businessService: BusinessService,
    private readonly bookingService: BookingService,
  ) {}

  ngOnInit(): void {
    const p = this.route.snapshot.queryParams;
    this.serviceId = Number(p['serviceId']);
    this.employeeId = p['employeeId'] ? Number(p['employeeId']) : undefined;
    this.branchId = Number(p['branchId']);
    this.date = p['date'];
    this.slot = p['slot'];

    this.sub.add(this.store.select(selectCreating).subscribe(v => (this.creating = v)));
    this.sub.add(this.store.select(selectCreateError).subscribe(v => (this.createError = v)));

    // Navigate to success once booking is created
    this.sub.add(
      this.store.select(selectPendingBooking).subscribe(booking => {
        if (booking) {
          this.router.navigate(['/booking/success'], { queryParams: { bookingId: booking.id } });
        }
      }),
    );
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
      hour: '2-digit', minute: '2-digit',
    });
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}
