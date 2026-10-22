import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';

@Component({
  standalone: true,
  imports: [FormsModule],
  template: `
    <header class="page-header"><div><p class="eyebrow">Adaptador universal</p><h1>Integraciones CRM</h1><p class="muted">1. El motor clasifica en Business Knowledge / Decision Engine. 2. Las rutas eligen el CRM. 3. La plantilla indica qué datos guardar en el caso.</p></div></header>

    <div class="two-column">
      <section class="panel">
        <div class="panel-title"><h2>Integraciones</h2><button class="btn btn-secondary" (click)="newIntegration()">Nueva</button></div>
        <div class="list compact">
          @for (item of integrations(); track item.id) {
            <button class="list-row" [class.selected]="item.id === selectedIntegrationId" (click)="selectIntegration(item)">
              <span><strong>{{ item.name }}</strong><small>{{ item.code }} · {{ item.authType }}</small></span><span class="badge">{{ item.active ? 'Activa' : 'Off' }}</span>
            </button>
          }
        </div>
        <form class="form-grid" (ngSubmit)="saveIntegration()">
          <label>Código<input name="icode" [(ngModel)]="integrationForm.code" required></label>
          <label>Nombre<input name="iname" [(ngModel)]="integrationForm.name" required></label>
          <label class="span-2">Base URL<input name="ibase" [(ngModel)]="integrationForm.baseUrl" required placeholder="https://crm.example.com"></label>
          <label>Auth<select name="iauth" [(ngModel)]="integrationForm.authType"><option>NONE</option><option>BEARER</option><option>BASIC</option><option>API_KEY_HEADER</option><option>OAUTH2_CLIENT_CREDENTIALS</option></select></label>
          <label>API Key header<input name="ikey" [(ngModel)]="integrationForm.apiKeyName" placeholder="X-API-Key"></label>
          <label>Usuario ref<input name="iuser" [(ngModel)]="integrationForm.usernameRef" placeholder="ENV:CRM_USER"></label>
          <label>Secreto ref<input name="isecret" [(ngModel)]="integrationForm.secretRef" placeholder="ENV:CRM_TOKEN"></label>
          <label class="span-2">OAuth Token URL<input name="itoken" [(ngModel)]="integrationForm.tokenUrl" placeholder="https://idp.example.com/oauth/token"></label>
          <label class="span-2">OAuth Scope<input name="iscope" [(ngModel)]="integrationForm.scope" placeholder="tickets.write"></label>
          <label class="span-2">Headers base JSON<textarea name="iheaders" [(ngModel)]="integrationForm.defaultHeadersJson" rows="3"></textarea></label>
          <label class="check"><input name="iactive" [(ngModel)]="integrationForm.active" type="checkbox"> Activa</label>
          <div class="actions span-2"><button class="btn btn-primary">Guardar integración</button>@if(integrationForm.id){<button type="button" class="btn btn-danger" (click)="deleteIntegration()">Eliminar</button>}</div>
        </form>
      </section>

      <section class="panel">
        <div class="panel-title"><h2>Operaciones</h2><button class="btn btn-secondary" [disabled]="!selectedIntegrationId" (click)="newOperation()">Nueva</button></div>
        <div class="list compact">
          @for (operation of operations(); track operation.id) {
            <button class="list-row" [class.selected]="operation.id === operationForm.id" (click)="editOperation(operation)"><span><strong>{{ operation.code }}</strong><small>{{ operation.httpMethod }} {{ operation.pathTemplate }}</small></span><span>{{ operation.successStatusCodes }}</span></button>
          }
        </div>
        @if (selectedIntegrationId) {
          <form class="form-grid" (ngSubmit)="saveOperation()">
            <label>Código<input name="ocode" [(ngModel)]="operationForm.code" required></label>
            <label>Método<select name="omethod" [(ngModel)]="operationForm.httpMethod"><option>POST</option><option>PUT</option><option>PATCH</option><option>DELETE</option><option>GET</option></select></label>
            <label class="span-2">Path<input name="opath" [(ngModel)]="operationForm.pathTemplate" required placeholder="/api/tickets"></label>
            <label>Content-Type<input name="ocontent" [(ngModel)]="operationForm.contentType"></label>
            <label>Status éxito<input name="ostatus" [(ngModel)]="operationForm.successStatusCodes" placeholder="200,201"></label>
            <label class="span-2">Headers JSON<textarea name="oheaders" [(ngModel)]="operationForm.headersTemplateJson" rows="2"></textarea></label>
            <label class="span-2">Body template JSON<textarea name="obody" [(ngModel)]="operationForm.bodyTemplate" rows="10" class="mono"></textarea></label>
            <p class="muted span-2">EspoCRM solo muestra los campos que recibe y conoce. Usa <code>{{ '{{classificationSummary}}' }}</code> para incluir todas las dimensiones en la descripción; puedes mapear <code>{{ '{{decisions.DESTINO.value}}' }}</code> a un campo personalizado si ese campo ya existe en EspoCRM. El equipo asignado requiere un ID de equipo real y el campo <code>teamsIds</code>.</p>
            <div class="actions span-2"><button type="button" class="btn btn-secondary" (click)="applyEspoTemplate()">Usar plantilla EspoCRM con clasificación</button></div>
            <label>ID externo path<input name="oid" [(ngModel)]="operationForm.externalIdPath" placeholder="data.caseNumber"></label>
            <label>Timeout ms<input name="otimeout" [(ngModel)]="operationForm.timeoutMs" type="number"></label>
            <label>Intentos<input name="oattempts" [(ngModel)]="operationForm.maxAttempts" type="number"></label>
            <label>Espera retry ms<input name="oretry" [(ngModel)]="operationForm.retryDelayMs" type="number"></label>
            <label class="check"><input name="oactive" [(ngModel)]="operationForm.active" type="checkbox"> Activa</label>
            <div class="actions span-2"><button class="btn btn-primary">Guardar operación</button>@if(operationForm.id){<button type="button" class="btn btn-danger" (click)="deleteOperation()">Eliminar</button>}</div>
          </form>
        }
      </section>
    </div>

    <section class="panel lower">
      <div class="panel-title"><div><h2>Rutas de integración</h2><p class="muted">Una ruta elige integración/operación según una dimensión. No crea automáticamente equipos ni campos en EspoCRM.</p></div><button class="btn btn-secondary" (click)="newRoute()">Nueva</button></div>
      <div class="table-wrap"><table><thead><tr><th>Perfil</th><th>Condición</th><th>Integración</th><th>Operación</th><th>Prioridad</th><th></th></tr></thead><tbody>
        @for (route of routes(); track route.id) {<tr><td>{{route.profileCode}}</td><td>{{route.dimensionCode || 'DEFAULT'}} {{route.optionCode || ''}}</td><td>{{route.integrationCode}}</td><td>{{route.operationCode}}</td><td>{{route.priority}}</td><td><button class="link" (click)="editRoute(route)">Editar</button></td></tr>}
      </tbody></table></div>
      <form class="form-grid route-form" (ngSubmit)="saveRoute()">
        <label>Perfil<input name="rprofile" [(ngModel)]="routeForm.profileCode" required></label>
        <label>Dimensión<input name="rdim" [(ngModel)]="routeForm.dimensionCode" placeholder="DESTINO"></label>
        <label>Opción<input name="ropt" [(ngModel)]="routeForm.optionCode" placeholder="BACKOFFICE"></label>
        <label>Integración<input name="rint" [(ngModel)]="routeForm.integrationCode" required></label>
        <label>Operación<input name="rop" [(ngModel)]="routeForm.operationCode" required></label>
        <label>Prioridad<input name="rpriority" [(ngModel)]="routeForm.priority" type="number"></label>
        <label class="check"><input name="ractive" [(ngModel)]="routeForm.active" type="checkbox"> Activa</label>
        <div class="actions span-2"><button class="btn btn-primary">Guardar ruta</button>@if(routeForm.id){<button type="button" class="btn btn-danger" (click)="deleteRoute()">Eliminar</button>}</div>
      </form>
    </section>
  `
})
export class CrmComponent implements OnInit {
  integrations=signal<any[]>([]); operations=signal<any[]>([]); routes=signal<any[]>([]);
  selectedIntegrationId?:number;
  integrationForm:any=this.integrationDefaults(); operationForm:any=this.operationDefaults(); routeForm:any=this.routeDefaults();
  constructor(private readonly api:ApiService){}
  ngOnInit():void{this.reload();}
  reload():void{this.api.crmIntegrations().subscribe(x=>this.integrations.set(x));this.api.crmRoutes().subscribe(x=>this.routes.set(x));}
  selectIntegration(item:any):void{this.selectedIntegrationId=item.id;this.integrationForm={...item};this.api.crmOperations(item.id).subscribe(x=>this.operations.set(x));this.newOperation();}
  newIntegration():void{this.selectedIntegrationId=undefined;this.integrationForm=this.integrationDefaults();this.operations.set([]);}
  saveIntegration():void{const c=this.integrationForm.id?this.api.updateCrmIntegration(this.integrationForm.id,this.integrationForm):this.api.createCrmIntegration(this.integrationForm);c.subscribe((x:any)=>{this.reload();this.selectIntegration(x);});}
  deleteIntegration():void{if(this.integrationForm.id)this.api.deleteCrmIntegration(this.integrationForm.id).subscribe(()=>{this.newIntegration();this.reload();});}
  newOperation():void{this.operationForm=this.operationDefaults();}
  editOperation(x:any):void{this.operationForm={...x};}
  saveOperation():void{if(!this.selectedIntegrationId)return;const c=this.operationForm.id?this.api.updateCrmOperation(this.operationForm.id,this.operationForm):this.api.createCrmOperation(this.selectedIntegrationId,this.operationForm);c.subscribe(()=>{this.api.crmOperations(this.selectedIntegrationId!).subscribe(x=>this.operations.set(x));this.newOperation();});}
  deleteOperation():void{if(this.operationForm.id)this.api.deleteCrmOperation(this.operationForm.id).subscribe(()=>{this.api.crmOperations(this.selectedIntegrationId!).subscribe(x=>this.operations.set(x));this.newOperation();});}
  applyEspoTemplate():void {
    if (this.operationForm.bodyTemplate && !window.confirm('¿Reemplazar el texto de la plantilla actual? Deberás pulsar Guardar operación para persistirla.')) return;
    this.operationForm.bodyTemplate = JSON.stringify({
      name: '{{subject}}',
      description: '{{body}}\n\n--- Clasificación AgnosticTicketRouter ---\n{{classificationSummary}}\nSolicitud: {{requestId}}\nPerfil: {{profileCode}}'
    }, null, 2);
  }
  newRoute():void{this.routeForm=this.routeDefaults();}
  editRoute(x:any):void{this.routeForm={...x};}
  saveRoute():void{const c=this.routeForm.id?this.api.updateCrmRoute(this.routeForm.id,this.routeForm):this.api.createCrmRoute(this.routeForm);c.subscribe(()=>{this.reload();this.newRoute();});}
  deleteRoute():void{if(this.routeForm.id)this.api.deleteCrmRoute(this.routeForm.id).subscribe(()=>{this.reload();this.newRoute();});}
  private integrationDefaults(){return{code:'',name:'',baseUrl:'',authType:'NONE',usernameRef:'',secretRef:'',apiKeyName:'',tokenUrl:'',scope:'',defaultHeadersJson:'{}',active:true};}
  private operationDefaults(){return{code:'CREATE_CASE',httpMethod:'POST',pathTemplate:'/api/v1/Case',contentType:'application/json',headersTemplateJson:'{}',bodyTemplate:JSON.stringify({name:'{{subject}}',description:'{{body}}\n\n--- Clasificación AgnosticTicketRouter ---\n{{classificationSummary}}\nSolicitud: {{requestId}}\nPerfil: {{profileCode}}'},null,2),successStatusCodes:'200,201',externalIdPath:'id',timeoutMs:15000,maxAttempts:1,retryDelayMs:1000,active:true};}
  private routeDefaults(){return{profileCode:'TELCO_DEMO',dimensionCode:'',optionCode:'',integrationCode:'ESPOCRM_LOCAL',operationCode:'CREATE_CASE',priority:100,active:true};}
}
