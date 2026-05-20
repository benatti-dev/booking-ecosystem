import { inject, Injectable } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { AdminService } from '../../core/admin/admin.service';
import { AnalyticsService } from '../../core/analytics/analytics.service';
import { AdminActions } from './admin.actions';

@Injectable()
export class AdminEffects {

  private readonly actions$      = inject(Actions);
  private readonly adminSvc      = inject(AdminService);
  private readonly analyticsSvc  = inject(AnalyticsService);

  loadPlatformOverview$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AdminActions.loadPlatformOverview),
      switchMap(() =>
        this.analyticsSvc.getPlatformOverview().pipe(
          map(overview => AdminActions.loadPlatformOverviewSuccess({ overview })),
          catchError(() =>
            of(AdminActions.loadPlatformOverviewFailure({ error: 'Failed to load platform overview.' }))
          ),
        )
      ),
    )
  );

  loadUsers$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AdminActions.loadUsers),
      switchMap(({ page, role, status }) =>
        this.adminSvc.listUsers(
          page ?? 0,
          20,
          role as any,
          status as any,
        ).pipe(
          map(res => AdminActions.loadUsersSuccess({
            users: res.content,
            total: res.totalElements,
            totalPages: res.totalPages,
          })),
          catchError(() =>
            of(AdminActions.loadUsersFailure({ error: 'Failed to load users.' }))
          ),
        )
      ),
    )
  );

  changeUserStatus$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AdminActions.changeUserStatus),
      switchMap(({ userId, status }) =>
        this.adminSvc.changeUserStatus(userId, status).pipe(
          map(user => AdminActions.changeUserStatusSuccess({ user })),
          catchError(() =>
            of(AdminActions.changeUserStatusFailure({ error: 'Failed to update user status.' }))
          ),
        )
      ),
    )
  );

  loadAuditLogs$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AdminActions.loadAuditLogs),
      switchMap(({ page, action, from, to }) =>
        this.adminSvc.listAuditLogs(page ?? 0, 50, action, from, to).pipe(
          map(res => AdminActions.loadAuditLogsSuccess({
            logs: res.content,
            total: res.totalElements,
            totalPages: res.totalPages,
          })),
          catchError(() =>
            of(AdminActions.loadAuditLogsFailure({ error: 'Failed to load audit logs.' }))
          ),
        )
      ),
    )
  );
}
