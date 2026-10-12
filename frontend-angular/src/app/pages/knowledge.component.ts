import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../core/api.service';

@Component({
  standalone: true,
  imports: [FormsModule],
  template: `
    <header class="page-header">
      <div><p class="eyebrow">Motor configurable</p><h1>Business Knowledge</h1><p class="muted">El motor no conoce productos ni rubros: consume perfiles, dimensiones, opciones y reglas.</p></div>
    </header>

    <div class="two-column">
      <section class="panel">
        <div class="panel-title"><h2>Perfiles</h2><button class="btn btn-secondary" (click)="newProfile()">Nuevo</button></div>
        <div class="list">
          @for (profile of profiles; track profile.id) {
            <button class="list-row" [class.selected]="profile.id === selectedProfileId" (click)="selectProfile(profile)">
              <span><strong>{{ profile.name }}</strong><small>{{ profile.code }} · v{{ profile.version }}</small></span>
              <span class="badge" [class.off]="!profile.active">{{ profile.active ? 'Activo' : 'Inactivo' }}</span>
            </button>
          }
        </div>
        <form class="form-grid" (ngSubmit)="saveProfile()">
          <label>Código<input name="pcode" [(ngModel)]="profileForm.code" required></label>
          <label>Nombre<input name="pname" [(ngModel)]="profileForm.name" required></label>
          <label>Versión<input name="pversion" [(ngModel)]="profileForm.version" type="number"></label>
          <label class="check"><input name="pactive" [(ngModel)]="profileForm.active" type="checkbox"> Activo</label>
          <div class="actions"><button class="btn btn-primary">Guardar perfil</button>@if(profileForm.id){<button type="button" class="btn btn-danger" (click)="deleteProfile()">Eliminar</button>}</div>
        </form>
      </section>

      <section class="panel">
        <div class="panel-title"><h2>Dimensiones</h2><button class="btn btn-secondary" [disabled]="!selectedProfileId" (click)="newDimension()">Nueva</button></div>
        @if (!selectedProfileId) { <p class="empty">Selecciona un perfil.</p> }
        <div class="list compact">
          @for (dimension of dimensions; track dimension.id) {
            <button class="list-row" [class.selected]="dimension.id === selectedDimensionId" (click)="selectDimension(dimension)">
              <span><strong>{{ dimension.executionOrder }}. {{ dimension.name }}</strong><small>{{ dimension.code }} · {{ dimension.primitiveType }}</small></span>
              <span class="badge">{{ dimension.required ? 'Obligatoria' : 'Opcional' }}</span>
            </button>
          }
        </div>
        @if (selectedProfileId) {
          <form class="form-grid" (ngSubmit)="saveDimension()">
            <label>Código<input name="dcode" [(ngModel)]="dimensionForm.code" required></label>
            <label>Nombre<input name="dname" [(ngModel)]="dimensionForm.name" required></label>
            <label>Tipo<select name="dtype" [(ngModel)]="dimensionForm.primitiveType"><option>CHOICE</option><option>SCORE</option><option>NOUL</option></select></label>
            <label>Orden<input name="dorder" [(ngModel)]="dimensionForm.executionOrder" type="number"></label>
            <label class="span-2">Instrucción<input name="dinstructions" [(ngModel)]="dimensionForm.instructions" required></label>
            <label>Prob. automática<input name="dprob" [(ngModel)]="dimensionForm.autoMinProbability" type="number" step="0.01"></label>
            <label>Margen mínimo<input name="dmargin" [(ngModel)]="dimensionForm.minMargin" type="number" step="0.01"></label>
            <label>Depende de dimensión<input name="ddep" [(ngModel)]="dimensionForm.dependsOnDimensionCode"></label>
            <label>Opción padre<input name="dopt" [(ngModel)]="dimensionForm.dependsOnOptionCode"></label>
            <label class="check"><input name="drequired" [(ngModel)]="dimensionForm.required" type="checkbox"> Obligatoria</label>
            <label class="check"><input name="dactive" [(ngModel)]="dimensionForm.active" type="checkbox"> Activa</label>
            <div class="actions span-2"><button class="btn btn-primary">Guardar dimensión</button>@if(dimensionForm.id){<button type="button" class="btn btn-danger" (click)="deleteDimension()">Eliminar</button>}</div>
          </form>
        }
      </section>
    </div>

    @if (selectedDimensionId) {
      <div class="two-column lower">
        <section class="panel">
          <div class="panel-title"><h2>Opciones</h2><button class="btn btn-secondary" (click)="newOption()">Nueva</button></div>
          <div class="list compact">
            @for (option of options; track option.id) {
              <button class="list-row" [class.selected]="option.id === optionForm.id" (click)="editOption(option)"><span><strong>{{ option.name }}</strong><small>{{ option.code }}</small></span><span>{{ option.priority }}</span></button>
            }
          </div>
          <form class="form-grid" (ngSubmit)="saveOption()">
            <label>Código<input name="ocode" [(ngModel)]="optionForm.code" required></label>
            <label>Nombre<input name="oname" [(ngModel)]="optionForm.name" required></label>
            <label class="span-2">Descripción<textarea name="odesc" [(ngModel)]="optionForm.description" rows="3" required></textarea></label>
            <label class="span-2">Incluye<textarea name="oinclude" [(ngModel)]="optionForm.includeContext" rows="2"></textarea></label>
            <label class="span-2">Excluye<textarea name="oexclude" [(ngModel)]="optionForm.excludeContext" rows="2"></textarea></label>
            <label>Prioridad<input name="opriority" [(ngModel)]="optionForm.priority" type="number"></label>
            <label class="check"><input name="oactive" [(ngModel)]="optionForm.active" type="checkbox"> Activa</label>
            <div class="actions span-2"><button class="btn btn-primary">Guardar opción</button>@if(optionForm.id){<button type="button" class="btn btn-danger" (click)="deleteOption()">Eliminar</button>}</div>
          </form>
        </section>

        <section class="panel">
          <div class="panel-title"><h2>Reglas</h2><button class="btn btn-secondary" (click)="newRule()">Nueva</button></div>
          <div class="list compact">
            @for (rule of rules; track rule.id) {
              <button class="list-row" [class.selected]="rule.id === ruleForm.id" (click)="editRule(rule)"><span><strong>{{ rule.code }}</strong><small>{{ rule.strength }} → {{ rule.targetOptionCode }}</small></span><span>{{ rule.ruleType }}</span></button>
            }
          </div>
          <form class="form-grid" (ngSubmit)="saveRule()">
            <label>Código<input name="rcode" [(ngModel)]="ruleForm.code" required></label>
            <label>Opción destino<input name="rtarget" [(ngModel)]="ruleForm.targetOptionCode" required></label>
            <label>Tipo<select name="rtype" [(ngModel)]="ruleForm.ruleType"><option>CONTAINS_ANY</option><option>CONTAINS_ALL</option><option>REGEX</option></select></label>
            <label>Fuerza<select name="rstrength" [(ngModel)]="ruleForm.strength"><option>SOFT</option><option>HARD</option></select></label>
            <label class="span-2">Patrón<textarea name="rpattern" [(ngModel)]="ruleForm.pattern" rows="3" required></textarea></label>
            <label>Prioridad<input name="rpriority" [(ngModel)]="ruleForm.priority" type="number"></label>
            <label class="check"><input name="ractive" [(ngModel)]="ruleForm.active" type="checkbox"> Activa</label>
            <div class="actions span-2"><button class="btn btn-primary">Guardar regla</button>@if(ruleForm.id){<button type="button" class="btn btn-danger" (click)="deleteRule()">Eliminar</button>}</div>
          </form>
        </section>
      </div>
    }
  `
})
export class KnowledgeComponent implements OnInit {
  profiles: any[] = [];
  dimensions: any[] = [];
  options: any[] = [];
  rules: any[] = [];
  selectedProfileId?: number;
  selectedDimensionId?: number;

