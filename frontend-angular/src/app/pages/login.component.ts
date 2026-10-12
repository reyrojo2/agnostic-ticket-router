import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="login-page">
      <form class="login-card" (ngSubmit)="submit()">
        <div class="brand large"><span class="brand-mark">ATR</span><div><strong>AgnosticTicketRouter</strong><small>Configuración académica</small></div></div>
        <h1>Iniciar sesión</h1>
        <p class="muted">Administra conocimiento, integraciones CRM y notificaciones.</p>
        <label>Usuario<input name="username" [(ngModel)]="username" required></label>
        <label>Contraseña<input name="password" [(ngModel)]="password" type="password" required></label>
        @if (error) { <div class="alert error">{{ error }}</div> }
        <button class="btn btn-primary full" [disabled]="loading">{{ loading ? 'Ingresando…' : 'Ingresar' }}</button>
      </form>
    </div>
  `
})
export class LoginComponent {
  username = '';
  password = '';
  loading = false;
  error = '';

  constructor(private readonly auth: AuthService, private readonly router: Router) {}

  submit(): void {
    this.loading = true;
    this.error = '';
    this.auth.login(this.username, this.password).subscribe({
      next: () => this.router.navigateByUrl('/'),
      error: () => { this.loading = false; this.error = 'Credenciales inválidas o servicio no disponible.'; }
    });
  }
}
