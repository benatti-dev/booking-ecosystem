import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { BusinessDetailComponent } from './business-detail/business-detail.component';

const routes: Routes = [
  { path: '', component: BusinessDetailComponent },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BookingDetailRoutingModule {}
