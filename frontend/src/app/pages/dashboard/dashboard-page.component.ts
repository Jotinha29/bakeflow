import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { SystemStatusService } from '../../core/services/system-status.service';
import { TranslatePipe } from '../../core/i18n/translate.pipe';
import { ProductionDashboardService } from '../../features/production/production-dashboard.service';
import { LocalizedNumberPipe } from '../../core/i18n/localized-number.pipe';
import { InventoryService } from '../../features/inventory/inventory.service';
import { forkJoin, map } from 'rxjs';
import { SectionCardComponent } from '../../shared/ui/section-card/section-card.component';
import { StatusBadgeComponent, BadgeTone } from '../../shared/ui/status-badge/status-badge.component';
import { DashboardMetricCardComponent } from './components/dashboard-metric-card.component';
import { ProductionStatus } from '../../features/production/production.models';

@Component({
  selector: 'app-dashboard-page',
  imports: [AsyncPipe, TranslatePipe, LocalizedNumberPipe, SectionCardComponent, StatusBadgeComponent, DashboardMetricCardComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPageComponent {
  protected readonly status$ = inject(SystemStatusService).getStatus();
  protected readonly production$ = inject(ProductionDashboardService).summary();
  private readonly inventory = inject(InventoryService);
  protected readonly inventory$ = forkJoin({
    items: this.inventory.items({ active: true, page: 0, size: 100 }),
    balances: this.inventory.stockBalances({ page: 0, size: 100 }),
    expiring: this.inventory.stockBalances({ expiration: 'EXPIRING_SOON', page: 0, size: 1 }),
  }).pipe(map(({items,balances,expiring}) => {
    const totals = new Map<string, number>();
    balances.content.forEach((balance) => totals.set(balance.itemId, (totals.get(balance.itemId) ?? 0) + balance.quantity));
    return { belowMinimum: items.content.filter((item) => item.minimumStock != null && (totals.get(item.id) ?? 0) < item.minimumStock).length, expiringBatches: expiring.totalElements };
  }));

  protected productionTone(status: ProductionStatus): BadgeTone {
    return { PLANNED: 'info', IN_PROGRESS: 'warn', COMPLETED: 'success', CANCELLED: 'secondary' }[status] as BadgeTone;
  }
}
