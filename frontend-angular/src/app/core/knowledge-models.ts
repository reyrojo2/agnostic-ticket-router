export interface KnowledgeProfile { id?: number; code: string; name: string; version: number; active: boolean; }
export interface KnowledgeDimension {
  id?: number; code: string; name: string; primitiveType: string; instructions: string;
  executionOrder: number; required: boolean; autoMinProbability: number; minMargin: number;
  dependsOnDimensionCode: string | null; dependsOnOptionCode: string | null; active: boolean;
}
export interface KnowledgeOption {
  id?: number; code: string; name: string; description: string; includeContext: string | null; excludeContext: string | null;
  priority: number; dependsOnDimensionCode: string | null; dependsOnOptionCode: string | null; active: boolean;
}
export interface KnowledgeRule {
  id?: number; code: string; targetOptionCode: string; ruleType: string; pattern: string; strength: string; priority: number; active: boolean;
}
export function knowledgeCode(name: string, used: string[], prefix = 'DATO'): string {
  let base = name.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_+|_+$/g, '');
  if (!/^[A-Z]/.test(base)) base = prefix + (base ? '_' + base : '');
  base = base.slice(0, 70);
  let code = base, suffix = 2;
  while (used.includes(code)) code = base + '_' + suffix++;
  return code;
}
