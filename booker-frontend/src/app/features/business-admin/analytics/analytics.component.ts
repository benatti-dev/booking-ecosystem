import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { AnalyticsService, BusinessAnalyticsResponse } from '../../../core/analytics/analytics.service';

@Component({
  selector: 'app-analytics',
  templateUrl: './analytics.component.html',
  standalone: false,
})
export class AnalyticsComponent implements OnInit, OnDestroy {

  analytics: BusinessAnalyticsResponse | null = null;
  loading = false;
  error: string | null = null;

  private sub = new Subscription();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly analyticsSvc: AnalyticsService,
  ) {}

  ngOnInit(): void {
    this.loading = true;
    this.sub.add(
      this.route.params.pipe(
        switchMap(params => {
          const businessId = Number(params['businessId']);
          return this.analyticsSvc.getBusinessAnalytics(businessId);
        }),
      ).subscribe({
        next: data => {
          this.analytics = data;
          this.loading = false;
        },
        error: () => {
          this.error = 'Failed to load analytics. Please try again.';
          this.loading = false;
        },
      })
    );
  }

  get maxDailyBookings(): number {
    if (!this.analytics || !this.analytics.dailyBookings.length) return 1;
    return Math.max(...this.analytics.dailyBookings.map(d => d.totalBookings), 1);
  }

  /**
   * Returns a width percentage for the revenue bar chart.
   * Scales each service's revenue relative to the highest revenue service.
   */
  revenueBarWidth(revenue: number): string {
    if (!this.analytics || !this.analytics.revenueByService.length) return '0%';
    const max = Math.max(...this.analytics.revenueByService.map(s => s.totalRevenue));
    if (max === 0) return '0%';
    return `${Math.round((revenue / max) * 100)}%`;
  }

  /**
   * Returns a width percentage for the employee utilization bar chart.
   */
  utilizationBarWidth(count: number): string {
    if (!this.analytics || !this.analytics.employeeUtilization.length) return '0%';
    const max = Math.max(...this.analytics.employeeUtilization.map(e => e.completedBookings));
    if (max === 0) return '0%';
    return `${Math.round((count / max) * 100)}%`;
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }
}
