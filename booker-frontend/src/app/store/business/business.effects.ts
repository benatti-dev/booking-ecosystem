import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { BusinessService } from '../../core/business/business.service';
import { BusinessActions } from './business.actions';

@Injectable()
export class BusinessEffects {
  private readonly actions$    = inject(Actions);
  private readonly businessSvc = inject(BusinessService);

  loadMyBusinesses$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BusinessActions.loadMyBusinesses),
      switchMap(() =>
        this.businessSvc.getMyBusinesses().pipe(
          map(page => BusinessActions.loadMyBusinessesSuccess({ businesses: page.content })),
          catchError(() =>
            of(BusinessActions.loadMyBusinessesFailure({ error: 'Failed to load your businesses.' }))
          ),
        )
      ),
    )
  );

  loadAllBusinesses$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BusinessActions.loadAllBusinesses),
      switchMap(() =>
        forkJoin({
          pending:   this.businessSvc.getBusinessesByStatus('PENDING',   0, 100),
          active:    this.businessSvc.getBusinessesByStatus('ACTIVE',    0, 100),
          suspended: this.businessSvc.getBusinessesByStatus('SUSPENDED', 0, 100),
          rejected:  this.businessSvc.getBusinessesByStatus('REJECTED',  0, 100),
        }).pipe(
          map(({ pending, active, suspended, rejected }) => {
            const businesses = [
              ...pending.content,
              ...active.content,
              ...suspended.content,
              ...rejected.content,
            ].sort((a, b) => a.name.localeCompare(b.name));
            return BusinessActions.loadAllBusinessesSuccess({ businesses });
          }),
          catchError(() =>
            of(BusinessActions.loadAllBusinessesFailure({ error: 'Failed to load all businesses.' }))
          ),
        )
      ),
    )
  );

  loadPendingBusinesses$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BusinessActions.loadPendingBusinesses),
      switchMap(() =>
        this.businessSvc.getBusinessesByStatus('PENDING', 0, 50).pipe(
          map(page => BusinessActions.loadPendingBusinessesSuccess({ businesses: page.content })),
          catchError(() =>
            of(BusinessActions.loadPendingBusinessesFailure({ error: 'Failed to load pending businesses.' }))
          ),
        )
      ),
    )
  );

  changeBusinessStatus$ = createEffect(() =>
    this.actions$.pipe(
      ofType(BusinessActions.changeBusinessStatus),
      switchMap(({ id, status, reason }) =>
        this.businessSvc.changeBusinessStatus(id, status, reason).pipe(
          map(() => BusinessActions.changeBusinessStatusSuccess({ id, status })),
          catchError(() =>
            of(BusinessActions.changeBusinessStatusFailure({ error: 'Failed to update business status.' }))
          ),
        )
      ),
    )
  );
}
