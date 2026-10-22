import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { AiProviderService, AiProviderCommand, AiProviderView, AiProviderSettings, AiProbeResult } from '../core/ai-provider.service';

@Component({
  standalone: true, imports: [FormsModule], templateUrl: './ai-providers.component.html', styleUrls: ['./crm-workflow.css']
})
export class AiProvidersComponent implements OnInit {
  readonly providers = signal<AiProviderView[]>([]);
  readonly settings = signal<AiProviderSettings | null>(null);
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly info = signal('');
  readonly probe = signal<AiProbeResult | null>(null);
  readonly probeName = signal('');
  selected?: AiProviderView;
  editing = false;
  form: AiProviderCommand = this.defaults();
  constructor(private readonly api: AiProviderService) {}
  ngOnInit(): void { this.load(); }
  load(): void {
    this.loading.set(true); this.error.set('');
    forkJoin({ providers: this.api.list(), settings: this.api.settings() }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: result => { this.providers.set(result.providers); this.settings.set(result.settings); }, error: e => this.fail(e)
    });
  }
  currentName(): string { return this.providers().find(p => p.defaultProvider)?.name ?? 'Configuración del servidor'; }
  newProvider(): void { this.selected = undefined; this.form = this.defaults(); this.editing = true; this.error.set(''); this.probe.set(null); }
  edit(provider: AiProviderView): void {
    this.selected = provider;
    this.form = { name: provider.name, baseUrl: provider.baseUrl, endpointPath: provider.endpointPath, model: provider.model,
      language: provider.language, authType: provider.authType, apiKeyHeader: provider.apiKeyHeader,
      apiKey: '', removeApiKey: false, enabled: provider.enabled };
    this.editing = true; this.error.set(''); this.probe.set(null);
  }
  close(): void { this.form.apiKey = ''; this.form.removeApiKey = false; this.editing = false; this.selected = undefined; }
  save(): void {
    this.busy.set(true); this.error.set(''); this.info.set('');
    const request = this.selected ? this.api.update(this.selected.id, this.form) : this.api.create(this.form);
    request.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: saved => { this.close(); this.load(); this.info.set(saved.defaultProvider ? 'Proveedor en uso actualizado. Los cambios se aplican a las próximas consultas.' : 'Proveedor guardado. Puedes probarlo antes de usarlo para las decisiones.'); },
      error: e => { this.form.apiKey = ''; this.fail(e); }
    });
  }
  select(provider: AiProviderView): void {
    this.busy.set(true); this.error.set('');
    this.api.select(provider.id).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: () => { this.load(); this.info.set(provider.name + ' se usará en las próximas consultas de IA.'); }, error: e => this.fail(e)
    });
  }
  useEnvironment(): void {
    this.busy.set(true); this.error.set('');
    this.api.useEnvironment().pipe(finalize(() => this.busy.set(false))).subscribe({ next: () => { this.load(); this.info.set('El motor utiliza la configuración del servidor.'); }, error: e => this.fail(e) });
  }
  test(provider: AiProviderView): void {
    this.busy.set(true); this.error.set(''); this.probe.set(null); this.probeName.set(provider.name);
    this.api.test(provider.id).pipe(finalize(() => this.busy.set(false))).subscribe({ next: result => this.probe.set(result), error: e => this.fail(e) });
  }
  delete(provider: AiProviderView): void {
    if (!window.confirm('¿Eliminar la conexión IA «' + provider.name + '»?')) return;
    this.busy.set(true);
    this.api.delete(provider.id).pipe(finalize(() => this.busy.set(false))).subscribe({ next: () => { if (this.selected?.id === provider.id) this.close(); this.load(); }, error: e => this.fail(e) });
  }
  private fail(e: any): void { this.error.set(e.error?.detail ?? 'No se pudo consultar o guardar el proveedor (HTTP ' + e.status + ').'); }
  private defaults(): AiProviderCommand { return { name: '', baseUrl: '', endpointPath: '/v1/systemone', model: '', language: 'es', authType: 'BEARER', apiKeyHeader: 'X-Api-Key', apiKey: '', removeApiKey: false, enabled: true }; }
}
