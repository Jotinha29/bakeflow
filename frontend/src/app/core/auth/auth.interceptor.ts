import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

const LOGIN = '/api/v1/auth/login';
const REFRESH = '/api/v1/auth/refresh';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const internal = request.url.startsWith('/api/');
  const authenticationRequest = request.url === LOGIN || request.url === REFRESH;
  const authenticated = internal && !authenticationRequest && auth.accessToken()
    ? request.clone({ setHeaders: { Authorization: `Bearer ${auth.accessToken()}` } })
    : request;

  return next(authenticated).pipe(
    catchError((error: HttpErrorResponse) => {
      if (!internal || authenticationRequest || error.status !== 401) return throwError(() => error);
      return auth.refresh().pipe(
        switchMap(() => next(request.clone({
          setHeaders: { Authorization: `Bearer ${auth.accessToken()}` },
        }))),
        catchError((refreshError) => {
          auth.clear();
          return throwError(() => refreshError);
        }),
      );
    }),
  );
};
