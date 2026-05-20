import { Component, OnDestroy, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Subscription } from 'rxjs';
import { selectPendingBooking } from '../../../store/booking/booking.selectors';
import { BookingResponse } from '../../../core/booking/booking.service';

@Component({
  selector: 'app-booking-success',
  templateUrl: './booking-success.component.html',
  styleUrl: './booking-success.component.scss',
  standalone: false,
})
export class BookingSuccessComponent implements OnInit, OnDestroy {
  booking: BookingResponse | null = null;

  private sub = new Subscription();

  constructor(private readonly store: Store) {}

  ngOnInit(): void {
    this.sub.add(
      this.store.select(selectPendingBooking).subscribe(b => (this.booking = b)),
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}
