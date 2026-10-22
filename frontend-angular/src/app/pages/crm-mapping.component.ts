import { Component, Input, OnChanges, OnDestroy, SimpleChanges, effect, untracked, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { catchError, concatMap, finalize, forkJoin, from, of, Subscription, tap, toArray, Observable } from 'rxjs';
import { ApiService } from '../core/api.service';
import { CrmKnowledgeService } from '../core/crm-knowledge.service';

@Component({
  selector: 'app-crm-mapping', standalone: true, imports: [FormsModule, DatePipe],
  templateUrl: './crm-mapping.component.html', styleUrls: ['./crm-workflow.css']
})
export class CrmMappingComponent implements OnChanges, OnDestroy {
  @Input() operationId?: number;
  @Input() integrationId?: number;
  @Input() operationCode = '';
  @Input() operationPath = '';
  @Input() section = 'fields';
  @Input() isEspo = true;
  readonly fields = signal<any[]>([]);
  readonly assignments = signal<any[]>([]);
  readonly catalog = signal<Record<string, any>>({});
  readonly teams = signal<any[]>([]);
  readonly users = signal<any[]>([]);
  readonly audits = signal<any[]>([]);
  readonly busy = signal(false);
  readonly loading = signal(false);
  readonly catalogLoading = signal(false);
  readonly catalogLoaded = signal(false);
  readonly catalogError = signal('');
  readonly teamCatalogError = signal('');
  readonly userCatalogError = signal('');
  readonly info = signal('');
  readonly error = signal('');
  readonly validation = signal<any>(null);
  readonly previewResult = signal<any>(null);
  fieldForm: any = this.fieldDefaults();
  equivalences: Record<string, any> = {};
  assignmentDimension = '';
  assignmentDrafts: Record<string, Record<string, string>> = {};
  previewSelections: Record<string, string> = {};
  previewSubject = 'Solicitud de ejemplo';
  previewBody = 'Prueba de configuración';
  newTeamOption = '';
  newTeamName = '';
  private context = 0;
  private readSubscription?: Subscription;
  private catalogSubscription?: Subscription;

  constructor(private readonly api: ApiService, public readonly knowledge: CrmKnowledgeService) {
    effect(() => {
      const profile = this.knowledge.profileCode();
      const dimensions = this.knowledge.dimensions();
      const options = this.knowledge.options();
      untracked(() => {
        this.resetField(); this.assignmentDrafts = {}; this.newTeamOption = ''; this.newTeamName = '';
        this.assignmentDimension = dimensions.find(d => d.code === 'DESTINO')?.code ?? dimensions[0]?.code ?? '';
        this.previewSelections = Object.fromEntries(dimensions.map(d => [d.code, options[d.code]?.find(o => o.active)?.code ?? '']));
        this.previewResult.set(null); this.validation.set(null);
        if (profile) this.info.set('');
      });
    });
  }
  ngOnChanges(changes: SimpleChanges): void {
    if (changes['operationId'] || changes['integrationId'] || changes['operationPath']) {
      this.context++;
      this.error.set(''); this.info.set(''); this.validation.set(null); this.previewResult.set(null);
      this.fields.set([]); this.assignments.set([]); this.assignmentDrafts = {}; this.resetField();
      this.reload(); this.loadCatalog();
    }
    if (changes['section']) { this.info.set(''); this.error.set(''); if (this.section === 'history') this.reloadAudits(); }
  }
  ngOnDestroy(): void { this.context++; this.readSubscription?.unsubscribe(); this.catalogSubscription?.unsubscribe(); }
  hasDrafts(): boolean { return Object.keys(this.assignmentDrafts).length > 0 || !!this.fieldForm.dimensionCode; }
  reload(): void {
    this.readSubscription?.unsubscribe(); this.validation.set(null);
    if (!this.operationId) return;
    const context = this.context;
    this.loading.set(true);
    this.readSubscription = forkJoin({ fields: this.api.crmFieldMappings(this.operationId), assignments: this.api.crmAssignments(this.operationId) })
      .pipe(finalize(() => this.loading.set(false))).subscribe({
        next: result => { if (context === this.context) { this.fields.set(result.fields); this.assignments.set(result.assignments); } },
        error: e => { if (context === this.context) this.fail(e); }
      });
  }
  loadCatalog(): void {
    this.catalogSubscription?.unsubscribe();
    this.catalog.set({}); this.teams.set([]); this.users.set([]); this.catalogLoaded.set(false);
    this.catalogError.set(''); this.teamCatalogError.set(''); this.userCatalogError.set('');
    if (!this.integrationId || !this.operationId || !this.isEspo) return;
    const entity = /^\/api\/v1\/([A-Za-z][A-Za-z0-9_]*)$/.exec(this.operationPath)?.[1];
    if (!entity) { this.catalogError.set('Esta acción no permite consultar un catálogo de campos. Usa la configuración avanzada.'); return; }
    const context = this.context;
    const failures: Record<string, string> = {};
    this.catalogLoading.set(true);
    this.catalogSubscription = forkJoin({
      fields: this.api.crmCatalogFields(this.integrationId, entity).pipe(catchError(e => { failures['fields'] = this.message(e); return of({}); })),
      teams: this.api.crmCatalogTeams(this.integrationId).pipe(catchError(e => { failures['teams'] = this.message(e); return of([]); })),
      users: this.api.crmCatalogUsers(this.integrationId).pipe(catchError(e => { failures['users'] = this.message(e); return of([]); }))
    }).pipe(finalize(() => this.catalogLoading.set(false))).subscribe(result => {
      if (context !== this.context) return;
      this.catalog.set(result.fields); this.teams.set(result.teams); this.users.set(result.users);
      this.catalogLoaded.set(true); this.catalogError.set(failures['fields'] ?? '');
      this.teamCatalogError.set(failures['teams'] ?? ''); this.userCatalogError.set(failures['users'] ?? '');
    });
  }
  availableFields(): string[] {
    const reserved = ['id', 'createdAt', 'modifiedAt', 'deleted', 'createdById', 'teamsIds', 'assignedUserId'];
    return Object.keys(this.catalog()).filter(k => !reserved.includes(k) && !this.catalog()[k].readOnly && !this.catalog()[k].notStorable).sort();
  }
  fieldName(code: string): string {
    const names: Record<string, string> = { name: 'Título', description: 'Descripción', priority: 'Prioridad', type: 'Tipo', status: 'Estado' };
    return names[code] ?? code.replace(/^c(?=[A-Z])/, '').replace(/([a-z])([A-Z])/g, '$1 $2').replace(/^./, c => c.toUpperCase());
  }
  targetOptions(): string[] { return this.catalog()[this.fieldForm.targetField]?.options ?? []; }
  hasDimension(code: string): boolean { return this.knowledge.dimensions().some(d => d.code === code); }
  resetField(): void { this.fieldForm = this.fieldDefaults(); this.equivalences = {}; }
  editField(field: any): void { this.fieldForm = { ...field }; this.readEquivalences(); this.error.set(''); this.info.set(''); }
  readEquivalences(): boolean {
    try {
      const value = JSON.parse(this.fieldForm.valueMapJson || '{}');
      if (!value || Array.isArray(value) || typeof value !== 'object') throw new Error();
      this.equivalences = value; return true;
    } catch { this.error.set('Las equivalencias avanzadas deben contener un objeto JSON válido.'); return false; }
  }
  setEquivalence(code: string, value: any): void {
    if (value === null || value === undefined) delete this.equivalences[code]; else this.equivalences[code] = value;
    this.fieldForm.valueMapJson = JSON.stringify(this.equivalences, null, 2);
  }
  hasEquivalence(code: string): boolean { return Object.prototype.hasOwnProperty.call(this.equivalences, code); }
  equivalence(code: string): any { return this.hasEquivalence(code) ? this.equivalences[code] : null; }
  originalAllowed(code: string): boolean { return !this.targetOptions().length || this.targetOptions().includes(code); }
  fieldProblems(field: any): string[] {
    if (!field.active) return [];
    const allowed: string[] = this.catalog()[field.targetField]?.options ?? [];
    if (!allowed.length) return [];
    let mapped: Record<string, string>;
    try { mapped = JSON.parse(field.valueMapJson || '{}'); }
    catch { return [`${this.fieldName(field.targetField)}: equivalencias inválidas.`]; }
    return this.knowledge.choices(field.dimensionCode).filter(option => option.active).flatMap(option => {
      const value = mapped[option.code] ?? option.code;
      return allowed.includes(value) ? [] : [`${this.fieldName(field.targetField)}: «${option.name}» debe tener un equivalente válido en EspoCRM.`];
    });
  }
  saveField(): void {
    if (!this.operationId || !this.readEquivalences()) return;
    if (!this.fieldForm.dimensionCode || !this.fieldForm.targetField) { this.error.set('Elige una dimensión y un campo de destino.'); return; }
    const problems = this.fieldProblems(this.fieldForm);
    if (problems.length) { this.error.set(problems.join(' ')); return; }
    const operationId = this.operationId, context = this.context;
    const request = this.fieldForm.id ? this.api.updateCrmFieldMapping(operationId, this.fieldForm.id, this.fieldForm) : this.api.createCrmFieldMapping(operationId, this.fieldForm);
    this.busy.set(true); this.error.set('');
    request.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: () => { if (context === this.context) { this.resetField(); this.reload(); this.info.set('Campo vinculado.'); } },
      error: e => { if (context === this.context) this.fail(e); }
    });
  }
  deleteField(field: any): void {
    if (!window.confirm('¿Quitar el vínculo con ' + this.fieldName(field.targetField) + '?')) return;
    this.busy.set(true);
    this.api.deleteCrmFieldMapping(field.id).pipe(finalize(() => this.busy.set(false))).subscribe({ next: () => this.reload(), error: e => this.fail(e) });
  }
  existingAssignment(option: string, kind: string): any {
    return this.assignments().find(a => a.dimensionCode === this.assignmentDimension && a.optionCode === option && a.assignmentKind === kind);
  }
  assignmentValue(option: string, kind: string): string {
    const draft = this.assignmentDrafts[option];
    return draft && Object.prototype.hasOwnProperty.call(draft, kind) ? draft[kind] : this.existingAssignment(option, kind)?.externalId ?? '';
  }
  setAssignment(option: string, kind: string, value: string): void {
    this.assignmentDrafts[option] = { ...this.assignmentDrafts[option], [kind]: value }; this.validation.set(null); this.info.set('');
  }
  changeAssignmentDimension(code: string): void {
    if (Object.keys(this.assignmentDrafts).length && !window.confirm('¿Descartar los cambios de equipos sin guardar?')) return;
    this.assignmentDimension = code; this.assignmentDrafts = {}; this.newTeamOption = ''; this.newTeamName = '';
  }
  recordName(id: string, kind: string): string {
    return (kind === 'TEAM' ? this.teams() : this.users()).find(r => r.id === id)?.name
      ?? this.assignments().find(a => a.externalId === id && a.assignmentKind === kind)?.externalName ?? 'Asignación guardada';
  }
  inCatalog(id: string, kind: string): boolean { return (kind === 'TEAM' ? this.teams() : this.users()).some(r => r.id === id); }
  saveAssignments(): void {
    if (!this.operationId) return;
    const operationId = this.operationId, context = this.context;
    const updates: Array<() => Observable<any>> = [];
    let removes = 0;
    for (const [option, kinds] of Object.entries(this.assignmentDrafts)) {
      for (const [kind, id] of Object.entries(kinds)) {
        const existing = this.existingAssignment(option, kind);
        if (!id) { if (existing) { updates.push(() => this.api.deleteCrmAssignment(existing.id)); removes++; } continue; }
        if (existing?.externalId === id && existing.active) continue;
        const body = { dimensionCode: this.assignmentDimension, optionCode: option, assignmentKind: kind, externalId: id, externalName: this.recordName(id, kind), active: true };
        updates.push(() => existing ? this.api.updateCrmAssignment(operationId, existing.id, body) : this.api.createCrmAssignment(operationId, body));
      }
    }
    if (!updates.length) { this.assignmentDrafts = {}; this.info.set('No hay cambios pendientes.'); return; }
    if (removes && !window.confirm('Se quitarán ' + removes + ' asignaciones guardadas. ¿Continuar?')) return;
    let completed = 0;
    this.busy.set(true); this.error.set(''); this.info.set('');
    from(updates).pipe(concatMap(save => save().pipe(tap(() => completed++))), toArray(), finalize(() => this.busy.set(false))).subscribe({
      next: () => { if (context === this.context) { this.assignmentDrafts = {}; this.reload(); this.info.set('Equipos y responsables guardados.'); } },
      error: e => { if (context === this.context) { this.fail(e); this.info.set(completed + ' cambios confirmados. Revisa y vuelve a guardar los pendientes.'); this.reload(); } }
    });
  }
  ensureTeam(): void {
    if (!this.operationId || !this.newTeamOption || !this.newTeamName.trim()) return;
    if (!window.confirm('¿Crear o reutilizar el equipo «' + this.newTeamName.trim() + '» en EspoCRM y vincularlo?')) return;
    const context = this.context;
    this.busy.set(true); this.error.set('');
    this.api.crmEnsureTeam(this.operationId, { dimensionCode: this.assignmentDimension, optionCode: this.newTeamOption, name: this.newTeamName.trim() })
      .pipe(finalize(() => this.busy.set(false))).subscribe({
        next: r => { if (context === this.context) { this.assignmentDrafts = {}; this.reload(); this.loadCatalog(); this.info.set('Equipo vinculado: ' + r.name); } },
        error: e => { if (context === this.context) this.fail(e); }
      });
  }
  validate(): void {
    if (!this.operationId) return;
    const context = this.context;
    this.busy.set(true); this.error.set(''); this.validation.set(null);
    this.api.crmValidate(this.operationId).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: r => {
        if (context !== this.context) return;
        const errors = [...(r.errors ?? []), ...this.fields().flatMap(field => this.fieldProblems(field))];
        this.validation.set({ ...r, compatible: r.compatible && errors.length === 0, errors });
      },
      error: e => { if (context === this.context) this.fail(e); }
    });
  }
  preview(): void {
    const decisions = Object.fromEntries(Object.entries(this.previewSelections)
      .filter(([, value]) => value !== '' && value !== null && value !== undefined)
      .map(([key, value]) => [key, String(value)]));
    const context = this.context;
    this.busy.set(true); this.error.set(''); this.previewResult.set(null);
    this.api.crmPreview({ profileCode: this.knowledge.profileCode(), subject: this.previewSubject, body: this.previewBody, decisions })
      .pipe(finalize(() => this.busy.set(false))).subscribe({
        next: r => {
          if (context !== this.context) return;
          let body: any; try { body = JSON.parse(r.bodyJson); } catch { body = {}; }
          this.previewResult.set({ ...r, body, formatted: JSON.stringify(body, null, 2) });
        }, error: e => { if (context === this.context) this.fail(e); }
      });
  }
  previewTeams(): string { return (this.previewResult()?.body?.teamsIds ?? []).map((id: string) => this.recordName(id, 'TEAM')).join(', '); }
  previewUser(): string { const id = this.previewResult()?.body?.assignedUserId; return id ? this.recordName(id, 'USER') : ''; }
  previewFields(): Array<{ name: string; value: string }> {
    return (this.previewResult()?.mappedFields ?? []).filter((f: string) => !['teamsIds', 'assignedUserId'].includes(f)).map((f: string) => ({ name: this.fieldName(f), value: String(this.previewResult()?.body?.[f] ?? '') }));
  }
  reloadAudits(): void { this.api.crmAudits().subscribe({ next: r => this.audits.set(r), error: e => this.fail(e) }); }
  outcome(code: string): string { return ({ CRM_CREATED: 'Caso creado', CRM_ERROR: 'Error de envío', REVIEW_REQUIRED: 'Requiere revisión' } as Record<string, string>)[code] ?? code; }
  private fieldDefaults(): any { return { dimensionCode: '', targetField: '', valueMapJson: '{}', required: false, active: true }; }
  private message(e: any): string { return e.error?.detail ?? 'No se pudo completar la consulta (HTTP ' + e.status + ').'; }
  private fail(e: any): void { this.error.set(this.message(e)); }
}
