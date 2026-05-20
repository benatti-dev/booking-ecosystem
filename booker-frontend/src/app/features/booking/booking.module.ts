import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { BookingRoutingModule } from './booking-routing.module';
import { BookingConfirmComponent } from './booking-confirm/booking-confirm.component';
import { BookingSuccessComponent } from './booking-success/booking-success.component';

@NgModule({
  declarations: [BookingConfirmComponent, BookingSuccessComponent],
  imports: [SharedModule, BookingRoutingModule],
})
export class BookingModule {}
