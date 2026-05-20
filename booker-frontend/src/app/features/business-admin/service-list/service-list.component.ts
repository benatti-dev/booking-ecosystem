import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BusinessService, ServiceResponse, Page } from '../../../core/business/business.service';

@Component({
  selector: 'app-service-list',
  templateUrl: './service-list.component.html',
  standalone: false,
})
export class ServiceListComponent implements OnInit {
  businessId = signal(0);
  services   = signal<ServiceResponse[]>([]);
  loading    = signal(true);
  error      = signal<string | null>(null);

  constructor(
    private readonly businessSvc: BusinessService,
    private readonly route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('businessId'));
    this.businessId.set(id);

    this.businessSvc.getServices(id).subscribe({
      next: (page: Page<ServiceResponse>) => {
        this.services.set(page.content);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load services.');
        this.loading.set(false);
      }
    });
  }

  deactivate(serviceId: number): void {
    if (!confirm('Deactivate this service?')) return;
    this.businessSvc.deactivateService(this.businessId(), serviceId).subscribe({
      next: () => this.services.update(s => s.filter(x => x.id !== serviceId)),
      error: () => alert('Failed to deactivate service.')
    });
  }

  formatDuration(min: number): string {
    if (min < 60) return `${min} min`;
    const h = Math.floor(min / 60);
    const m = min % 60;
    return m ? `${h}h ${m}min` : `${h}h`;
  }
}
