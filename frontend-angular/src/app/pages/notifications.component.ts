import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';

@Component({
  standalone: true,
  imports: [FormsModule],
  template: `
    <header class="page-header"><div><p class="eyebrow">Comunicaciones configurables</p><h1>Notificaciones</h1><p class="muted">Define canales LOG/SMTP y plantillas por perfil y evento.</p></div></header>

    <div class="two-column">
      <section class="panel">
        <div class="panel-title"><h2>Canales</h2><button class="btn btn-secondary" (click)="newChannel()">Nuevo</button></div>
        <div class="list compact">
          @for (item of channels; track item.id) {
            <button class="list-row" [class.selected]="item.id===channelForm.id" (click)="editChannel(item)"><span><strong>{{item.name}}</strong><small>{{item.code}} · {{item.type}}</small></span><span class="badge">{{item.active?'Activo':'Off'}}</span></button>
          }
        </div>
        <form class="form-grid" (ngSubmit)="saveChannel()">
          <label>Código<input name="ccode" [(ngModel)]="channelForm.code" required></label>
          <label>Nombre<input name="cname" [(ngModel)]="channelForm.name" required></label>
          <label>Tipo<select name="ctype" [(ngModel)]="channelForm.type"><option>LOG</option><option>SMTP</option></select></label>
          <label>Host<input name="chost" [(ngModel)]="channelForm.host" placeholder="smtp.example.com"></label>
          <label>Puerto<input name="cport" [(ngModel)]="channelForm.port" type="number"></label>
          <label>From<input name="cfrom" [(ngModel)]="channelForm.fromAddress" placeholder="no-reply@example.com"></label>
          <label>Usuario ref<input name="cuser" [(ngModel)]="channelForm.usernameRef" placeholder="ENV:SMTP_USER"></label>
          <label>Password ref<input name="cpass" [(ngModel)]="channelForm.passwordRef" placeholder="ENV:SMTP_PASSWORD"></label>
          <label class="check"><input name="cauth" [(ngModel)]="channelForm.smtpAuth" type="checkbox"> SMTP Auth</label>
          <label class="check"><input name="ctls" [(ngModel)]="channelForm.startTls" type="checkbox"> STARTTLS</label>
          <label class="check"><input name="cactive" [(ngModel)]="channelForm.active" type="checkbox"> Activo</label>
          <div class="actions span-2"><button class="btn btn-primary">Guardar canal</button>@if(channelForm.id){<button type="button" class="btn btn-danger" (click)="deleteChannel()">Eliminar</button>}</div>
        </form>
      </section>

      <section class="panel">
        <div class="panel-title"><h2>Plantillas</h2><button class="btn btn-secondary" (click)="newTemplate()">Nueva</button></div>
        <div class="list compact">
          @for (item of templates; track item.id) {
            <button class="list-row" [class.selected]="item.id===templateForm.id" (click)="editTemplate(item)"><span><strong>{{item.eventType}}</strong><small>{{item.profileCode}} · {{item.channelCode}}</small></span><span class="badge">{{item.active?'Activa':'Off'}}</span></button>
          }
        </div>
        <form class="form-grid" (ngSubmit)="saveTemplate()">
          <label>Perfil<input name="tprofile" [(ngModel)]="templateForm.profileCode" placeholder="* o TELCO_DEMO"></label>
          <label>Evento<select name="tevent" [(ngModel)]="templateForm.eventType"><option>EMAIL_RECEIVED</option><option>CRM_CREATED</option><option>REVIEW_REQUIRED</option><option>CRM_ERROR</option></select></label>
          <label>Canal<input name="tchannel" [(ngModel)]="templateForm.channelCode" required></label>
          <label>Destinatario<input name="trecipient" [(ngModel)]="templateForm.recipientTemplate" required placeholder="{{recipient}}"></label>
          <label class="span-2">Asunto<input name="tsubject" [(ngModel)]="templateForm.subjectTemplate" required></label>
          <label class="span-2">Cuerpo<textarea name="tbody" [(ngModel)]="templateForm.bodyTemplate" rows="8" required></textarea></label>
          <label class="check"><input name="tactive" [(ngModel)]="templateForm.active" type="checkbox"> Activa</label>
          <div class="actions span-2"><button class="btn btn-primary">Guardar plantilla</button>@if(templateForm.id){<button type="button" class="btn btn-danger" (click)="deleteTemplate()">Eliminar</button>}</div>
        </form>
      </section>
    </div>

    <section class="panel lower">
      <h2>Variables disponibles</h2>
      <p class="muted">Recepción: <code>{{ '{{from}}' }}</code>, <code>{{ '{{subject}}' }}</code>, <code>{{ '{{requestId}}' }}</code>. Eventos de sistema: <code>{{ '{{recipient}}' }}</code>, <code>{{ '{{message}}' }}</code>, <code>{{ '{{data.externalTicketId}}' }}</code>.</p>
    </section>
  `
})
export class NotificationsComponent implements OnInit {
  channels:any[]=[]; templates:any[]=[];
  channelForm:any=this.channelDefaults(); templateForm:any=this.templateDefaults();
  constructor(private readonly api:ApiService){}
  ngOnInit():void{this.reload();}
  reload():void{this.api.notificationChannels().subscribe(x=>this.channels=x);this.api.notificationTemplates().subscribe(x=>this.templates=x);}
  newChannel():void{this.channelForm=this.channelDefaults();}
  editChannel(x:any):void{this.channelForm={...x};}
  saveChannel():void{const c=this.channelForm.id?this.api.updateNotificationChannel(this.channelForm.id,this.channelForm):this.api.createNotificationChannel(this.channelForm);c.subscribe(()=>{this.reload();this.newChannel();});}
  deleteChannel():void{if(this.channelForm.id)this.api.deleteNotificationChannel(this.channelForm.id).subscribe(()=>{this.reload();this.newChannel();});}
  newTemplate():void{this.templateForm=this.templateDefaults();}
  editTemplate(x:any):void{this.templateForm={...x};}
  saveTemplate():void{const c=this.templateForm.id?this.api.updateNotificationTemplate(this.templateForm.id,this.templateForm):this.api.createNotificationTemplate(this.templateForm);c.subscribe(()=>{this.reload();this.newTemplate();});}
  deleteTemplate():void{if(this.templateForm.id)this.api.deleteNotificationTemplate(this.templateForm.id).subscribe(()=>{this.reload();this.newTemplate();});}
  private channelDefaults(){return{code:'',name:'',type:'LOG',host:'',port:587,usernameRef:'',passwordRef:'',fromAddress:'',smtpAuth:true,startTls:true,active:true};}
  private templateDefaults(){return{profileCode:'TELCO_DEMO',eventType:'EMAIL_RECEIVED',channelCode:'LOG_DEFAULT',recipientTemplate:'{{from}}',subjectTemplate:'Solicitud recibida - {{requestId}}',bodyTemplate:'Hemos recibido tu solicitud.',active:true};}
}
