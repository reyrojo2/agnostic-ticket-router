import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginComponent } from './pages/login.component';
import { DashboardComponent } from './pages/dashboard.component';
import { KnowledgeComponent } from './pages/knowledge.component';
import { CrmComponent } from './pages/crm.component';
import { NotificationsComponent } from './pages/notifications.component';
import { GmailComponent } from './pages/gmail.component';
import { AiProvidersComponent } from './pages/ai-providers.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'knowledge', component: KnowledgeComponent, canActivate: [authGuard] },
  { path: 'ai-providers', component: AiProvidersComponent, canActivate: [authGuard] },
  { path: 'crm', component: CrmComponent, canActivate: [authGuard] },
  { path: 'notifications', component: NotificationsComponent, canActivate: [authGuard] },
  { path: 'gmail', component: GmailComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
