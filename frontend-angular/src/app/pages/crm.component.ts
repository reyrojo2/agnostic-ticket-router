import { Component, OnInit, ViewChild, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { ApiService } from '../core/api.service';
import { CrmKnowledgeService } from '../core/crm-knowledge.service';
import { CrmMappingComponent } from './crm-mapping.component';

@Component({
  standalone: true, imports: [FormsModule, CrmMappingComponent], providers: [CrmKnowledgeService],
  templateUrl: './crm.component.html', styleUrls: ['./crm-workflow.css']
})
export class CrmComponent implements OnInit {
  @ViewChild(CrmMappingComponent) mapping?: CrmMappingComponent;
  readonly integrations = signal<any[]>([]);
  readonly operations = signal<any[]>([]);
  readonly routes = signal<any[]>([]);
  readonly routeOperations = signal<any[]>([]);
  readonly step = signal('connection');
  readonly busy = signal(false);
  readonly operationsLoading = signal(false);
  readonly routeLoading = signal(false);
  readonly error = signal('');
  readonly info = signal('');
  readonly steps = [{ key: 'connection', label: '1. Conexión' }, { key: 'fields', label: '2. Campos' },
    { key: 'teams', label: '3. Equipos' }, { key: 'preview', label: '4. Comprobar' }];
  selectedIntegrationId?: number;
  selectedOperation: any;
  integrationForm: any = this.integrationDefaults();
  operationForm: any = this.operationDefaults();
  routeForm: any = this.routeDefaults();
  connectionEditor = false;
  operationEditor = false;
  private selection = 0;
  private routeSelection = 0;

  constructor(private readonly api: ApiService, public readonly knowledge: CrmKnowledgeService) {}
  ngOnInit(): void { this.knowledge.load(); this.reload(); }
  locked(): boolean { return this.busy() || !!this.mapping?.busy(); }
  isEspo(): boolean { return (this.integrationForm.code ?? '').toUpperCase().startsWith('ESPOCRM'); }
  navigate(step: string): void { if (!this.locked()) { this.step.set(step); this.info.set(''); this.error.set(''); } }
  next(): void { const i = this.steps.findIndex(s => s.key === this.step()); if (i >= 0 && i < this.steps.length - 1) this.navigate(this.steps[i + 1].key); }
  previous(): void { const i = this.steps.findIndex(s => s.key === this.step()); if (i > 0) this.navigate(this.steps[i - 1].key); }
  canDiscard(): boolean { return !this.mapping?.hasDrafts() || window.confirm('Hay vínculos sin guardar. ¿Descartar los cambios y cambiar la selección?'); }
  selectProfile(code: string): void {
    if (!this.canDiscard()) return;
    this.knowledge.select(code); this.newRoute();
  }
  reload(): void {
    this.error.set('');
    forkJoin({ integrations: this.api.crmIntegrations(), routes: this.api.crmRoutes() }).subscribe({
      next: result => {
        this.integrations.set(result.integrations); this.routes.set(result.routes);
        if (!this.selectedIntegrationId) {
          const integration = result.integrations.find(i => i.active) ?? result.integrations[0];
          if (integration) this.selectIntegration(integration.id, true); else this.newIntegration();
        }
      }, error: e => this.fail(e)
    });
  }
  selectIntegration(id: number, initial = false): void {
    if (!initial && !this.canDiscard()) return;
    const item = this.integrations().find(i => i.id === id);
    if (!item) return;
    const selection = ++this.selection;
    this.selectedIntegrationId = id; this.integrationForm = { ...item };
    this.selectedOperation = undefined; this.operationForm = this.operationDefaults(); this.operations.set([]);
    this.connectionEditor = false; this.operationEditor = false; this.operationsLoading.set(true);
    this.api.crmOperations(id).subscribe({
      next: operations => {
        if (selection !== this.selection) return;
        this.operations.set(operations); this.operationsLoading.set(false);
        const operation = operations.find(o => o.active && o.code === 'CREATE_CASE') ?? operations.find(o => o.active) ?? operations[0];
        if (operation) this.editOperation(operation.id, true);
      }, error: e => { if (selection === this.selection) { this.operationsLoading.set(false); this.fail(e); } }
    });
  }
  newIntegration(): void {
    if (!this.canDiscard()) return;
    this.selection++; this.selectedIntegrationId = undefined; this.selectedOperation = undefined;
    this.integrationForm = this.integrationDefaults(); this.operationForm = this.operationDefaults(); this.operations.set([]);
    this.connectionEditor = true; this.operationEditor = false;
  }
  saveIntegration(): void {
    this.busy.set(true); this.error.set(''); this.info.set('');
    const request = this.integrationForm.id ? this.api.updateCrmIntegration(this.integrationForm.id, this.integrationForm) : this.api.createCrmIntegration(this.integrationForm);
    request.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (saved: any) => {
        this.integrations.update(items => [...items.filter(i => i.id !== saved.id), saved]);
        this.selectIntegration(saved.id, true); this.info.set('Conexión guardada.');
      }, error: e => this.fail(e)
    });
  }
  prepareEspo(): void {
    if (!this.selectedIntegrationId) return;
    this.operationForm = this.operationDefaults(); this.saveOperation();
  }
  editOperation(id: number, initial = false): void {
    if (!initial && !this.canDiscard()) return;
    const operation = this.operations().find(o => o.id === id);
    if (operation) { this.selectedOperation = { ...operation }; this.operationForm = { ...operation }; this.operationEditor = false; }
  }
  newOperation(): void { if (this.canDiscard()) { this.selectedOperation = undefined; this.operationForm = this.operationDefaults(); this.operationEditor = true; } }
  saveOperation(): void {
    if (!this.selectedIntegrationId) return;
    this.busy.set(true); this.error.set('');
    const request = this.operationForm.id ? this.api.updateCrmOperation(this.operationForm.id, this.operationForm) : this.api.createCrmOperation(this.selectedIntegrationId, this.operationForm);
    request.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (saved: any) => {
        this.operations.update(items => [...items.filter(o => o.id !== saved.id), saved]); this.editOperation(saved.id, true);
        this.info.set('Acción guardada. Puedes continuar con los campos y equipos.');
      }, error: e => this.fail(e)
    });
  }
  deleteIntegration(): void {
    if (!this.integrationForm.id || !window.confirm('¿Eliminar esta conexión y sus operaciones?')) return;
    this.busy.set(true);
    this.api.deleteCrmIntegration(this.integrationForm.id).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: () => { this.selectedIntegrationId = undefined; this.selectedOperation = undefined; this.reload(); }, error: e => this.fail(e)
    });
  }
  deleteOperation(): void {
    if (!this.operationForm.id || !window.confirm('¿Eliminar esta acción y sus vínculos?')) return;
    this.busy.set(true);
    this.api.deleteCrmOperation(this.operationForm.id).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: () => this.selectIntegration(this.selectedIntegrationId!, true), error: e => this.fail(e)
    });
  }
  operationName(operation: any): string {
    return operation.code === 'CREATE_CASE' ? 'Crear caso' : operation.code === 'CREATE_TICKET' ? 'Crear ticket' : operation.code;
  }
  integrationName(code: string): string { return this.integrations().find(i => i.code === code)?.name ?? code; }
  profileRoutes(): any[] { return this.routes().filter(r => r.profileCode === this.knowledge.profileCode()); }
  openRoutes(): void { if (this.routeForm.profileCode !== this.knowledge.profileCode() || !this.routeForm.integrationCode) this.newRoute(); this.navigate('routes'); }
  changeRouteDimension(code: string): void {
    this.routeForm.dimensionCode = code; this.routeForm.optionCode = '';
    if (!this.routeForm.id) this.routeForm.priority = code ? 10 : 100;
  }
  newRoute(): void {
    this.routeForm = { ...this.routeDefaults(), profileCode: this.knowledge.profileCode(), integrationCode: this.integrationForm.code ?? '', operationCode: this.selectedOperation?.code ?? '' };
    if (this.routeForm.integrationCode) this.loadRouteOperations(false);
  }
  editRoute(route: any): void { this.routeForm = { ...route }; this.loadRouteOperations(false); }
  loadRouteOperations(reset = true): void {
    if (reset) this.routeForm.operationCode = '';
    const integration = this.integrations().find(i => i.code === this.routeForm.integrationCode);
    const selection = ++this.routeSelection;
    this.routeOperations.set([]); this.routeLoading.set(false);
    if (!integration) return;
    this.routeLoading.set(true);
    this.api.crmOperations(integration.id).subscribe({
      next: operations => { if (selection === this.routeSelection) { this.routeOperations.set(operations); this.routeLoading.set(false); if (reset) this.routeForm.operationCode = operations.find(o => o.active)?.code ?? ''; } },
      error: e => { if (selection === this.routeSelection) { this.routeLoading.set(false); this.fail(e); } }
    });
  }
  useForProfile(): void {
    if (!this.selectedOperation || !this.knowledge.profileCode()) return;
    const existing = this.profileRoutes().find(r => !r.dimensionCode);
    this.routeForm = { ...(existing ?? this.routeDefaults()), profileCode: this.knowledge.profileCode(),
      dimensionCode: '', optionCode: '', integrationCode: this.integrationForm.code, operationCode: this.selectedOperation.code, active: true };
    this.saveRoute();
  }
  saveRoute(): void {
    if (!this.knowledge.profileCode()) return;
    const body = { ...this.routeForm, profileCode: this.knowledge.profileCode() };
    if (!body.dimensionCode) body.optionCode = '';
    this.busy.set(true); this.error.set('');
    const request = body.id ? this.api.updateCrmRoute(body.id, body) : this.api.createCrmRoute(body);
    request.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: () => { this.api.crmRoutes().subscribe({ next: routes => this.routes.set(routes), error: e => this.fail(e) }); this.newRoute(); this.info.set('Destino del perfil guardado.'); }, error: e => this.fail(e)
    });
  }
  deleteRoute(): void {
    if (!this.routeForm.id || !window.confirm('¿Quitar esta ruta de envío?')) return;
    this.busy.set(true);
    this.api.deleteCrmRoute(this.routeForm.id).pipe(finalize(() => this.busy.set(false))).subscribe({ next: () => { this.newRoute(); this.reload(); }, error: e => this.fail(e) });
  }
  private fail(e: any): void { this.error.set(e.error?.detail ?? 'No se pudo guardar o consultar la configuración (HTTP ' + e.status + ').'); }
  private integrationDefaults(): any { return { code: 'ESPOCRM_' + Date.now().toString(36).toUpperCase(), name: 'EspoCRM', baseUrl: '', authType: 'API_KEY_HEADER', apiKeyName: 'X-Api-Key', usernameRef: '', secretRef: 'ENV:ESPOCRM_API_KEY', tokenUrl: '', scope: '', defaultHeadersJson: '{}', active: true }; }
  private operationDefaults(): any { return { code: 'CREATE_CASE', httpMethod: 'POST', pathTemplate: '/api/v1/Case', contentType: 'application/json', headersTemplateJson: '{}', bodyTemplate: JSON.stringify({ name: '{{subject}}', description: '{{body}}\n\nClasificación:\n{{classificationSummary}}' }, null, 2), successStatusCodes: '200,201', externalIdPath: 'id', externalNumberPath: 'number', timeoutMs: 15000, maxAttempts: 1, retryDelayMs: 1000, active: true }; }
  private routeDefaults(): any { return { profileCode: '', dimensionCode: '', optionCode: '', integrationCode: '', operationCode: '', priority: 100, active: true }; }
}
