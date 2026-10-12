import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { API } from './api.config';

interface LoginResponse {
  authenticated: boolean;
  username: string;
  authorities: string[];
  token: string;
  expiresAt: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'atr_token';
  private readonly userKey = 'atr_user';

  readonly username = signal(localStorage.getItem(this.userKey));

  constructor(private readonly http: HttpClient) {}

  login(username: string, password: string) {
    return this.http.post<LoginResponse>(`${API.knowledge}/api/auth/login`, { username, password })
      .pipe(tap(response => {
        localStorage.setItem(this.tokenKey, response.token);
        localStorage.setItem(this.userKey, response.username);
        this.username.set(response.username);
      }));
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.username.set(null);
  }

  token(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return !!this.token();
  }
}
