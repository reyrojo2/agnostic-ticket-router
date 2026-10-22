import { Component, Input } from '@angular/core';
import { PercentPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RequestTrace, TraceDecision, readableCode, stateLabel } from '../core/metrics-models';

@Component({
  selector: 'app-request-detail', standalone: true, imports: [PercentPipe, RouterLink],
  templateUrl: './request-detail.component.html', styleUrls: ['./crm-workflow.css', './dashboard.css']
})
export class RequestDetailComponent {
  @Input({ required: true }) trace!: RequestTrace;
  stateLabel = stateLabel;
  readable = readableCode;
  get decisions() { return Object.entries(this.trace.decisions).map(([code, decision]) => ({ code, ...decision })); }
  sourceLabel(source: string) { return ({ RULE: 'Regla', AI: 'Inteligencia artificial', UNAVAILABLE: 'Sin resolver' } as Record<string, string>)[source] || source; }
  valueLabel(decision: TraceDecision) {
    if (decision.source === 'UNAVAILABLE' || decision.value === 'REVIEW_REQUIRED' || decision.value === null) return 'Sin respuesta';
    if (decision.value === 'YES') return 'Sí';
    if (decision.value === 'NO') return 'No';
    return decision.diagnostic?.valueName || readableCode(decision.value);
  }
  message(decision: TraceDecision) {
    if (decision.diagnostic?.message) return decision.diagnostic.message;
    if (decision.source === 'UNAVAILABLE') return 'La IA no estuvo disponible. No se guardó la causa detallada en esta solicitud histórica.';
    if (decision.accepted) return decision.source === 'RULE' ? 'Resuelta por una regla.' : 'Respuesta IA aceptada.';
    return 'Respuesta no aceptada. No se guardaron los umbrales de esta solicitud histórica.';
  }
}
