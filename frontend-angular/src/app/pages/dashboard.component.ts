import { Component, ElementRef, OnInit, computed, signal, viewChild } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { ApiService } from '../core/api.service';
import { OperationalMetrics, RequestTrace, stateLabel } from '../core/metrics-models';
import { RequestDetailComponent } from './request-detail.component';

@Component({
  standalone: true, imports: [SlicePipe, RequestDetailComponent],
  templateUrl: './dashboard.component.html', styleUrls: ['./crm-workflow.css', './dashboard.css']
})
export class DashboardComponent implements OnInit {
  metrics = signal<OperationalMetrics | null>(null);
  error = signal(''); loading = signal(false); selectedId = signal<string | null>(null);
  selected = computed(() => this.metrics()?.recent.find(trace => trace.requestId === this.selectedId()));
  detail = viewChild<ElementRef<HTMLElement>>('requestDetail');
  stateLabel = stateLabel;
  constructor(private api: ApiService) {}
  ngOnInit() { this.load(); }
  load() {
    if (this.loading()) return;
    this.error.set(''); this.loading.set(true);
    this.api.operationalMetrics().subscribe({
      next: metrics => { this.metrics.set(metrics); this.loading.set(false); },
      error: () => { this.error.set('No se pudo cargar el seguimiento de solicitudes. Intenta actualizar de nuevo.'); this.loading.set(false); }
    });
  }
  showDetails(trace: RequestTrace) {
    this.selectedId.set(trace.requestId);
    setTimeout(() => { this.detail()?.nativeElement.focus(); this.detail()?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' }); });
  }
  date(value: string) {
    return new Intl.DateTimeFormat('es-PE', { timeZone: this.metrics()?.timezone || 'America/Lima', dateStyle: 'short', timeStyle: 'medium' }).format(new Date(value));
  }
}
