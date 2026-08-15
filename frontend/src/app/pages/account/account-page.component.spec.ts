import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { ConfirmationService, MessageService } from 'primeng/api';
import { AuthService } from '../../core/auth/auth.service';
import { AccountPageComponent } from './account-page.component';

describe('AccountPageComponent', () => {
  const auth = {
    user: vi.fn(() => ({ name: 'Admin', email: 'admin@example.test', roles: ['ADMIN'] })),
    sessions: vi.fn(() => of([])),
    changePassword: vi.fn(() => of(undefined)),
    revokeSession: vi.fn(() => of(undefined)),
    logout: vi.fn(() => of(undefined)),
    logoutAll: vi.fn(() => of(undefined)),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [AccountPageComponent],
      providers: [
        provideNoopAnimations(),
        ConfirmationService,
        MessageService,
        { provide: AuthService, useValue: auth },
      ],
    }).compileComponents();
  });

  it('does not submit a password change until confirmation matches', () => {
    const component = TestBed.createComponent(AccountPageComponent).componentInstance;
    component.current = 'CurrentPass123';
    component.next = 'NewPassword123';
    component.confirmation = 'DifferentPassword';
    component.change();
    expect(auth.changePassword).not.toHaveBeenCalled();
    component.confirmation = component.next;
    component.change();
    expect(auth.changePassword).toHaveBeenCalledWith('CurrentPass123', 'NewPassword123');
  });

  it('asks for confirmation before ending all sessions', () => {
    const fixture = TestBed.createComponent(AccountPageComponent);
    const confirmations = TestBed.inject(ConfirmationService);
    const spy = vi.spyOn(confirmations, 'confirm');
    fixture.componentInstance.logoutAll();
    expect(spy).toHaveBeenCalledOnce();
    expect(auth.logoutAll).not.toHaveBeenCalled();
  });
});