  profileForm: any = this.profileDefaults();
  dimensionForm: any = this.dimensionDefaults();
  optionForm: any = this.optionDefaults();
  ruleForm: any = this.ruleDefaults();

  constructor(private readonly api: ApiService) {}

  ngOnInit(): void { this.loadProfiles(); }

  loadProfiles(): void { this.api.profiles().subscribe(data => this.profiles = data); }
  selectProfile(profile: any): void {
    this.selectedProfileId = profile.id;
    this.profileForm = { ...profile };
    this.selectedDimensionId = undefined;
    this.options = [];
    this.rules = [];
    this.api.dimensions(profile.id).subscribe(data => this.dimensions = data);
  }
  newProfile(): void { this.profileForm = this.profileDefaults(); }
  saveProfile(): void {
    const call = this.profileForm.id ? this.api.updateProfile(this.profileForm.id, this.profileForm) : this.api.createProfile(this.profileForm);
    call.subscribe(() => this.loadProfiles());
  }
  deleteProfile(): void { if (this.profileForm.id) this.api.deleteProfile(this.profileForm.id).subscribe(() => { this.selectedProfileId = undefined; this.dimensions=[]; this.newProfile(); this.loadProfiles(); }); }

  selectDimension(dimension: any): void {
    this.selectedDimensionId = dimension.id;
    this.dimensionForm = { ...dimension };
    this.loadOptionsAndRules();
  }
  newDimension(): void { this.selectedDimensionId = undefined; this.dimensionForm = this.dimensionDefaults(); this.options=[]; this.rules=[]; }
  saveDimension(): void {
    if (!this.selectedProfileId) return;
    const call = this.dimensionForm.id ? this.api.updateDimension(this.dimensionForm.id, this.dimensionForm) : this.api.createDimension(this.selectedProfileId, this.dimensionForm);
    call.subscribe((saved: any) => { this.api.dimensions(this.selectedProfileId!).subscribe(data => this.dimensions=data); this.selectDimension(saved); });
  }
  deleteDimension(): void { if(this.dimensionForm.id) this.api.deleteDimension(this.dimensionForm.id).subscribe(() => { this.selectedDimensionId=undefined; this.options=[]; this.rules=[]; this.api.dimensions(this.selectedProfileId!).subscribe(data=>this.dimensions=data); this.dimensionForm=this.dimensionDefaults(); }); }

