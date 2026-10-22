export interface DecisionDiagnostic {
  code: string; message: string; dimensionName: string; valueName: string;
  required: boolean; minimumConfidence: number | null; minimumMargin: number | null; providerStatus: number | null;
}
export interface TraceDecision {
  value: string | null; source: string; accepted: boolean; confidence: number | null;
  margin?: number | null; secondOption?: string | null; secondConfidence?: number | null;
  matchedRule?: string | null; diagnostic?: DecisionDiagnostic | null;
}
export interface TraceIssue {
  dimensionCode: string; dimensionName: string; code: string; message: string;
  required: boolean | null; providerStatus: number | null; action: 'AI' | 'KNOWLEDGE';
}
export interface RequestTrace {
  requestId: string; at: string; profile: string; source: string; state: string;
  rules: number; ai: number; rejected: number; review: boolean; externalId: string | null; externalNumber?: string | null;
  decisions: Record<string, TraceDecision>; summary: string; issues: TraceIssue[];
  outcomeMessage?: string | null; crmHttpStatus?: number | null;
}
export interface OperationalMetrics {
  timezone: string; windowDays: number; todayReceived: number; received: number; ruleOnly: number;
  aiUsed: number; ruleDimensions: number; aiDimensions: number; lowConfidence: number; aiUnavailable: number;
  review: number; crmCreated: number; crmError: number; pending: number;
  daily: { day: string; received: number; created: number; review: number; errors: number }[];
  recent: RequestTrace[];
}
export function stateLabel(state: string): string {
  return ({ RECEIVED: 'Recibida', CLASSIFIED: 'Clasificada · pendiente de CRM', REVIEW_REQUIRED: 'Necesita revisión', CRM_CREATED: 'Caso creado', CRM_ERROR: 'Error al enviar al CRM' } as Record<string, string>)[state] || state;
}
export function readableCode(value: string): string {
  const text = value.replaceAll('_', ' ').toLocaleLowerCase('es');
  return text.charAt(0).toUpperCase() + text.slice(1);
}
