import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BookingConfirmComponent } from './booking-confirm/booking-confirm.component';
import { BookingSuccessComponent } from './booking-success/booking-success.component';

const routes: Routes = [
  { path: 'confirm', component: BookingConfirmComponent },
  { path: 'success', component: BookingSuccessComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BookingRoutingModule {}
