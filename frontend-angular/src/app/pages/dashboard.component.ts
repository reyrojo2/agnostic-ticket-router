import { Component, OnInit, signal } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { ApiService } from '../core/api.service';
@Component({
 standalone:true, imports:[SlicePipe],
 template:`
 <header class="page-header"><div><p class="eyebrow">Indicadores operativos</p><h1>Procesamiento de solicitudes</h1>
 <p class="muted">Últimos 7 días · Zona horaria {{metrics()?.timezone||'America/Lima'}} · Datos registrados desde RabbitMQ</p></div>
 <button class="btn btn-secondary" (click)="load()">Actualizar</button></header>
 @if(error()){<p class="alert error" role="alert">{{error()}}</p>}
 @if(metrics();as m){
 <div class="stat-grid">
 <article class="stat"><span>Recibidos hoy</span><strong>{{m.todayReceived}}</strong><small>{{m.received}} en los últimos 7 días</small></article>
 <article class="stat"><span>Solo reglas</span><strong>{{m.ruleOnly}}</strong><small>{{m.ruleDimensions}} decisiones determinísticas</small></article>
 <article class="stat"><span>Con SystemOne</span><strong>{{m.aiUsed}}</strong><small>{{m.aiDimensions}} decisiones IA</small></article>
 <article class="stat"><span>Baja confianza</span><strong>{{m.lowConfidence}}</strong><small>Al menos una decisión no aceptada</small></article>
 <article class="stat"><span>Casos creados</span><strong>{{m.crmCreated}}</strong><small>Confirmados por adaptador CRM</small></article>
 <article class="stat"><span>Revisión humana</span><strong>{{m.review}}</strong><small>Antes de enviar al CRM</small></article>
 <article class="stat"><span>Errores CRM</span><strong>{{m.crmError}}</strong><small>Solicitudes con fallo</small></article>
 <article class="stat"><span>Pendientes</span><strong>{{m.pending}}</strong><small>Sin resultado final confirmado</small></article>
 </div>
 <section class="panel"><h2>Volumen diario</h2>
 <div class="table-wrap"><table><thead><tr><th>Día</th><th>Recibidos</th><th>Creados</th><th>Revisión</th><th>Errores</th></tr></thead>
 <tbody>@for(d of m.daily;track d.day){<tr><td>{{d.day}}</td><td>{{d.received}}</td><td>{{d.created}}</td><td>{{d.review}}</td><td>{{d.errors}}</td></tr>}</tbody></table></div></section>
 <section class="panel lower"><h2>Solicitudes recientes</h2>
 <div class="table-wrap"><table><thead><tr><th>Hora</th><th>ID</th><th>Origen</th><th>Estado</th><th>Reglas</th><th>IA</th><th>Rechazadas</th><th>Case</th></tr></thead>
 <tbody>@for(x of m.recent;track x.requestId){<tr>
 <td>{{x.at|slice:0:19}}</td><td class="mono">{{x.requestId|slice:0:8}}</td>
 <td>{{x.source}}</td><td>{{x.state}}</td><td>{{x.rules}}</td><td>{{x.ai}}</td><td>{{x.rejected}}</td><td>{{x.externalId||'—'}}</td>
 </tr>}</tbody></table></div></section>
 } @else {<section class="panel">Consultando métricas...</section>}
 `
})
export class DashboardComponent implements OnInit {
 metrics=signal<any>(null);error=signal('');
 constructor(private api:ApiService){}
 ngOnInit(){this.load();}
 load(){this.error.set('');this.api.operationalMetrics().subscribe({next:x=>this.metrics.set(x),error:e=>this.error.set('Métricas no disponibles (HTTP '+e.status+'). Consulta docker compose logs observability-service.')});}
}
