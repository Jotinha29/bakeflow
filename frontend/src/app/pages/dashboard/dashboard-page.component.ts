import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { SystemStatusService } from '../../core/services/system-status.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [AsyncPipe, CardModule, TagModule],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPageComponent {
  protected readonly status$ = inject(SystemStatusService).getStatus();
}
