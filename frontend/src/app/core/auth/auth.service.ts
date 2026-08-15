import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { finalize, Observable, shareReplay, tap } from 'rxjs';
import { AuthSession, AuthUser, TokenResponse, UserPage } from './auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  readonly user = signal<AuthUser | null>(null);
  private token: string | null = null;
  private refreshing?: Observable<TokenResponse>;

  accessToken() { return this.token; }
  can(permission: string) { return this.user()?.permissions.includes(permission) ?? false; }

  login(email: string, password: string) {
    return this.http.post<TokenResponse>('/api/v1/auth/login', { email, password },
      { withCredentials: true }).pipe(tap((response) => this.accept(response)));
  }

  restore() { return this.refresh(); }

  refresh() {
    if (!this.refreshing) {
      this.refreshing = this.http.post<TokenResponse>('/api/v1/auth/refresh', {},
        { withCredentials: true }).pipe(
          tap((response) => this.accept(response)),
          finalize(() => this.refreshing = undefined),
          shareReplay({ bufferSize: 1, refCount: false }),
        );
    }
    return this.refreshing;
  }

  logout() {
    return this.http.post<void>('/api/v1/auth/logout', {}, { withCredentials: true })
      .pipe(finalize(() => this.clear()));
  }

  logoutAll() {
    return this.http.post<void>('/api/v1/auth/logout-all', {}, { withCredentials: true })
      .pipe(finalize(() => this.clear()));
  }

  sessions() { return this.http.get<AuthSession[]>('/api/v1/auth/sessions', { withCredentials: true }); }
  revokeSession(id: string) { return this.http.delete<void>(`/api/v1/auth/sessions/${id}`); }
  changePassword(currentPassword: string, newPassword: string) {
    return this.http.post<void>('/api/v1/auth/change-password', { currentPassword, newPassword },
      { withCredentials: true });
  }
  users(params: Record<string, string | number | boolean>) {
    return this.http.get<UserPage>('/api/v1/users', { params });
  }
  createUser(input: unknown) { return this.http.post<AuthUser>('/api/v1/users', input); }
  updateUser(id: string, input: unknown) { return this.http.put<AuthUser>(`/api/v1/users/${id}`, input); }

  clear() {
    this.token = null;
    this.user.set(null);
    void this.router.navigateByUrl('/login');
  }

  private accept(response: TokenResponse) {
    this.token = response.accessToken;
    this.user.set(response.user);
  }
}
