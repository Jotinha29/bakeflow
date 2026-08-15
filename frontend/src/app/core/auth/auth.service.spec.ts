import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

const user = {
  id: '1',
  name: 'Viewer',
  email: 'viewer@bakeflow.local',
  active: true,
  roles: ['VIEWER'],
  permissions: ['ITEM_READ'],
};

describe('AuthService and interceptor', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  it('stores the access token only after login and adds it only to internal API calls', () => {
    auth.login(user.email, 'a secure passphrase').subscribe();
    http.expectOne('/api/v1/auth/login').flush({ accessToken: 'access', user });

    const client = TestBed.inject(HttpClient);
    client.get('/api/v1/items').subscribe();
    expect(http.expectOne('/api/v1/items').request.headers.get('Authorization')).toBe(
      'Bearer access',
    );
    client.get('https://example.com/data').subscribe();
    expect(http.expectOne('https://example.com/data').request.headers.has('Authorization')).toBe(
      false,
    );
  });

  it('shares one refresh request among five concurrent subscribers', () => {
    for (let index = 0; index < 5; index++) auth.refresh().subscribe();
    const requests = http.match('/api/v1/auth/refresh');
    expect(requests.length).toBe(1);
    requests[0].flush({ accessToken: 'renewed', user });
    expect(auth.accessToken()).toBe('renewed');
  });

  it('does not attach a token to login or refresh', () => {
    auth.login(user.email, 'a secure passphrase').subscribe();
    const login = http.expectOne('/api/v1/auth/login');
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({ accessToken: 'access', user });
    auth.refresh().subscribe();
    const refresh = http.expectOne('/api/v1/auth/refresh');
    expect(refresh.request.headers.has('Authorization')).toBe(false);
    refresh.flush({ accessToken: 'renewed', user });
  });

  it('clears local state and redirects once when refresh fails', async () => {
    const navigate = vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    auth.login(user.email, 'a secure passphrase').subscribe();
    http.expectOne('/api/v1/auth/login').flush({ accessToken: 'access', user });
    const client = TestBed.inject(HttpClient);
    client.get('/api/v1/items').subscribe({ error: () => undefined });
    http.expectOne('/api/v1/items').flush({}, { status: 401, statusText: 'Unauthorized' });
    http.expectOne('/api/v1/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });
    await Promise.resolve();
    expect(auth.user()).toBeNull();
    expect(navigate).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});
