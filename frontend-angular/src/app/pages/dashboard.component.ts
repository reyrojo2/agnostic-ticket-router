import { Component, OnInit, signal } from '@angular/core';
import { ApiService } from '../core/api.service';

@Component({
  standalone: true,
  template: `
    <header class="page-header"><div><p class="eyebrow">Vista general</p><h1>Dashboard</h1><p class="muted">Configuración activa del MVP académico.</p></div></header>
    @if (errors().length) { <p class="alert error" role="alert">No se pudieron consultar: {{ errors().join(", ") }}. Comprueba los servicios y vuelve a cargar.</p> }
    <div class="stat-grid">
      <article class="stat"><span>Perfiles de decisión</span><strong>{{ profiles() }}</strong><small>Business Knowledge</small></article>
      <article class="stat"><span>Integraciones CRM</span><strong>{{ integrations() }}</strong><small>Adaptadores configurados</small></article>
      <article class="stat"><span>Rutas CRM</span><strong>{{ routes() }}</strong><small>Enrutamiento dinámico</small></article>
      <article class="stat"><span>Plantillas</span><strong>{{ templates() }}</strong><small>Notificaciones</small></article>
    </div>
    <section class="panel">
      <h2>Flujo implementado</h2>
      <div class="flow">
        <span>Correo</span><b>→</b><span>RabbitMQ</span><b>→</b><span>Decision Engine</span><b>→</b><span>Standard Ticket</span><b>→</b><span>CRM Adapter</span><b>→</b><span>Notificación</span>
      </div>
    </section>
  `
})
export class DashboardComponent implements OnInit {
  profiles = signal(0);
  integrations = signal(0);
  routes = signal(0);
  templates = signal(0);
  errors = signal<string[]>([]);

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    // Independent requests: one unavailable service must not hide every counter.
    this.api.profiles().subscribe({
      next: data => this.profiles.set(data.length),
      error: () => this.addError('Business Knowledge')
    });
    this.api.crmIntegrations().subscribe({
      next: data => this.integrations.set(data.length),
      error: () => this.addError('Integraciones CRM')
    });
    this.api.crmRoutes().subscribe({
      next: data => this.routes.set(data.length),
      error: () => this.addError('Rutas CRM')
    });
    this.api.notificationTemplates().subscribe({
      next: data => this.templates.set(data.length),
      error: () => this.addError('Notificaciones')
    });
  }

  private addError(service: string): void {
    this.errors.update(values => [...values, service]);
  }
}