  loadOptionsAndRules(): void {
    if (!this.selectedDimensionId) return;
    this.api.options(this.selectedDimensionId).subscribe(data => this.options = data);
    this.api.rules(this.selectedDimensionId).subscribe(data => this.rules = data);
  }
  newOption(): void { this.optionForm = this.optionDefaults(); }
  editOption(option: any): void { this.optionForm = { ...option }; }
  saveOption(): void {
    if (!this.selectedDimensionId) return;
    const call = this.optionForm.id ? this.api.updateOption(this.optionForm.id, this.optionForm) : this.api.createOption(this.selectedDimensionId, this.optionForm);
    call.subscribe(() => { this.newOption(); this.loadOptionsAndRules(); });
  }
  deleteOption(): void { if(this.optionForm.id) this.api.deleteOption(this.optionForm.id).subscribe(() => { this.newOption(); this.loadOptionsAndRules(); }); }

  newRule(): void { this.ruleForm = this.ruleDefaults(); }
  editRule(rule: any): void { this.ruleForm = { ...rule }; }
  saveRule(): void {
    if (!this.selectedDimensionId) return;
    const call = this.ruleForm.id ? this.api.updateRule(this.ruleForm.id, this.ruleForm) : this.api.createRule(this.selectedDimensionId, this.ruleForm);
    call.subscribe(() => { this.newRule(); this.loadOptionsAndRules(); });
  }
  deleteRule(): void { if(this.ruleForm.id) this.api.deleteRule(this.ruleForm.id).subscribe(() => { this.newRule(); this.loadOptionsAndRules(); }); }

  private profileDefaults() { return { code:'', name:'', version:1, active:true }; }
  private dimensionDefaults() { return { code:'', name:'', primitiveType:'CHOICE', instructions:'', executionOrder:100, required:true, autoMinProbability:.70, minMargin:.20, dependsOnDimensionCode:'', dependsOnOptionCode:'', active:true }; }
  private optionDefaults() { return { code:'', name:'', description:'', includeContext:'', excludeContext:'', priority:100, dependsOnDimensionCode:'', dependsOnOptionCode:'', active:true }; }
  private ruleDefaults() { return { code:'', targetOptionCode:'', ruleType:'CONTAINS_ANY', pattern:'', strength:'SOFT', priority:100, active:true }; }
}
