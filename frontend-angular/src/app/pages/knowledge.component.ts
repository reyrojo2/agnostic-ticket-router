import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Observable, finalize, forkJoin, map, of, switchMap } from 'rxjs';
import { ApiService } from '../core/api.service';
import { KnowledgeProfile, KnowledgeDimension, KnowledgeOption, KnowledgeRule } from '../core/knowledge-models';
import { KnowledgeProfileEditorComponent, KnowledgeDimensionEditorComponent, KnowledgeOptionEditorComponent, KnowledgeRuleEditorComponent } from './knowledge-editors.component';

@Component({
  standalone: true, imports: [FormsModule, RouterLink, KnowledgeProfileEditorComponent, KnowledgeDimensionEditorComponent, KnowledgeOptionEditorComponent, KnowledgeRuleEditorComponent],
  templateUrl: './knowledge.component.html', styleUrls: ['./crm-workflow.css']
})
export class KnowledgeComponent implements OnInit {
  readonly profiles = signal<KnowledgeProfile[]>([]);
  readonly dimensions = signal<KnowledgeDimension[]>([]);
  readonly allOptions = signal<Record<string, KnowledgeOption[]>>({});
  readonly rules = signal<KnowledgeRule[]>([]);
  readonly busy = signal(false);
  readonly loading = signal(false);
  readonly rulesLoading = signal(false);
  readonly error = signal('');
  readonly info = signal('');
  readonly step = signal('profile');
  readonly steps = [{ key: 'profile', label: '1. Perfil' }, { key: 'decisions', label: '2. Decisiones' }, { key: 'answers', label: '3. Respuestas' }, { key: 'rules', label: '4. Reglas' }];
  selectedProfile?: KnowledgeProfile;
  selectedDimension?: KnowledgeDimension;
  profileForm = this.profileDefaults();
  dimensionForm = this.dimensionDefaults();
  optionForm = this.optionDefaults();
  ruleForm = this.ruleDefaults();
  profileEditor = false;
  dimensionEditor = false;
  optionEditor = false;
  ruleEditor = false;
  private profileGeneration = 0;
  private dimensionGeneration = 0;
  constructor(private readonly api: ApiService) {}
  ngOnInit(): void { this.loadProfiles(); }
  locked(): boolean { return this.busy() || this.loading(); }
  profileCodes(): string[] { return this.profiles().map(p => p.code); }
  choices(): KnowledgeOption[] { return this.allOptions()[this.selectedDimension?.code ?? ''] ?? []; }
  navigate(step: string): void { if (!this.locked()) { this.step.set(step); this.error.set(''); this.info.set(''); } }
  next(): void { const i = this.steps.findIndex(s => s.key === this.step()); if (i >= 0 && i < 3) this.navigate(this.steps[i + 1].key); }
  previous(): void { const i = this.steps.findIndex(s => s.key === this.step()); if (i > 0) this.navigate(this.steps[i - 1].key); }
  loadProfiles(preferredId?: number): void {
    this.loading.set(true); this.error.set('');
    this.api.profiles().subscribe({
      next: profiles => {
        this.profiles.set(profiles); this.loading.set(false);
        const selected = profiles.find(p => p.id === (preferredId ?? this.selectedProfile?.id)) ?? profiles.find(p => p.active) ?? profiles[0];
        if (selected) this.selectProfile(selected.id); else { this.selectedProfile = undefined; this.newProfile(); }
      }, error: e => { this.loading.set(false); this.fail(e); }
    });
  }
  selectProfile(id: number | undefined): void {
    const profile = this.profiles().find(p => p.id === id);
    if (!profile) return;
    this.selectedProfile = profile; this.profileForm = { ...profile }; this.profileEditor = false;
    this.selectedDimension = undefined; this.dimensionGeneration++; this.dimensions.set([]); this.allOptions.set({}); this.rules.set([]);
    this.dimensionEditor = false; this.optionEditor = false; this.ruleEditor = false; this.loadDimensions();
  }
  private loadDimensions(preferredId?: number): void {
    if (!this.selectedProfile?.id) return;
    const generation = ++this.profileGeneration;
    this.loading.set(true);
    this.api.dimensions(this.selectedProfile.id).pipe(switchMap((dimensions: KnowledgeDimension[]) => {
      const requests = dimensions.map(d => this.api.options(d.id!));
      return (requests.length ? forkJoin(requests) : of([])).pipe(map(options => ({ dimensions, options })));
    })).subscribe({
      next: result => {
        if (generation !== this.profileGeneration) return;
        this.dimensions.set(result.dimensions); this.allOptions.set(Object.fromEntries(result.dimensions.map((d, i) => [d.code, result.options[i] ?? []])));
        this.loading.set(false);
        const d = result.dimensions.find(d => d.id === (preferredId ?? this.selectedDimension?.id)) ?? result.dimensions.find(d => d.active) ?? result.dimensions[0];
        if (d) this.selectDimension(d.id); else { this.selectedDimension = undefined; this.rules.set([]); }
      }, error: e => { if (generation === this.profileGeneration) { this.loading.set(false); this.fail(e); } }
    });
  }
  selectDimension(id: number | undefined): void {
    const dimension = this.dimensions().find(d => d.id === id);
    if (!dimension) return;
    this.selectedDimension = dimension; this.dimensionForm = { ...dimension };
    this.dimensionEditor = false; this.optionEditor = false; this.ruleEditor = false; this.loadRules();
  }
  private loadRules(): void {
    const generation = ++this.dimensionGeneration;
    this.rules.set([]);
    if (!this.selectedDimension?.id) { this.rulesLoading.set(false); return; }
    this.rulesLoading.set(true);
    this.api.rules(this.selectedDimension.id).subscribe({
      next: rules => { if (generation === this.dimensionGeneration) { this.rules.set(rules); this.rulesLoading.set(false); } },
      error: e => { if (generation === this.dimensionGeneration) { this.rulesLoading.set(false); this.fail(e); } }
    });
  }
  newProfile(): void { this.profileForm = this.profileDefaults(); this.profileEditor = true; this.step.set('profile'); }
  editProfile(): void { if (this.selectedProfile) { this.profileForm = { ...this.selectedProfile }; this.profileEditor = true; } }
  saveProfile(): void {
    const request = this.profileForm.id ? this.api.updateProfile(this.profileForm.id, this.profileForm) : this.api.createProfile(this.profileForm);
    this.mutate(request, (saved: KnowledgeProfile) => { this.profileEditor = false; this.loadProfiles(saved.id); this.info.set('Perfil guardado. Continúa con las decisiones.'); });
  }
  deleteProfile(): void {
    if (!this.selectedProfile?.id || !window.confirm('¿Eliminar el perfil «' + this.selectedProfile.name + '»? Si tiene decisiones, elimina primero sus datos o deshabilítalo.')) return;
    this.mutate(this.api.deleteProfile(this.selectedProfile.id), () => { this.selectedProfile = undefined; this.selectedDimension = undefined; this.loadProfiles(); });
  }
  newDimension(): void { this.dimensionForm = this.dimensionDefaults(Math.max(0, ...this.dimensions().map(d => d.executionOrder)) + 10); this.dimensionEditor = true; }
  editDimension(dimension: KnowledgeDimension): void { this.dimensionForm = { ...dimension }; this.dimensionEditor = true; }
  saveDimension(): void {
    if (!this.selectedProfile?.id) return;
    const request = this.dimensionForm.id ? this.api.updateDimension(this.dimensionForm.id, this.dimensionForm) : this.api.createDimension(this.selectedProfile.id, this.dimensionForm);
    this.mutate(request, (saved: KnowledgeDimension) => { this.dimensionEditor = false; this.loadDimensions(saved.id); this.info.set('Decisión guardada. Configura sus respuestas en el siguiente paso.'); });
  }
  deleteDimension(dimension: KnowledgeDimension): void {
    if (!dimension.id || !window.confirm('¿Eliminar «' + dimension.name + '» y sus respuestas y reglas? Los mapeos externos que usen su código deberán revisarse.')) return;
    this.mutate(this.api.deleteDimension(dimension.id), () => { this.selectedDimension = undefined; this.dimensionEditor = false; this.loadDimensions(); });
  }
  newOption(): void { this.optionForm = this.optionDefaults(); this.optionEditor = true; }
  editOption(option: KnowledgeOption): void { this.optionForm = { ...option }; this.optionEditor = true; }
  saveOption(): void {
    if (!this.selectedDimension?.id) return;
    const dimension = this.selectedDimension;
    const request = this.optionForm.id ? this.api.updateOption(this.optionForm.id, this.optionForm) : this.api.createOption(dimension.id!, this.optionForm);
    this.mutate(request, (saved: KnowledgeOption) => {
      this.allOptions.update(all => ({ ...all, [dimension.code]: [...(all[dimension.code] ?? []).filter(o => o.id !== saved.id), saved].sort((a, b) => a.priority - b.priority) }));
      this.optionEditor = false; this.info.set('Respuesta guardada.');
    });
  }
  deleteOption(option: KnowledgeOption): void {
    if (!option.id || !window.confirm('¿Eliminar la respuesta «' + option.name + '»? Revisa las reglas y asignaciones que la utilicen.')) return;
    const code = this.selectedDimension!.code;
    this.mutate(this.api.deleteOption(option.id), () => { this.allOptions.update(all => ({ ...all, [code]: all[code].filter(o => o.id !== option.id) })); this.optionEditor = false; });
  }
  newRule(): void { this.ruleForm = this.ruleDefaults(); this.ruleEditor = true; }
  editRule(rule: KnowledgeRule): void { this.ruleForm = { ...rule }; this.ruleEditor = true; }
  saveRule(): void {
    if (!this.selectedDimension?.id) return;
    const request = this.ruleForm.id ? this.api.updateRule(this.ruleForm.id, this.ruleForm) : this.api.createRule(this.selectedDimension.id, this.ruleForm);
    this.mutate(request, () => { this.ruleEditor = false; this.loadRules(); this.info.set('Regla guardada.'); });
  }
  deleteRule(rule: KnowledgeRule): void {
    if (!rule.id || !window.confirm('¿Eliminar esta regla?')) return;
    this.mutate(this.api.deleteRule(rule.id), () => { this.ruleEditor = false; this.loadRules(); });
  }
  optionName(code: string): string { return this.choices().find(o => o.code === code)?.name ?? code; }
  ruleDescription(rule: KnowledgeRule): string { return ({ CONTAINS_ANY: 'Contiene alguna expresión', CONTAINS_ALL: 'Contiene todas las expresiones', REGEX: 'Coincide con un patrón avanzado' } as Record<string, string>)[rule.ruleType] ?? rule.ruleType; }
  typeName(type: string): string { return ({ CHOICE: 'Elegir una respuesta', SCORE: 'Puntuación', NOUL: 'Sí o no' } as Record<string, string>)[type] ?? type; }
  private mutate<T>(request: Observable<T>, next: (saved: T) => void): void {
    this.busy.set(true); this.error.set(''); this.info.set('');
    request.pipe(finalize(() => this.busy.set(false))).subscribe({ next, error: e => this.fail(e) });
  }
  private fail(e: any): void { this.error.set(e.error?.detail ?? 'No se pudo consultar o guardar la configuración (HTTP ' + e.status + ').'); }
  private profileDefaults(): KnowledgeProfile { return { code: '', name: '', version: 1, active: true }; }
  private dimensionDefaults(order = 100): KnowledgeDimension { return { code: '', name: '', primitiveType: 'CHOICE', instructions: '', executionOrder: order, required: true, autoMinProbability: .70, minMargin: .20, dependsOnDimensionCode: null, dependsOnOptionCode: null, active: true }; }
  private optionDefaults(): KnowledgeOption { return { code: '', name: '', description: '', includeContext: '', excludeContext: '', priority: 100, dependsOnDimensionCode: null, dependsOnOptionCode: null, active: true }; }
  private ruleDefaults(): KnowledgeRule { return { code: '', targetOptionCode: '', ruleType: 'CONTAINS_ANY', pattern: '', strength: 'SOFT', priority: 100, active: true }; }
}
