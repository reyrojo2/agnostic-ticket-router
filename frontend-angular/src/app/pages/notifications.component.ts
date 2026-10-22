import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { ApiService } from '../core/api.service';
import { KnowledgeProfile } from '../core/knowledge-models';
import { ChannelCommand, MessagePreview, NotificationChannel, NotificationEvent, NotificationService, NotificationTemplate } from '../core/notification.service';
import { NotificationChannelEditorComponent } from './notification-channel-editor.component';
import { NotificationTemplateEditorComponent } from './notification-template-editor.component';

@Component({ standalone: true, imports: [FormsModule, NotificationChannelEditorComponent, NotificationTemplateEditorComponent], templateUrl: './notifications.component.html', styleUrls: ['./crm-workflow.css', './notifications.css'] })
export class NotificationsComponent implements OnInit {
  channels = signal<NotificationChannel[]>([]); templates = signal<NotificationTemplate[]>([]); events = signal<NotificationEvent[]>([]); profiles = signal<KnowledgeProfile[]>([]);
  loading = signal(false); busy = signal(false); error = signal(''); info = signal(''); previewBusy = signal(false); preview = signal<MessagePreview | null>(null);
  step = signal('messages'); profileCode = ''; credentialStorageAvailable = false;
  channelDraft: ChannelCommand | null = null; templateDraft: NotificationTemplate | null = null;
  private previewGeneration = 0;
  constructor(private api: ApiService, private notifications: NotificationService) {}
  ngOnInit() { this.load(); }
  load() {
    this.loading.set(true); this.error.set('');
    forkJoin({ channels: this.notifications.channels(), templates: this.notifications.templates(), events: this.notifications.events(), settings: this.notifications.settings(), profiles: this.api.profiles() }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: result => {
        this.channels.set(result.channels); this.templates.set(result.templates); this.events.set(result.events); this.profiles.set(result.profiles); this.credentialStorageAvailable = result.settings.credentialStorageAvailable;
        if (!this.profileCode) this.profileCode = result.profiles.find(p => result.templates.some(t => t.profileCode === p.code))?.code || result.profiles.find(p => p.active)?.code || '*';
      }, error: e => this.fail(e)
    });
  }
  channelName(code: string) { return this.channels().find(c => c.code === code)?.name || 'Canal no disponible'; }
  eventTemplate(code: string) { return this.templates().find(t => t.profileCode === this.profileCode && t.eventType === code); }
  fallback(code: string) { return this.profileCode === '*' ? undefined : this.templates().find(t => t.profileCode === '*' && t.eventType === code); }
  selectProfile() { this.templateDraft = null; this.clearPreview(); this.error.set(''); this.info.set(''); }
  editMessage(event: NotificationEvent) {
    this.error.set(''); this.info.set('');
    const own = this.eventTemplate(event.code), inherited = this.fallback(event.code);
    const source = own || inherited;
    this.templateDraft = source ? {...source, id: own?.id, profileCode: this.profileCode} : { profileCode: this.profileCode, eventType: event.code, channelCode: this.channels().find(c => c.active && c.type === 'SMTP')?.code || this.channels().find(c => c.active)?.code || '', recipientTemplate: '{{recipient}}', subjectTemplate: event.subject, bodyTemplate: event.body, active: true };
    this.loadPreview(this.templateDraft);
  }
  saveMessage(command: NotificationTemplate) {
    this.busy.set(true); this.error.set(''); this.info.set('');
    this.notifications.saveTemplate(command).pipe(finalize(() => this.busy.set(false))).subscribe({ next: saved => { this.templates.update(items => [...items.filter(t => t.id !== saved.id), saved]); this.templateDraft = {...saved}; this.info.set('Mensaje guardado. Se aplicará a los próximos eventos.'); this.loadPreview(saved); }, error: e => this.fail(e) });
  }
  clearPreview() { this.previewGeneration++; this.preview.set(null); this.previewBusy.set(false); }
  loadPreview(command: NotificationTemplate) {
    const generation = ++this.previewGeneration; this.previewBusy.set(true); this.preview.set(null);
    this.notifications.preview(command).subscribe({ next: result => { if (generation === this.previewGeneration) { this.preview.set(result); this.previewBusy.set(false); } }, error: e => { if (generation === this.previewGeneration) { this.previewBusy.set(false); this.fail(e); } } });
  }
  closeMessage() { this.templateDraft = null; this.clearPreview(); }
  deleteMessage() {
    const id = this.templateDraft?.id; if (!id || !window.confirm('¿Eliminar este mensaje? Dejará de enviarse para este perfil y momento.')) return;
    this.busy.set(true); this.notifications.deleteTemplate(id).pipe(finalize(() => this.busy.set(false))).subscribe({next: () => { this.templates.update(items => items.filter(t => t.id !== id)); this.closeMessage(); this.info.set('Mensaje eliminado.'); }, error: e => this.fail(e)});
  }
  newChannel() { this.channelDraft = { code: '', name: '', type: 'SMTP', host: '', port: 587, fromAddress: '', smtpAuth: true, startTls: true, sslEnabled: false, active: true, usernameRef: '', passwordRef: '', usernameConfigured: false, passwordConfigured: false, username: '', password: '', removeCredentials: false }; this.error.set(''); this.info.set(''); }
  editChannel(channel: NotificationChannel) { this.channelDraft = {...channel, username: '', password: '', removeCredentials: false}; this.error.set(''); this.info.set(''); }
  saveChannel(command: ChannelCommand) {
    this.busy.set(true); this.error.set(''); this.info.set('');
    this.notifications.saveChannel(command).pipe(finalize(() => this.busy.set(false))).subscribe({next: saved => { this.channels.update(items => [...items.filter(c => c.id !== saved.id), saved]); this.channelDraft = null; this.info.set('Canal guardado. Ya puedes elegirlo en los mensajes.'); }, error: e => this.fail(e)});
  }
  deleteChannel(channel: NotificationChannel) {
    if (!channel.id || !window.confirm('¿Eliminar este canal? Primero debe quedar sin mensajes asociados.')) return;
    this.busy.set(true); this.notifications.deleteChannel(channel.id).pipe(finalize(() => this.busy.set(false))).subscribe({next: () => { this.channels.update(items => items.filter(c => c.id !== channel.id)); if (this.channelDraft?.id === channel.id) this.channelDraft = null; this.info.set('Canal eliminado.'); }, error: e => this.fail(e)});
  }
  private fail(error: any) { this.error.set(typeof error?.error?.message === 'string' ? error.error.message : 'No se pudo completar la operación. Revisa los datos e inténtalo de nuevo.'); }
}
