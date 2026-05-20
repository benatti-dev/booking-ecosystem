import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { BookingDetailRoutingModule } from './booking-detail-routing.module';

import { BusinessDetailComponent } from './business-detail/business-detail.component';
import { SlotPickerComponent } from './slot-picker/slot-picker.component';
import { BookingBusinessCardComponent } from './business-detail/components/business-card/business-card.component';
import { BookingServiceCardComponent } from './business-detail/components/service-card/service-card.component';
import { BookingSpecialistCardComponent } from './business-detail/components/specialist-card/specialist-card.component';
import { BookingDateTimePickerComponent } from './business-detail/components/date-time-picker/date-time-picker.component';
import { BookingSummaryBarComponent } from './business-detail/components/booking-summary-bar/booking-summary-bar.component';

@NgModule({
  declarations: [
    BusinessDetailComponent,
    SlotPickerComponent,
    BookingBusinessCardComponent,
    BookingServiceCardComponent,
    BookingSpecialistCardComponent,
    BookingDateTimePickerComponent,
    BookingSummaryBarComponent,
  ],
  imports: [SharedModule, BookingDetailRoutingModule],
})
export class BookingDetailModule {}
