import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface NotificationChannel {
  id?: number; code: string; name: string; type: 'SMTP' | 'LOG'; host: string; port: number;
  fromAddress: string; smtpAuth: boolean; startTls: boolean; sslEnabled: boolean; active: boolean;
  usernameRef: string | null; passwordRef: string | null; usernameConfigured: boolean; passwordConfigured: boolean;
}
export interface ChannelCommand extends NotificationChannel { username: string; password: string; removeCredentials: boolean; }
export interface NotificationTemplate {
  id?: number; profileCode: string; eventType: string; channelCode: string; recipientTemplate: string;
  subjectTemplate: string; bodyTemplate: string; active: boolean;
}
export interface NotificationEvent { code: string; label: string; description: string; subject: string; body: string; variables: { key: string; label: string }[]; }
export interface MessagePreview { recipient: string; subject: string; body: string; }
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly root = '/api/v1/admin/notifications';
  constructor(private http: HttpClient) {}
  channels() { return this.http.get<NotificationChannel[]>(this.root + '/channels'); }
  templates() { return this.http.get<NotificationTemplate[]>(this.root + '/templates'); }
  events() { return this.http.get<NotificationEvent[]>(this.root + '/events'); }
  settings() { return this.http.get<{credentialStorageAvailable: boolean}>(this.root + '/settings'); }
  saveChannel(command: ChannelCommand) { return command.id ? this.http.put<NotificationChannel>(this.root + '/channels/' + command.id, command) : this.http.post<NotificationChannel>(this.root + '/channels', command); }
  deleteChannel(id: number) { return this.http.delete(this.root + '/channels/' + id); }
  saveTemplate(command: NotificationTemplate) { return command.id ? this.http.put<NotificationTemplate>(this.root + '/templates/' + command.id, command) : this.http.post<NotificationTemplate>(this.root + '/templates', command); }
  deleteTemplate(id: number) { return this.http.delete(this.root + '/templates/' + id); }
  preview(command: NotificationTemplate) { return this.http.post<MessagePreview>(this.root + '/preview', command); }
}
