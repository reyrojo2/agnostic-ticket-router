import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { KnowledgeProfile, KnowledgeDimension, KnowledgeOption, KnowledgeRule, knowledgeCode } from '../core/knowledge-models';

@Component({
  selector: 'app-knowledge-profile-editor', standalone: true, imports: [FormsModule],
  template: `
    <form (ngSubmit)="save.emit()"><fieldset class="form-grid" [disabled]="busy">
      <label>Nombre del perfil<input name="profileName" [(ngModel)]="model.name" (ngModelChange)="nameChanged()" placeholder="Atención al cliente" required maxlength="160"></label>
      <label class="check"><input name="profileActive" [(ngModel)]="model.active" type="checkbox"> Perfil habilitado</label>
      <details class="advanced span-2"><summary>Identificador y versión</summary><div class="form-grid"><label>Identificador estable<input name="profileCode" [(ngModel)]="model.code" [readonly]="!!model.id" (input)="customCode=true" required></label><label>Versión<input name="profileVersion" [(ngModel)]="model.version" type="number" min="1" required></label></div><p class="hint muted">El identificador se genera con el nombre. Se conserva al editar para mantener los vínculos con integraciones y correos.</p></details>
      <div class="actions span-2"><button class="btn btn-primary">Guardar perfil</button><button type="button" class="btn btn-secondary" (click)="cancel.emit()">Cerrar</button></div>
    </fieldset></form>`, styleUrls: ['./crm-workflow.css']
})
export class KnowledgeProfileEditorComponent implements OnChanges {
  @Input({ required: true }) model!: KnowledgeProfile;
  @Input() codes: string[] = [];
  @Input() busy = false;
  @Output() save = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
  customCode = false;
  ngOnChanges(changes: SimpleChanges): void { if (changes['model']) this.customCode = !!this.model.code; }
  nameChanged(): void { if (!this.model.id && !this.customCode) this.model.code = knowledgeCode(this.model.name, this.codes, 'PERFIL'); }
}

@Component({
  selector: 'app-knowledge-dimension-editor', standalone: true, imports: [FormsModule], templateUrl: './knowledge-dimension-editor.component.html', styleUrls: ['./crm-workflow.css']
})
export class KnowledgeDimensionEditorComponent implements OnChanges {
  @Input({ required: true }) model!: KnowledgeDimension;
  @Input() dimensions: KnowledgeDimension[] = [];
  @Input() options: Record<string, KnowledgeOption[]> = {};
  @Input() busy = false;
  @Output() save = new EventEmitter<void>();
  customCode = false;
  validationError = '';
  ngOnChanges(changes: SimpleChanges): void { if (changes['model']) { this.customCode = !!this.model.code; this.validationError = ''; } }
  nameChanged(): void { if (!this.model.id && !this.customCode) this.model.code = knowledgeCode(this.model.name, this.dimensions.map(d => d.code)); }
  dependencies(): KnowledgeDimension[] { return this.dimensions.filter(d => d.id !== this.model.id && d.executionOrder < this.model.executionOrder); }
  parentAvailable(): boolean { return this.dependencies().some(d => d.code === this.model.dependsOnDimensionCode); }
  parentOptions(): KnowledgeOption[] { return this.options[this.model.dependsOnDimensionCode ?? ''] ?? []; }
  get confidence(): number { return Math.round(this.model.autoMinProbability * 100); }
  set confidence(value: number) { this.model.autoMinProbability = value / 100; }
  get margin(): number { return Math.round(this.model.minMargin * 100); }
  set margin(value: number) { this.model.minMargin = value / 100; }
  submit(): void {
    const parent = this.dimensions.find(d => d.code === this.model.dependsOnDimensionCode);
    if (parent && parent.executionOrder >= this.model.executionOrder) { this.validationError = 'La decisión de la que depende debe evaluarse antes. Ajusta el orden en las opciones avanzadas.'; return; }
    if (!this.model.dependsOnDimensionCode) this.model.dependsOnOptionCode = null;
    this.save.emit();
  }
}

@Component({
  selector: 'app-knowledge-option-editor', standalone: true, imports: [FormsModule], templateUrl: './knowledge-option-editor.component.html', styleUrls: ['./crm-workflow.css']
})
export class KnowledgeOptionEditorComponent implements OnChanges {
  @Input({ required: true }) model!: KnowledgeOption;
  @Input() choices: KnowledgeOption[] = [];
  @Input() dimensions: KnowledgeDimension[] = [];
  @Input() options: Record<string, KnowledgeOption[]> = {};
  @Input() dimensionOrder = 0;
  @Input() busy = false;
  @Output() save = new EventEmitter<void>();
  customCode = false;
  ngOnChanges(changes: SimpleChanges): void { if (changes['model']) this.customCode = !!this.model.code; }
  nameChanged(): void { if (!this.model.id && !this.customCode) this.model.code = knowledgeCode(this.model.name, this.choices.map(o => o.code), 'OPCION'); }
  dependencies(): KnowledgeDimension[] { return this.dimensions.filter(d => d.executionOrder < this.dimensionOrder); }
  parentAvailable(): boolean { return this.dependencies().some(d => d.code === this.model.dependsOnDimensionCode); }
  parentOptions(): KnowledgeOption[] { return this.options[this.model.dependsOnDimensionCode ?? ''] ?? []; }
  submit(): void { if (!this.model.dependsOnDimensionCode) this.model.dependsOnOptionCode = null; this.save.emit(); }
}

@Component({
  selector: 'app-knowledge-rule-editor', standalone: true, imports: [FormsModule], templateUrl: './knowledge-rule-editor.component.html', styleUrls: ['./crm-workflow.css']
})
export class KnowledgeRuleEditorComponent implements OnChanges {
  @Input({ required: true }) model!: KnowledgeRule;
  @Input() choices: KnowledgeOption[] = [];
  @Input() rules: KnowledgeRule[] = [];
  @Input() busy = false;
  @Output() save = new EventEmitter<void>();
  lines = '';
  ngOnChanges(changes: SimpleChanges): void { if (changes['model']) this.lines = this.model.ruleType === 'REGEX' ? this.model.pattern : this.model.pattern.split('|').join('\n'); }
  changeType(): void { this.lines = this.model.ruleType === 'REGEX' ? this.lines : this.lines.split('|').join('\n'); }
  updatePattern(): void { this.model.pattern = this.model.ruleType === 'REGEX' ? this.lines : this.lines.split(/\r?\n/).map(s => s.trim()).filter(Boolean).join('|'); }
  submit(): void {
    this.updatePattern();
    if (!this.model.pattern) return;
    if (!this.model.id && !this.model.code) this.model.code = knowledgeCode('REGLA_' + this.model.targetOptionCode, this.rules.map(r => r.code), 'REGLA');
    this.save.emit();
  }
}
