import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/auth/auth.service';
import { AuthSession } from '../../core/auth/auth.models';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';

@Component({ standalone: true, imports: [FormsModule, ButtonModule, TranslatePipe],
  templateUrl: './account-page.component.html' })
export class AccountPageComponent {
  readonly auth = inject(AuthService);
  private readonly messages = inject(MessageService);
  private readonly i18n = inject(I18nService);
  readonly sessions = signal<AuthSession[]>([]);
  readonly loading = signal(false);
  readonly changing = signal(false);
  current = '';
  next = '';

  constructor() { this.load(); }

  load() {
    this.loading.set(true);
    this.auth.sessions().pipe(finalize(() => this.loading.set(false))).subscribe({
      next: (value) => this.sessions.set(value),
      error: () => this.feedback('error', 'account.sessionsLoadError'),
    });
  }

  change() {
    if (this.changing() || this.next.length < 8) return;
    this.changing.set(true);
    this.auth.changePassword(this.current, this.next).pipe(finalize(() => this.changing.set(false)))
      .subscribe({
        next: () => { this.current = ''; this.next = ''; this.feedback('success', 'account.passwordChanged'); },
        error: (error) => this.feedback('error', error.status === 401 ?
          'account.currentPasswordInvalid' : 'account.passwordChangeError'),
      });
  }

  revoke(id: string) {
    this.auth.revokeSession(id).subscribe({
      next: () => { this.feedback('success', 'account.sessionRevoked'); this.load(); },
      error: () => this.feedback('error', 'account.sessionRevokeError'),
    });
  }

  logout() { this.auth.logout().subscribe({ error: () => this.feedback('error', 'account.logoutError') }); }
  logoutAll() { this.auth.logoutAll().subscribe({ error: () => this.feedback('error', 'account.logoutError') }); }

  private feedback(severity: 'success' | 'error', key: string) {
    this.messages.add({ severity, summary: this.i18n.translate(`common.${severity}`),
      detail: this.i18n.translate(key) });
  }
}
