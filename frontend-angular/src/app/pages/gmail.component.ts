import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';

type MailboxMode = 'UNREAD' | 'RECENT' | 'ALL';
const QUERIES: Record<MailboxMode,string> = {
  UNREAD: 'in:inbox is:unread',
  RECENT: 'in:inbox newer_than:1d',
  ALL: 'in:inbox'
};

@Component({
  standalone:true,
  imports:[FormsModule],
  template:`
  <header class="page-header"><div><p class="eyebrow">Correo entrante</p><h1>Buzón Gmail</h1>
  <p class="muted">Conecta tu cuenta de pruebas mediante OAuth 2.0 de solo lectura.</p></div></header>
  <section class="panel">
  @if(error()){<p class="alert error" role="alert">{{error()}}</p>}
  @if(info()){<p role="status">{{info()}}</p>}
  @if(data(); as g){
    <p><strong>Configuración:</strong> {{g.configured?'Disponible':'Configura las credenciales Google Cloud en .env'}}</p>
    <p><strong>Buzón:</strong> {{g.email||'No conectado'}}</p>
    <p><strong>Última consulta:</strong> {{g.lastSync||'Nunca'}}</p>
    @if(g.lastError){<p class="alert error">{{g.lastError}}</p>}
    @if(!g.connected){
      <button class="btn btn-primary" [disabled]="!g.configured" (click)="connect()">Conectar con Google</button>
    } @else {
      <form class="form-grid" (ngSubmit)="save()">
        <label>Perfil de clasificación <input name="profile" [(ngModel)]="form.profileCode" required></label>
        <label>Qué mensajes leer
          <select name="mode" [(ngModel)]="form.mode" (ngModelChange)="legacy.set(false)">
            <option value="UNREAD">Correos no leídos de la bandeja de entrada</option>
            <option value="RECENT">Correos de las últimas 24 horas</option>
            <option value="ALL">Toda la bandeja de entrada</option>
          </select>
        </label>
        <label class="span-2">El asunto contiene (opcional)
          <input name="subject" [(ngModel)]="form.subject" maxlength="100" placeholder="Déjalo vacío para leer cualquier asunto">
        </label>
        @if(legacy()){
          <p class="alert error span-2">Tu filtro anterior era avanzado. Selecciona un alcance de lectura y guarda para reemplazarlo.</p>
        }
        @if(form.mode === 'ALL'){
          <p class="muted span-2">Atención: también incluye correos antiguos. Para las pruebas utiliza una cuenta exclusiva.</p>
        }
        <label class="check span-2"><input type="checkbox" name="enabled" [(ngModel)]="form.enabled">
          Activar lectura automática (cada {{g.pollSeconds}} segundos)
        </label>
        <div class="actions span-2">
          <button class="btn btn-primary" [disabled]="busy() || legacy()">Guardar</button>
          <button type="button" class="btn btn-secondary" [disabled]="busy()" (click)="sync()">Consultar ahora</button>
          <button type="button" class="btn btn-danger" [disabled]="busy()" (click)="disconnect()">Desconectar</button>
        </div>
      </form>
    }
    <p class="muted">Los mensajes no se marcan como leídos. El sistema consulta IDs de Gmail en cada sincronización; solo descarga y publica los que no figuran en PostgreSQL. Los correos existentes no se procesan nuevamente salvo que el registro se pierda o haya un fallo entre publicar y guardar el ID.</p>
  } @else {<p>Cargando estado del conector…</p>}
  </section>
  `
})
export class GmailComponent implements OnInit {
  data=signal<any>(null); error=signal(''); info=signal(''); busy=signal(false); legacy=signal(false);
  form:{profileCode:string;mode:MailboxMode;subject:string;enabled:boolean}={profileCode:'TELCO_DEMO',mode:'UNREAD',subject:'',enabled:false};
  constructor(private api:ApiService){}
  ngOnInit(){this.reload();}
  private parseQuery(q:string){
    const m=q.match(/^(in:inbox(?: is:unread| newer_than:1d)?)(?: subject:"([^"]+)"| subject:([^ ]+))?$/);
    if(!m)return {mode:'UNREAD' as MailboxMode,subject:'',legacy:true};
    const mode=(Object.keys(QUERIES) as MailboxMode[]).find(k=>QUERIES[k]===m[1]);
    return {mode:mode||'UNREAD' as MailboxMode,subject:m[2]||m[3]||'',legacy:!mode};
  }
  private query(){
    const subject=this.form.subject.replace(/["\\\r\n]/g,' ').trim().replace(/\s+/g,' ');
    return QUERIES[this.form.mode]+(subject?' subject:"'+subject+'"':'');
  }
  reload(){
    this.api.gmailStatus().subscribe({next:g=>{
      this.data.set(g);
      const q=this.parseQuery(g.query||QUERIES.UNREAD);
      this.form={profileCode:g.profileCode,mode:q.mode,subject:q.subject,enabled:g.enabled};
      this.legacy.set(q.legacy);
    },error:e=>this.error.set('Gmail no disponible (HTTP '+e.status+')')});
  }
  connect(){this.api.gmailAuthorize().subscribe({next:x=>window.location.assign(x.url),error:e=>this.error.set(e.error?.detail||'Error iniciando OAuth')});}
  save(){
    if(this.legacy())return;
    this.busy.set(true);
    this.api.gmailSave({profileCode:this.form.profileCode,query:this.query(),enabled:this.form.enabled}).subscribe({
      next:g=>{this.data.set(g);this.busy.set(false);this.error.set('');this.info.set('Configuración guardada');},
      error:e=>{this.busy.set(false);this.error.set(e.error?.detail||'Error de guardado');}
    });
  }
  sync(){this.busy.set(true);this.api.gmailSync().subscribe({
    next:x=>{this.busy.set(false);this.info.set('Estado: '+x.status+' · Consultados: '+(x.listed??0)+' · Ya procesados (omitidos): '+(x.alreadyProcessed??0)+' · Nuevos publicados: '+x.processed);this.reload();},
    error:e=>{this.busy.set(false);this.error.set('Error HTTP '+e.status);}
  });}
  disconnect(){
    if(!window.confirm('¿Desconectar Gmail y detener la lectura?'))return;
    this.api.gmailDisconnect().subscribe({next:()=>{this.reload();this.info.set('Cuenta desconectada localmente. Puedes revocar su acceso en Google.');},error:e=>this.error.set('Error HTTP '+e.status)});
  }
}
