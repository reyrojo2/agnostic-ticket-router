import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API } from './api.config';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private readonly http: HttpClient) {}

  profiles() { return this.http.get<any[]>(`${API.knowledge}/api/v1/admin/profiles`); }
  createProfile(body: any) { return this.http.post(`${API.knowledge}/api/v1/admin/profiles`, body); }
  updateProfile(id: number, body: any) { return this.http.put(`${API.knowledge}/api/v1/admin/profiles/${id}`, body); }
  deleteProfile(id: number) { return this.http.delete(`${API.knowledge}/api/v1/admin/profiles/${id}`); }

  dimensions(profileId: number) { return this.http.get<any[]>(`${API.knowledge}/api/v1/admin/profiles/${profileId}/dimensions`); }
  createDimension(profileId: number, body: any) { return this.http.post(`${API.knowledge}/api/v1/admin/profiles/${profileId}/dimensions`, body); }
  updateDimension(id: number, body: any) { return this.http.put(`${API.knowledge}/api/v1/admin/dimensions/${id}`, body); }
  deleteDimension(id: number) { return this.http.delete(`${API.knowledge}/api/v1/admin/dimensions/${id}`); }

  options(dimensionId: number) { return this.http.get<any[]>(`${API.knowledge}/api/v1/admin/dimensions/${dimensionId}/options`); }
  createOption(dimensionId: number, body: any) { return this.http.post(`${API.knowledge}/api/v1/admin/dimensions/${dimensionId}/options`, body); }
  updateOption(id: number, body: any) { return this.http.put(`${API.knowledge}/api/v1/admin/options/${id}`, body); }
  deleteOption(id: number) { return this.http.delete(`${API.knowledge}/api/v1/admin/options/${id}`); }

  rules(dimensionId: number) { return this.http.get<any[]>(`${API.knowledge}/api/v1/admin/dimensions/${dimensionId}/rules`); }
  createRule(dimensionId: number, body: any) { return this.http.post(`${API.knowledge}/api/v1/admin/dimensions/${dimensionId}/rules`, body); }
  updateRule(id: number, body: any) { return this.http.put(`${API.knowledge}/api/v1/admin/rules/${id}`, body); }
  deleteRule(id: number) { return this.http.delete(`${API.knowledge}/api/v1/admin/rules/${id}`); }

  crmIntegrations() { return this.http.get<any[]>(`${API.crm}/api/v1/admin/crm/integrations`); }
  createCrmIntegration(body: any) { return this.http.post(`${API.crm}/api/v1/admin/crm/integrations`, body); }
  updateCrmIntegration(id: number, body: any) { return this.http.put(`${API.crm}/api/v1/admin/crm/integrations/${id}`, body); }
  deleteCrmIntegration(id: number) { return this.http.delete(`${API.crm}/api/v1/admin/crm/integrations/${id}`); }
  crmOperations(integrationId: number) { return this.http.get<any[]>(`${API.crm}/api/v1/admin/crm/integrations/${integrationId}/operations`); }
  createCrmOperation(integrationId: number, body: any) { return this.http.post(`${API.crm}/api/v1/admin/crm/integrations/${integrationId}/operations`, body); }
  updateCrmOperation(id: number, body: any) { return this.http.put(`${API.crm}/api/v1/admin/crm/operations/${id}`, body); }
  deleteCrmOperation(id: number) { return this.http.delete(`${API.crm}/api/v1/admin/crm/operations/${id}`); }
  crmRoutes() { return this.http.get<any[]>(`${API.crm}/api/v1/admin/crm/routes`); }
  createCrmRoute(body: any) { return this.http.post(`${API.crm}/api/v1/admin/crm/routes`, body); }
  updateCrmRoute(id: number, body: any) { return this.http.put(`${API.crm}/api/v1/admin/crm/routes/${id}`, body); }
  deleteCrmRoute(id: number) { return this.http.delete(`${API.crm}/api/v1/admin/crm/routes/${id}`); }

  crmFieldMappings(operationId: number) { return this.http.get<any[]>(`${API.crm}/api/v1/admin/crm/operations/${operationId}/field-mappings`); }
  createCrmFieldMapping(operationId: number, body: any) { return this.http.post<any>(`${API.crm}/api/v1/admin/crm/operations/${operationId}/field-mappings`, body); }
  updateCrmFieldMapping(operationId: number, id: number, body: any) { return this.http.put<any>(`${API.crm}/api/v1/admin/crm/operations/${operationId}/field-mappings/${id}`, body); }
  deleteCrmFieldMapping(id: number) { return this.http.delete(`${API.crm}/api/v1/admin/crm/field-mappings/${id}`); }
  crmAssignments(operationId: number) { return this.http.get<any[]>(`${API.crm}/api/v1/admin/crm/operations/${operationId}/assignments`); }
  createCrmAssignment(operationId: number, body: any) { return this.http.post<any>(`${API.crm}/api/v1/admin/crm/operations/${operationId}/assignments`, body); }
  updateCrmAssignment(operationId: number, id: number, body: any) { return this.http.put<any>(`${API.crm}/api/v1/admin/crm/operations/${operationId}/assignments/${id}`, body); }
  deleteCrmAssignment(id: number) { return this.http.delete(`${API.crm}/api/v1/admin/crm/assignments/${id}`); }
  crmCatalogFields(integrationId: number) { return this.http.get<Record<string, any>>(`${API.crm}/api/v1/admin/crm/integrations/${integrationId}/catalog/fields`); }
  crmCatalogTeams(integrationId: number) { return this.http.get<any[]>(`${API.crm}/api/v1/admin/crm/integrations/${integrationId}/catalog/teams`); }
  crmCatalogUsers(integrationId: number) { return this.http.get<any[]>(`${API.crm}/api/v1/admin/crm/integrations/${integrationId}/catalog/users`); }
  crmEnsureTeam(operationId: number, body: any) { return this.http.post<any>(`${API.crm}/api/v1/admin/crm/operations/${operationId}/teams/ensure`, body); }
  crmValidate(operationId: number) { return this.http.post<any>(`${API.crm}/api/v1/admin/crm/operations/${operationId}/validate`, {}); }
  crmPreview(body: any) { return this.http.post<any>(`${API.crm}/api/v1/admin/crm/preview`, body); }
  crmAudits() { return this.http.get<any[]>(`${API.crm}/api/v1/admin/crm/audits`); }

  notificationChannels() { return this.http.get<any[]>(`${API.notifications}/api/v1/admin/notifications/channels`); }
  createNotificationChannel(body: any) { return this.http.post(`${API.notifications}/api/v1/admin/notifications/channels`, body); }
  updateNotificationChannel(id: number, body: any) { return this.http.put(`${API.notifications}/api/v1/admin/notifications/channels/${id}`, body); }
  deleteNotificationChannel(id: number) { return this.http.delete(`${API.notifications}/api/v1/admin/notifications/channels/${id}`); }
  notificationTemplates() { return this.http.get<any[]>(`${API.notifications}/api/v1/admin/notifications/templates`); }
  createNotificationTemplate(body: any) { return this.http.post(`${API.notifications}/api/v1/admin/notifications/templates`, body); }
  updateNotificationTemplate(id: number, body: any) { return this.http.put(`${API.notifications}/api/v1/admin/notifications/templates/${id}`, body); }
  deleteNotificationTemplate(id: number) { return this.http.delete(`${API.notifications}/api/v1/admin/notifications/templates/${id}`); }

  gmailStatus() { return this.http.get<any>('/api/v1/admin/gmail/status'); }
  gmailAuthorize() { return this.http.get<{url:string}>('/api/v1/admin/gmail/authorize'); }
  gmailSave(body:any) { return this.http.put<any>('/api/v1/admin/gmail/settings',body); }
  gmailSync() { return this.http.post<any>('/api/v1/admin/gmail/sync',{}); }
  gmailDisconnect() { return this.http.delete('/api/v1/admin/gmail/connection'); }
  operationalMetrics() { return this.http.get<any>('/api/v1/admin/metrics/overview'); }
  submitEmail(body: any) { return this.http.post(`${API.intake}/api/v1/intake/emails`, body); }
}
