import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MultiSelectModule } from 'primeng/multiselect';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../core/auth/auth.service';
import { AuthUser } from '../../core/auth/auth.models';
import { I18nService } from '../../core/i18n/i18n.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { LocalizedDateTimePipe } from '../../core/i18n/localized-date-time.pipe';
import { RoleLabelPipe } from '../../core/i18n/role-label.pipe';
import { PageHeaderComponent } from '../../shared/ui/page-header/page-header.component';

@Component({
  standalone: true,
  imports: [
    FormsModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    MultiSelectModule,
    PasswordModule,
    SelectModule,
    TableModule,
    TagModule,
    TooltipModule,
    TranslatePipe,
    LocalizedDateTimePipe,
    RoleLabelPipe,
    PageHeaderComponent,
  ],
  templateUrl: './users-page.component.html',
})
export class UsersPageComponent {
  readonly auth = inject(AuthService);
  private readonly confirmations = inject(ConfirmationService);
  private readonly messages = inject(MessageService);
  private readonly i18n = inject(I18nService);
  readonly users = signal<AuthUser[]>([]);
  readonly total = signal(0);
  readonly dialog = signal(false);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly roleOptions = ['ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER'];
  readonly statusOptions = [
    { label: 'common.active', value: true },
    { label: 'common.inactive', value: false },
  ];
  editing?: AuthUser;
  name = '';
  email = '';
  password = '';
  roles: string[] = ['VIEWER'];
  active = true;
  search = '';
  status: boolean | null = null;
  role: string | null = null;

  constructor() {
    this.load();
  }

  load(page = 0) {
    this.loading.set(true);
    this.auth
      .users({
        search: this.search,
        active: this.status ?? '',
        role: this.role ?? '',
        page,
        size: 20,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (value) => {
          this.users.set(value.content);
          this.total.set(value.total);
        },
        error: () => this.feedback('error', 'users.loadError'),
      });
  }

  clearFilters() {
    this.search = '';
    this.status = null;
    this.role = null;
    this.load();
  }

  open(user?: AuthUser) {
    this.editing = user;
    this.name = user?.name ?? '';
    this.email = user?.email ?? '';
    this.password = '';
    this.roles = [...(user?.roles ?? ['VIEWER'])];
    this.active = user?.active ?? true;
    this.dialog.set(true);
  }

  save() {
    if (
      this.saving() ||
      !this.name.trim() ||
      !this.email.trim() ||
      this.roles.length === 0 ||
      (!this.editing && this.password.length < 8)
    )
      return;
    if (this.editing?.active && !this.active) {
      this.confirmations.confirm({
        header: this.i18n.translate('users.deactivate'),
        message: this.i18n.translate('users.deactivateConfirm'),
        accept: () => this.persist(),
      });
      return;
    }
    this.persist();
  }

  private persist() {
    this.saving.set(true);
    const common = { name: this.name, email: this.email, roles: this.roles, active: this.active };
    const operation = this.editing
      ? this.auth.updateUser(this.editing.id, common)
      : this.auth.createUser({ ...common, password: this.password });
    operation.pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.dialog.set(false);
        this.feedback(
          'success',
          this.editing?.active && !this.active
            ? 'users.deactivated'
            : this.editing
              ? 'users.updated'
              : 'users.created',
        );
        this.load();
      },
      error: () => this.feedback('error', 'users.saveError'),
    });
  }

  private feedback(severity: 'success' | 'error', key: string) {
    this.messages.add({
      severity,
      summary: this.i18n.translate(`common.${severity}`),
      detail: this.i18n.translate(key),
    });
  }
}
