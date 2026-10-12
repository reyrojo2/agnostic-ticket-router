import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { API } from './api.config';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const protectedApi = [API.knowledge, API.crm, API.notifications]
    .some(base => request.url.startsWith(base));

  const token = auth.token();
  if (!protectedApi || !token || request.url.endsWith('/api/auth/login')) {
    return next(request);
  }

  return next(request.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  }));
};
