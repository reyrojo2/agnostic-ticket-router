import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (auth.isAuthenticated()) {
      <div class="app-shell">
        <aside class="sidebar">
          <div class="brand">
            <span class="brand-mark">ATR</span>
            <div><strong>AgnosticTicketRouter</strong><small>Academic MVP</small></div>
          </div>
          <nav>
            <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">Dashboard</a>
            <a routerLink="/knowledge" routerLinkActive="active">Business Knowledge</a>
            <a routerLink="/crm" routerLinkActive="active">Integraciones CRM</a>
            <a routerLink="/notifications" routerLinkActive="active">Notificaciones</a>
            <a routerLink="/gmail" routerLinkActive="active">Buzón Gmail</a>
          </nav>
          <div class="sidebar-footer">
            <span>{{ auth.username() }}</span>
            <button class="btn btn-ghost" (click)="logout()">Cerrar sesión</button>
          </div>
        </aside>
        <main class="content"><router-outlet /></main>
      </div>
    } @else {
      <router-outlet />
    }
  `
})
export class AppComponent {
  constructor(public readonly auth: AuthService, private readonly router: Router) {}

  logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
