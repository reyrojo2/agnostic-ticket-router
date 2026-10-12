import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginComponent } from './pages/login.component';
import { DashboardComponent } from './pages/dashboard.component';
import { KnowledgeComponent } from './pages/knowledge.component';
import { CrmComponent } from './pages/crm.component';
import { NotificationsComponent } from './pages/notifications.component';
import { IntakeComponent } from './pages/intake.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'knowledge', component: KnowledgeComponent, canActivate: [authGuard] },
  { path: 'crm', component: CrmComponent, canActivate: [authGuard] },
  { path: 'notifications', component: NotificationsComponent, canActivate: [authGuard] },
  { path: 'demo', component: IntakeComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
