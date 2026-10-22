import { Component, Input, OnChanges, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';
import { from } from 'rxjs';
import { concatMap, toArray } from 'rxjs/operators';

/** Configuration UI: never posts a real Case from its preview. */
@Component({
  selector: 'app-crm-mapping',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section class="panel lower">
      <div class="panel-title"><div><h2>Mapeo de clasificación al CRM</h2>
        <p class="muted">1. Selecciona CREATE_CASE arriba. 2. Consulta los campos del CRM. 3. Vincula dimensiones y destinos. 4. Previsualiza y valida antes de crear casos reales.</p></div>
      </div>
      @if (error()) { <p class="alert error" role="alert">{{error()}}</p> }
      @if (info()) { <p role="status">{{info()}}</p> }
      @if (!operationId || !integrationId) {
        <p>Selecciona primero una integración y después pulsa sobre la operación que quieras configurar.</p>
      } @else {
        <div class="actions">
          <button type="button" class="btn btn-secondary" (click)="reload()">Actualizar configuración</button>
          <button type="button" class="btn btn-secondary" [disabled]="busy()" (click)="loadCatalog()">Leer campos y equipos de EspoCRM</button>
          <button type="button" class="btn btn-secondary" [disabled]="busy()" (click)="validate()">Validar compatibilidad</button>
        </div>
        @if (validation(); as v) {
          <p><strong>Compatibilidad:</strong> {{v.compatible ? 'Compatible según los metadatos' : 'Requiere ajustes'}} · Entidad: {{v.entityType}}</p>
          @for (message of v.errors; track message) { <p class="alert error">{{message}}</p> }
          @for (message of v.warnings; track message) { <p class="muted">{{message}}</p> }
        }
        <div class="two-column">
          <section>
            <h3>Dimensiones → campos</h3>
            <p class="muted">Los campos personalizados deben existir en EspoCRM. Puedes definir equivalencias: ejemplo, CAMBIO_RUC → Cambio de RUC.</p>
            <div class="table-wrap"><table><thead><tr><th>Dimensión</th><th>Campo destino</th><th>Obligatorio</th><th></th></tr></thead><tbody>
              @for (item of fields(); track item.id) {
                <tr><td>{{item.dimensionCode}}</td><td>{{item.targetField}}</td><td>{{item.required?'Sí':'No'}}</td>
                  <td><button type="button" class="link" (click)="editField(item)">Editar</button> · <button type="button" class="link" (click)="deleteField(item)">Quitar</button></td></tr>
              }
            </tbody></table></div>
            <form class="form-grid" (ngSubmit)="saveField()">
              <label>Dimensión<input name="mfDimension" [(ngModel)]="fieldForm.dimensionCode" placeholder="TIPO_GESTION" required></label>
              <label>Campo API<input name="mfTarget" [(ngModel)]="fieldForm.targetField" placeholder="cTipologia" required></label>
              @if (fieldNames().length) {
                <label class="span-2">Campos disponibles (selecciona para rellenar)<select name="mfFieldSelected" [(ngModel)]="suggestedField" (ngModelChange)="fieldForm.targetField=$event">
                  <option value="">Elegir campo...</option>
                  @for (name of fieldNames(); track name) { <option [value]="name">{{name}}</option> }
                </select></label>
              }
              <label class="span-2">Equivalencias de valores (JSON)<textarea name="mfValueMap" [(ngModel)]="fieldForm.valueMapJson" rows="3" class="mono" placeholder='{"BACKOFFICE":"Back Office"}'></textarea></label>
              <label class="check"><input name="mfRequired" [(ngModel)]="fieldForm.required" type="checkbox"> Obligatorio</label>
              <label class="check"><input name="mfActive" [(ngModel)]="fieldForm.active" type="checkbox"> Activo</label>
              <div class="actions span-2"><button class="btn btn-primary" [disabled]="busy()">Guardar campo</button><button type="button" class="btn btn-secondary" (click)="resetField()">Nuevo</button></div>
            </form>
          </section>
          <section>
            <h3>Áreas y responsables</h3>
            <p class="muted">DESTINO=BACKOFFICE no es un ID de equipo. Se debe vincular a un Team existente o crearlo explícitamente con la API.</p>
            <div class="table-wrap"><table><thead><tr><th>Condición</th><th>Asignación</th><th></th></tr></thead><tbody>
              @for (item of assignments(); track item.id) {
                <tr><td>{{item.dimensionCode}}={{item.optionCode}}</td><td>{{item.assignmentKind}}: {{item.externalName || item.externalId}}</td>
                  <td><button type="button" class="link" (click)="editAssignment(item)">Editar</button> · <button type="button" class="link" (click)="deleteAssignment(item)">Quitar</button></td></tr>
              }
            </tbody></table></div>
            <form class="form-grid" (ngSubmit)="saveAssignment()">
              <label>Dimensión<input name="maDimension" [(ngModel)]="assignmentForm.dimensionCode" required></label>
              <label>Opción<input name="maOption" [(ngModel)]="assignmentForm.optionCode" required></label>
              <label>Tipo<select name="maKind" [(ngModel)]="assignmentForm.assignmentKind"><option value="TEAM">Equipo</option><option value="USER">Responsable</option></select></label>
              <label>ID externo<input name="maId" [(ngModel)]="assignmentForm.externalId" required placeholder="ID real de EspoCRM"></label>
              @if (assignmentForm.assignmentKind === 'TEAM' && teams().length) {
                <label class="span-2">Elegir equipo existente<select name="maTeam" [(ngModel)]="selectedTeamId" (ngModelChange)="chooseRecord($event,teams())"><option value="">Elegir equipo...</option>
                  @for (t of teams(); track t.id) { <option [value]="t.id">{{t.name}} · {{t.id}}</option> }</select></label>
              }
              @if (assignmentForm.assignmentKind === 'USER' && users().length) {
                <label class="span-2">Elegir usuario existente<select name="maUser" [(ngModel)]="selectedUserId" (ngModelChange)="chooseRecord($event,users())"><option value="">Elegir responsable...</option>
                  @for (u of users(); track u.id) { <option [value]="u.id">{{u.name}} · {{u.id}}</option> }</select></label>
              }
              <label>Nombre visible<input name="maName" [(ngModel)]="assignmentForm.externalName"></label>
              <label class="check"><input name="maActive" [(ngModel)]="assignmentForm.active" type="checkbox"> Activo</label>
              <div class="actions span-2"><button class="btn btn-primary" [disabled]="busy()">Guardar asignación</button><button type="button" class="btn btn-secondary" (click)="resetAssignment()">Nueva</button></div>
            </form>
            <h3>Crear / vincular equipo mediante API</h3>
            <p class="muted">Usa la dimensión y opción del formulario anterior. Si EspoCRM ya tiene un equipo con el mismo nombre, se reutiliza.</p>
            <div class="form-grid"><label class="span-2">Nombre de equipo<input name="newTeamName" [(ngModel)]="newTeamName" placeholder="Back Office"></label></div>
            <div class="actions"><button type="button" class="btn btn-secondary" [disabled]="busy()" (click)="ensureTeam()">Crear o vincular equipo</button></div>
            <h3>Importar equipos desde Business Knowledge</h3>
            <p class="muted">Usa el perfil de la vista previa y la dimensión elegida. Se propondrá crear o reutilizar un equipo por cada opción activa (máximo 30). Confirma el listado antes de ejecutarlo.</p>
            <div class="form-grid"><label class="span-2">Dimensión de áreas<input name="bulkDimension" [(ngModel)]="bulkDimension" placeholder="DESTINO"></label></div>
            <div class="actions"><button type="button" class="btn btn-secondary" [disabled]="busy()" (click)="importTeamsFromKnowledge()">Sincronizar equipos de la dimensión</button></div>
          </section>
        </div>
      }
    </section>
    <section class="panel lower">
      <h2>Vista previa del JSON (sin crear caso)</h2>
      <p class="muted">Usa un perfil y una clasificación de ejemplo. Se resuelve la ruta y se aplican exactamente los mismos mapeos que en un envío real.</p>
      <form class="form-grid" (ngSubmit)="preview()">
        <label>Perfil<input name="previewProfile" [(ngModel)]="previewForm.profileCode" required></label>
        <label>Asunto<input name="previewSubject" [(ngModel)]="previewForm.subject"></label>
        <label class="span-2">Cuerpo<textarea name="previewBody" [(ngModel)]="previewForm.body" rows="2"></textarea></label>
        <label class="span-2">Decisiones simuladas JSON<textarea name="previewDecisions" [(ngModel)]="previewDecisions" rows="5" class="mono"></textarea></label>
        <div class="actions span-2"><button class="btn btn-primary" [disabled]="busy()">Previsualizar sin enviar</button></div>
      </form>
      @if (previewJson()) {
        <p>Ruta: {{previewRoute()}}. Campos mapeados: {{previewFields()}}.</p>
        <pre class="mono" style="white-space:pre-wrap;overflow:auto;max-height:24rem">{{previewJson()}}</pre>
      }
    </section>
    <section class="panel lower">
      <div class="panel-title"><h2>Trazabilidad de entregas CRM</h2><button type="button" class="btn btn-secondary" (click)="reloadAudits()">Actualizar</button></div>
      <p class="muted">Se registran requestId, clasificación, operación, estado HTTP e ID externo; no se almacenan contraseñas, tokens ni el texto del correo.</p>
      <div class="table-wrap"><table><thead><tr><th>Fecha</th><th>Request ID</th><th>CRM / operación</th><th>Resultado</th><th>HTTP</th><th>Case ID</th></tr></thead><tbody>
        @for (a of audits(); track a.id) {
          <tr><td>{{a.createdAt}}</td><td>{{a.requestId}}</td><td>{{a.integrationCode}} / {{a.operationCode}}</td><td>{{a.outcome}}</td><td>{{a.httpStatus ?? '-'}}</td><td>{{a.externalId || '-'}}</td></tr>
        }
      </tbody></table></div>
    </section>
  `
})
export class CrmMappingComponent implements OnChanges {
  @Input() operationId?: number;
  @Input() integrationId?: number;
  fields = signal<any[]>([]); assignments = signal<any[]>([]);
  teams = signal<any[]>([]); users = signal<any[]>([]); fieldNames = signal<string[]>([]);
  audits = signal<any[]>([]); busy = signal(false); info = signal(''); error = signal(''); validation = signal<any>(null);
  previewJson = signal(''); previewRoute = signal(''); previewFields = signal('');
  fieldForm:any = this.fieldDefaults(); assignmentForm:any = this.assignmentDefaults();
  selectedTeamId = ''; selectedUserId = ''; suggestedField = ''; newTeamName = ''; bulkDimension = 'DESTINO';
  previewDecisions = JSON.stringify({PRODUCTO:'INTERNET_DEDICADO',TIPO_GESTION:'CAMBIO_RUC',PRIORIDAD:'MEDIA',DESTINO:'BACKOFFICE'},null,2);
  previewForm:any = {profileCode:'TELCO_DEMO',subject:'Cambio de RUC de internet dedicado',body:'Solicitud ficticia para demostración'};
  constructor(private readonly api:ApiService) {}
  ngOnChanges():void { this.error.set('');this.info.set('');this.validation.set(null);this.reload();this.reloadAudits(); }
  reload():void {
    if(!this.operationId) {this.fields.set([]);this.assignments.set([]);return;}
    this.api.crmFieldMappings(this.operationId).subscribe({next:x=>this.fields.set(x),error:e=>this.fail(e)});
    this.api.crmAssignments(this.operationId).subscribe({next:x=>this.assignments.set(x),error:e=>this.fail(e)});
  }
  loadCatalog():void {
    if(!this.integrationId)return;
    this.busy.set(true);this.error.set('');
    this.api.crmCatalogFields(this.integrationId).subscribe({next:x=>{this.fieldNames.set(Object.keys(x).sort());this.busy.set(false);this.info.set('Campos CRM consultados');},error:e=>{this.busy.set(false);this.fail(e);}});
    this.api.crmCatalogTeams(this.integrationId).subscribe({next:x=>this.teams.set(x),error:e=>this.fail(e)});
    this.api.crmCatalogUsers(this.integrationId).subscribe({next:x=>this.users.set(x),error:e=>this.fail(e)});
  }
  editField(x:any):void {this.fieldForm={...x};this.suggestedField=x.targetField;}
  resetField():void {this.fieldForm=this.fieldDefaults();this.suggestedField='';}
  saveField():void {
    if(!this.operationId)return;
    const request=this.fieldForm.id ? this.api.updateCrmFieldMapping(this.operationId,this.fieldForm.id,this.fieldForm)
      :this.api.createCrmFieldMapping(this.operationId,this.fieldForm);
    request.subscribe({next:()=>{this.resetField();this.reload();this.info.set('Mapeo de campo guardado');},error:e=>this.fail(e)});
  }
  deleteField(x:any):void {if(window.confirm('¿Eliminar este mapeo de campo?'))this.api.deleteCrmFieldMapping(x.id).subscribe({next:()=>this.reload(),error:e=>this.fail(e)});}
  editAssignment(x:any):void {this.assignmentForm={...x};}
  resetAssignment():void {this.assignmentForm=this.assignmentDefaults();this.selectedTeamId='';this.selectedUserId='';}
  chooseRecord(id:string,catalog:any[]):void {
    const record=catalog.find(x=>x.id===id);
    if(record){this.assignmentForm.externalId=record.id;this.assignmentForm.externalName=record.name;}
  }
  saveAssignment():void {
    if(!this.operationId)return;
    const request=this.assignmentForm.id?this.api.updateCrmAssignment(this.operationId,this.assignmentForm.id,this.assignmentForm)
      :this.api.createCrmAssignment(this.operationId,this.assignmentForm);
    request.subscribe({next:()=>{this.resetAssignment();this.reload();this.info.set('Asignación guardada');},error:e=>this.fail(e)});
  }
  deleteAssignment(x:any):void {if(window.confirm('¿Eliminar esta asignación?'))this.api.deleteCrmAssignment(x.id).subscribe({next:()=>this.reload(),error:e=>this.fail(e)});}
  ensureTeam():void {
    if(!this.operationId)return;
    const data={dimensionCode:this.assignmentForm.dimensionCode,optionCode:this.assignmentForm.optionCode,name:this.newTeamName.trim()};
    if(!data.dimensionCode||!data.optionCode||!data.name){this.error.set('Completa dimensión, opción y nombre del equipo');return;}
    if(!window.confirm('¿Consultar EspoCRM y crear el equipo '+data.name+' si no existe? Esta acción modifica el CRM real.'))return;
    this.busy.set(true);
    this.api.crmEnsureTeam(this.operationId,data).subscribe({next:result=>{
      this.busy.set(false);this.reload();this.loadCatalog();
      this.info.set((result.created?'Equipo creado: ':'Equipo reutilizado: ')+result.name+' ('+result.id+')');
    },error:e=>{this.busy.set(false);this.fail(e);}});
  }
  importTeamsFromKnowledge():void {
    if(!this.operationId)return;
    const operationId=this.operationId;
    this.api.profiles().subscribe({next:profiles=>{
      const profile=profiles.find(x=>x.code===this.previewForm.profileCode);
      if(!profile){this.error.set('Perfil de Business Knowledge no encontrado');return;}
      this.api.dimensions(profile.id).subscribe({next:dimensions=>{
        const dim=dimensions.find(x=>x.code===this.bulkDimension && x.active);
        if(!dim){this.error.set('La dimensión indicada no existe o no está activa');return;}
        this.api.options(dim.id).subscribe({next:options=>{
          const active=options.filter(x=>x.active);
          if(active.length===0||active.length>30){this.error.set('Debe haber entre 1 y 30 opciones activas para sincronizar');return;}
          const summary=active.map(x=>x.code+' → '+x.name).join('\n');
          if(!window.confirm('Se crearán o reutilizarán estos equipos en EspoCRM y se vincularán a '+dim.code+':\n'+summary+'\n¿Continuar?'))return;
          this.busy.set(true);
          from(active).pipe(concatMap(x=>this.api.crmEnsureTeam(operationId,{
            dimensionCode:dim.code,optionCode:x.code,name:x.name
          })),toArray()).subscribe({next:result=>{
            this.busy.set(false);this.reload();this.loadCatalog();
            this.info.set('Equipos sincronizados: '+result.length+'; nuevos: '+result.filter(x=>x.created).length);
          },error:e=>{this.busy.set(false);this.fail(e);this.reload();}});
        },error:e=>this.fail(e)});
      },error:e=>this.fail(e)});
    },error:e=>this.fail(e)});
  }
  validate():void {if(!this.operationId)return;this.busy.set(true);this.api.crmValidate(this.operationId).subscribe({next:x=>{this.busy.set(false);this.validation.set(x);},error:e=>{this.busy.set(false);this.fail(e);}});}
  preview():void {
    let decisions:Record<string,string>;
    try {decisions=JSON.parse(this.previewDecisions);if(!decisions||Array.isArray(decisions)||typeof decisions!=='object')throw new Error('Formato');}
    catch {this.error.set('Las decisiones deben ser un objeto JSON');return;}
    this.busy.set(true);this.error.set('');
    this.api.crmPreview({...this.previewForm,decisions}).subscribe({next:x=>{
      this.busy.set(false);
      try{this.previewJson.set(JSON.stringify(JSON.parse(x.bodyJson),null,2));}catch{this.previewJson.set(x.bodyJson);}
      this.previewRoute.set(x.integrationCode+' / '+x.operationCode);this.previewFields.set((x.mappedFields||[]).join(', ')||'Solo plantilla base');
    },error:e=>{this.busy.set(false);this.fail(e);}});
  }
  reloadAudits():void {this.api.crmAudits().subscribe({next:x=>this.audits.set(x),error:e=>this.fail(e)});}
  private fail(e:any):void {this.error.set(e.error?.detail||'Error HTTP '+e.status+' al consultar CRM');}
  private fieldDefaults(){return{dimensionCode:'DESTINO',targetField:'',valueMapJson:'{}',required:false,active:true};}
  private assignmentDefaults(){return{dimensionCode:'DESTINO',optionCode:'BACKOFFICE',assignmentKind:'TEAM',externalId:'',externalName:'',active:true};}
}
