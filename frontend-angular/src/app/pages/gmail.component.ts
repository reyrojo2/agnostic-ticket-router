import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';

@Component({
 standalone:true,
 imports:[FormsModule],
 template:`
 <header class="page-header"><div><p class="eyebrow">Correo entrante</p><h1>Buzón Gmail</h1>
 <p class="muted">Conecta una cuenta exclusiva de pruebas con OAuth 2.0 de solo lectura.</p></div></header>
 <section class="panel">
 @if(error()){<p class="alert error" role="alert">{{error()}}</p>}
 @if(info()){<p role="status">{{info()}}</p>}
 @if(data();as g){
 <p><strong>Configuración:</strong> {{g.configured?'Disponible':'Completa variables Google Cloud en .env'}}</p>
 <p><strong>Buzón:</strong> {{g.email||'No conectado'}}</p>
 <p><strong>Última consulta:</strong> {{g.lastSync||'Nunca'}}</p>
 @if(g.lastError){<p class="alert error">{{g.lastError}}</p>}
 @if(!g.connected){
 <button class="btn btn-primary" [disabled]="!g.configured" (click)="connect()">Conectar con Google</button>
 } @else {
 <form class="form-grid" (ngSubmit)="save()">
 <label>Perfil<input name="profile" [(ngModel)]="form.profileCode" required></label>
 <label>Filtro Gmail<input name="query" [(ngModel)]="form.query" required></label>
 <label class="check span-2"><input type="checkbox" name="enabled" [(ngModel)]="form.enabled"> Activar lectura automática</label>
 <div class="actions span-2">
 <button class="btn btn-primary" [disabled]="busy()">Guardar</button>
 <button type="button" class="btn btn-secondary" [disabled]="busy()" (click)="sync()">Consultar ahora</button>
 <button type="button" class="btn btn-danger" [disabled]="busy()" (click)="disconnect()">Desconectar</button>
 </div></form>
 }
 <p class="muted">Intervalo: {{g.pollSeconds}} segundos. Los correos no se modifican en Gmail; el conector registra los ID ya procesados.</p>
 } @else {<p>Cargando estado del conector…</p>}
 </section>
 `
})
export class GmailComponent implements OnInit {
 data=signal<any>(null);error=signal('');info=signal('');busy=signal(false);
 form={profileCode:'TELCO_DEMO',query:'in:inbox is:unread',enabled:false};
 constructor(private api:ApiService){}
 ngOnInit(){this.reload();}
 reload(){this.api.gmailStatus().subscribe({next:g=>{this.data.set(g);this.form={profileCode:g.profileCode,query:g.query,enabled:g.enabled};},error:e=>this.error.set('Gmail no disponible (HTTP '+e.status+')')});}
 connect(){this.api.gmailAuthorize().subscribe({next:x=>window.location.assign(x.url),error:e=>this.error.set(e.error?.detail||'Error iniciando OAuth')});}
 save(){this.busy.set(true);this.api.gmailSave(this.form).subscribe({next:g=>{this.data.set(g);this.busy.set(false);this.info.set('Configuración guardada');},error:e=>{this.busy.set(false);this.error.set(e.error?.detail||'Error de guardado');}});}
 sync(){this.busy.set(true);this.api.gmailSync().subscribe({next:x=>{this.busy.set(false);this.info.set('Estado: '+x.status+' · Publicados: '+x.processed);this.reload();},error:e=>{this.busy.set(false);this.error.set('Error HTTP '+e.status);}});}
 disconnect(){if(!window.confirm('¿Desconectar Gmail y detener la lectura?'))return;this.api.gmailDisconnect().subscribe({next:()=>{this.reload();this.info.set('Cuenta desconectada localmente. Revoca además su permiso en Google.');},error:e=>this.error.set('Error HTTP '+e.status)});}
}
