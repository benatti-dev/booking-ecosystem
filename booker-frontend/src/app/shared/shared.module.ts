import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { NavbarComponent } from './components/navbar/navbar.component';
import { StatusBadgeComponent } from './components/status-badge/status-badge.component';
import { PaginationComponent } from './components/pagination/pagination.component';
import { LoadingSpinnerComponent } from './components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from './components/empty-state/empty-state.component';
import { AlertComponent } from './components/alert/alert.component';
import { FormFieldComponent } from './components/form-field/form-field.component';
import { ConfirmDialogComponent } from './components/confirm-dialog/confirm-dialog.component';
import { DateTimePipe } from './pipes/date-time.pipe';
import { DistancePipe } from './pipes/distance.pipe';

@NgModule({
  declarations: [
    NavbarComponent,
    StatusBadgeComponent,
    PaginationComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    AlertComponent,
    FormFieldComponent,
    ConfirmDialogComponent,
    DateTimePipe,
    DistancePipe,
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
  ],
  exports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    NavbarComponent,
    StatusBadgeComponent,
    PaginationComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    AlertComponent,
    FormFieldComponent,
    ConfirmDialogComponent,
    DateTimePipe,
    DistancePipe,
  ],
})
export class SharedModule {}
