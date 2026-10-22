import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface AiProviderView {
  id: number; name: string; baseUrl: string; endpointPath: string; model: string; language: string;
  authType: string; apiKeyHeader: string; apiKeyConfigured: boolean; enabled: boolean; defaultProvider: boolean;
}
export interface AiProviderCommand {
  name: string; baseUrl: string; endpointPath: string; model: string; language: string;
  authType: string; apiKeyHeader: string; apiKey: string; removeApiKey: boolean; enabled: boolean;
}
export interface AiProviderSettings {
  keyStorageAvailable: boolean; defaultProviderId: number | null; environmentBaseUrl: string;
  environmentModel: string; environmentLanguage: string; environmentKeyConfigured: boolean;
}
export interface AiProbeResult { success: boolean; message: string; durationMs: number; }

@Injectable({ providedIn: 'root' })
export class AiProviderService {
  private readonly url = '/api/v1/admin/ai';
  constructor(private readonly http: HttpClient) {}
  list() { return this.http.get<AiProviderView[]>(this.url + '/providers'); }
  settings() { return this.http.get<AiProviderSettings>(this.url + '/settings'); }
  create(body: AiProviderCommand) { return this.http.post<AiProviderView>(this.url + '/providers', body); }
  update(id: number, body: AiProviderCommand) { return this.http.put<AiProviderView>(this.url + '/providers/' + id, body); }
  select(id: number) { return this.http.post<void>(this.url + '/providers/' + id + '/select', {}); }
  useEnvironment() { return this.http.post<void>(this.url + '/environment/select', {}); }
  test(id: number) { return this.http.post<AiProbeResult>(this.url + '/providers/' + id + '/test', {}); }
  delete(id: number) { return this.http.delete<void>(this.url + '/providers/' + id); }
}
