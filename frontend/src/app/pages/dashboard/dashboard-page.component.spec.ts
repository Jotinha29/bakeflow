import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { SystemStatusService } from '../../core/services/system-status.service';
import { InventoryService } from '../../features/inventory/inventory.service';
import { ProductionDashboardService } from '../../features/production/production-dashboard.service';
import { DashboardPageComponent } from './dashboard-page.component';

describe('DashboardPageComponent', () => {
  it('shows real operational data in inventory, production, recent activity and infrastructure order', async () => {
    const inventory = {
      items: () => of({ content: [{ id: 'flour', minimumStock: 20 }] }),
      stockBalances: (filters: Record<string, unknown>) => filters['expiration']
        ? of({ content: [], totalElements: 2 })
        : of({ content: [{ itemId: 'flour', quantity: 8 }], totalElements: 1 }),
    };
    const production = {
      summary: () => of({
        planned: 4,
        inProgress: 2,
        completedToday: 3,
        recent: [{ id: 'order', code: 'DEMO-OP-001', outputItemName: 'Pão francês', plannedQuantity: 500, unit: 'UNIT', status: 'PLANNED' }],
      }),
    };
    const system = { getStatus: () => of({ status: 'UP', postgres: 'UP', redis: 'UP' }) };
    await TestBed.configureTestingModule({
      imports: [DashboardPageComponent],
      providers: [
        { provide: InventoryService, useValue: inventory },
        { provide: ProductionDashboardService, useValue: production },
        { provide: SystemStatusService, useValue: system },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(DashboardPageComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Itens abaixo do estoque mínimo');
    expect(text).toContain('Lotes próximos do vencimento');
    expect(text).toContain('DEMO-OP-001');
    expect(text).toContain('Pão francês');
    expect(text).toContain('Planejada');
    expect(text.indexOf('Situação do estoque')).toBeLessThan(text.indexOf('Visão geral da produção'));
    expect(text.indexOf('Visão geral da produção')).toBeLessThan(text.indexOf('Serviços essenciais'));
  });
});
