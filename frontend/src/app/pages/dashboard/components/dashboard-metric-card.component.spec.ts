import { TestBed } from '@angular/core/testing';
import { DashboardMetricCardComponent } from './dashboard-metric-card.component';

describe('DashboardMetricCardComponent', () => {
  it('prioritizes the metric label and localized value', () => {
    const fixture = TestBed.createComponent(DashboardMetricCardComponent);
    fixture.componentRef.setInput('label', 'Lotes próximos do vencimento');
    fixture.componentRef.setInput('value', 3000);
    fixture.componentRef.setInput('tone', 'warning');
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Lotes próximos do vencimento');
    expect(element.textContent).toContain('3.000');
    expect(element.querySelector('[data-tone="warning"]')).toBeTruthy();
  });
});
