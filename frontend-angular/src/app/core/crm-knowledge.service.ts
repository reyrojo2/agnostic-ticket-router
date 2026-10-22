import { Injectable, signal } from '@angular/core';
import { forkJoin, map, of, switchMap } from 'rxjs';
import { ApiService } from './api.service';

/** One selected knowledge profile shared by the CRM configuration steps. */
@Injectable()
export class CrmKnowledgeService {
  readonly profiles = signal<any[]>([]);
  readonly profileCode = signal('');
  readonly dimensions = signal<any[]>([]);
  readonly options = signal<Record<string, any[]>>({});
  readonly loading = signal(false);
  readonly error = signal('');
  private generation = 0;
  constructor(private readonly api: ApiService) {}
  load(): void {
    this.error.set('');
    this.api.profiles().subscribe({
      next: profiles => {
        this.profiles.set(profiles);
        const selected = profiles.find(p => p.code === this.profileCode()) ?? profiles.find(p => p.active) ?? profiles[0];
        this.select(selected?.code ?? '');
      },
      error: e => this.error.set('No se pudieron cargar los perfiles (HTTP ' + e.status + '). Reintenta o revisa tu sesión.')
    });
  }
  select(code: string): void {
    const generation = ++this.generation;
    this.profileCode.set(code); this.dimensions.set([]); this.options.set({}); this.error.set('');
    const profile = this.profiles().find(p => p.code === code);
    if (!profile) { this.loading.set(false); return; }
    this.loading.set(true);
    this.api.dimensions(profile.id).pipe(switchMap(dimensions => {
      const requests = dimensions.map(d => this.api.options(d.id));
      return (requests.length ? forkJoin(requests) : of([])).pipe(map(options => ({ dimensions, options })));
    })).subscribe({
      next: result => {
        if (generation !== this.generation) return;
        this.dimensions.set(result.dimensions);
        this.options.set(Object.fromEntries(result.dimensions.map((d, i) => [d.code, result.options[i] ?? []])));
        this.loading.set(false);
      },
      error: e => { if (generation === this.generation) { this.loading.set(false); this.error.set('No se pudieron cargar las dimensiones y opciones (HTTP ' + e.status + ').'); } }
    });
  }
  choices(code: string): any[] { return this.options()[code] ?? []; }
  dimensionName(code: string): string { return this.dimensions().find(d => d.code === code)?.name ?? code; }
  optionName(dimension: string, code: string): string { return this.choices(dimension).find(o => o.code === code)?.name ?? code; }
}
