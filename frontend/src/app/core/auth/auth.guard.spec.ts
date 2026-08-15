import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { authGuard, permissionGuard } from './auth.guard';

describe('authentication guards', () => {
  let router: Router;
  const auth = { user: () => null as unknown, restore: () => of({}), can: () => false };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth }] });
    router = TestBed.inject(Router);
  });

  it('restores a session before allowing an authenticated route', async () => {
    auth.restore = () => of({});
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    expect(await firstValueFrom(result as ReturnType<typeof of>)).toBe(true);
  });

  it('redirects to login when session restoration fails', async () => {
    auth.restore = () => throwError(() => new Error('unauthenticated'));
    const result = TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
    expect((await firstValueFrom(result as ReturnType<typeof of>) as UrlTree).toString()).toBe('/login');
  });

  it('denies users route without USER_READ', () => {
    auth.can = () => false;
    const result = TestBed.runInInjectionContext(() => permissionGuard('USER_READ')({} as never, {} as never));
    expect((result as UrlTree).toString()).toBe('/');
    expect(router.url).toBe('/');
  });
});
