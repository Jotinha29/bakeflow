import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { DashboardPageComponent } from './pages/dashboard/dashboard-page.component';
import { ConfirmationService, MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';

describe('AppComponent', () => {
  it('renders the application shell and initial route', async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: '', component: DashboardPageComponent }]),
        MessageService,
        ConfirmationService,
        providePrimeNG(),
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(AppComponent);
    TestBed.inject(Router).initialNavigation();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    const request = TestBed.inject(HttpTestingController).expectOne('/api/system/status');
    request.flush({ status: 'UP', postgres: 'UP', redis: 'UP' });
    TestBed.inject(HttpTestingController).expectOne('/api/v1/production-dashboard').flush({ planned: 0, inProgress: 0, completedToday: 0, recent: [] });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('BakeFlow');
    expect(fixture.nativeElement.textContent).toContain('Gestão inteligente');
    expect(fixture.nativeElement.textContent).toContain('Produção');
  });
});
