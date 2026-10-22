import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChannelCommand, NotificationChannel } from '../core/notification.service';
import { knowledgeCode } from '../core/knowledge-models';

@Component({ selector: 'app-notification-channel-editor', standalone: true, imports: [FormsModule], templateUrl: './notification-channel-editor.component.html', styleUrls: ['./crm-workflow.css'] })
export class NotificationChannelEditorComponent implements OnChanges {
  @Input({ required: true }) model!: ChannelCommand;
  @Input() channels: NotificationChannel[] = [];
  @Input() busy = false; @Input() credentialStorageAvailable = false;
  @Output() save = new EventEmitter<ChannelCommand>(); @Output() cancel = new EventEmitter<void>();
  form!: ChannelCommand; transport = 'STARTTLS';
  ngOnChanges(changes: SimpleChanges) { if (changes['model']) { this.form = {...this.model, username: '', password: ''}; this.transport = this.form.sslEnabled ? 'SSL' : this.form.startTls ? 'STARTTLS' : 'NONE'; } }
  nameChanged() { if (!this.form.id) this.form.code = knowledgeCode(this.form.name, this.channels.map(c => c.code), 'CANAL'); }
  setTransport() { this.form.sslEnabled = this.transport === 'SSL'; this.form.startTls = this.transport === 'STARTTLS'; if ([25, 465, 587].includes(this.form.port)) this.form.port = this.form.sslEnabled ? 465 : this.form.startTls ? 587 : 25; }
  useLocal() { this.form = {...this.form, name: this.form.name || 'Correo de pruebas local', type: 'SMTP', host: 'mailpit', port: 1025, fromAddress: 'no-reply@demo.test', smtpAuth: false, startTls: false, sslEnabled: false}; this.transport = 'NONE'; this.nameChanged(); }
  submit() { this.save.emit({...this.form}); this.form.username = ''; this.form.password = ''; }
}
