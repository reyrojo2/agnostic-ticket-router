import { Component, OnInit } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ApiService } from '../core/api.service';

@Component({
  standalone: true,
  template: `
    <header class="page-header"><div><p class="eyebrow">Vista general</p><h1>Dashboard</h1><p class="muted">Configuración activa del MVP académico.</p></div></header>
    <div class="stat-grid">
      <article class="stat"><span>Perfiles de decisión</span><strong>{{ profiles }}</strong><small>Business Knowledge</small></article>
      <article class="stat"><span>Integraciones CRM</span><strong>{{ integrations }}</strong><small>Adaptadores configurados</small></article>
      <article class="stat"><span>Rutas CRM</span><strong>{{ routes }}</strong><small>Enrutamiento dinámico</small></article>
      <article class="stat"><span>Plantillas</span><strong>{{ templates }}</strong><small>Notificaciones</small></article>
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
  profiles = 0;
  integrations = 0;
  routes = 0;
  templates = 0;

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void {
    forkJoin({
      profiles: this.api.profiles(),
      integrations: this.api.crmIntegrations(),
      routes: this.api.crmRoutes(),
      templates: this.api.notificationTemplates()
    }).subscribe({
      next: data => {
        this.profiles = data.profiles.length;
        this.integrations = data.integrations.length;
        this.routes = data.routes.length;
        this.templates = data.templates.length;
      }
    });
  }
}
