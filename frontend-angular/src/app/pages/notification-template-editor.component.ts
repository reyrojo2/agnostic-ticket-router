import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationChannel, NotificationEvent, NotificationTemplate } from '../core/notification.service';

@Component({ selector: 'app-notification-template-editor', standalone: true, imports: [FormsModule], templateUrl: './notification-template-editor.component.html', styleUrls: ['./crm-workflow.css'] })
export class NotificationTemplateEditorComponent implements OnChanges {
  @Input({ required: true }) model!: NotificationTemplate;
  @Input() channels: NotificationChannel[] = []; @Input() events: NotificationEvent[] = [];
  @Input() busy = false; @Input() previewBusy = false;
  @Output() save = new EventEmitter<NotificationTemplate>(); @Output() preview = new EventEmitter<NotificationTemplate>(); @Output() cancel = new EventEmitter<void>(); @Output() changed = new EventEmitter<void>();
  form!: NotificationTemplate; recipientMode = 'client'; fixedRecipient = ''; customText = false; variable = '';
  ngOnChanges(changes: SimpleChanges) {
    if (changes['model']) {
      this.form = {...this.model}; this.recipientMode = ['{{from}}', '{{recipient}}'].includes(this.form.recipientTemplate) ? 'client' : this.form.recipientTemplate.includes('{{') ? 'advanced' : 'fixed';
      this.fixedRecipient = this.recipientMode === 'fixed' ? this.form.recipientTemplate : '';
      this.customText = this.form.subjectTemplate !== this.event()?.subject || this.form.bodyTemplate !== this.event()?.body; this.variable = '';
    }
  }
  event() { return this.events.find(e => e.code === this.form?.eventType); }
  eventChanged() { this.useRecommended(); this.updateRecipient(); }
  useRecommended() { const event = this.event(); if (event) { this.form.subjectTemplate = event.subject; this.form.bodyTemplate = event.body; this.customText = false; this.variable = ''; this.changed.emit(); } }
  updateRecipient() { if (this.recipientMode === 'client') this.form.recipientTemplate = '{{recipient}}'; else if (this.recipientMode === 'fixed') this.form.recipientTemplate = this.fixedRecipient; this.changed.emit(); }
  variables() { return (this.event()?.variables || []).filter(v => !v.key.startsWith('data.') && !['requestId', 'profileCode'].includes(v.key)); }
  insert(target: 'subjectTemplate' | 'bodyTemplate') { if (this.variable) { this.form[target] += (this.form[target] ? ' ' : '') + '{{' + this.variable + '}}'; this.changed.emit(); } }
  command() { this.updateRecipient(); return {...this.form}; }
}
