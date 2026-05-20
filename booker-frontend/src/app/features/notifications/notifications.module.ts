import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { NotificationsRoutingModule } from './notifications-routing.module';
import { NotificationsComponent } from './notifications.component';

@NgModule({
  declarations: [NotificationsComponent],
  imports: [SharedModule, NotificationsRoutingModule],
})
export class NotificationsModule {}
