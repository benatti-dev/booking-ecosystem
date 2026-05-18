import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { selectPendingBooking } from '../../../store/booking/booking.selectors';
import { BookingResponse } from '../../../core/booking/booking.service';

@Component({
  selector: 'app-booking-success',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './booking-success.component.html',
  styleUrl: './booking-success.component.scss',
})
export class BookingSuccessComponent implements OnInit {
  private readonly store = inject(Store);
  booking: BookingResponse | null = null;

  ngOnInit(): void {
    this.store.select(selectPendingBooking).subscribe(b => { this.booking = b; });
  }

  formatDateTime(iso: string): string {
    return new Date(iso).toLocaleString('en-US', {
      weekday: 'long', day: 'numeric', month: 'long',
      hour: '2-digit', minute: '2-digit'
    });
  }
}
