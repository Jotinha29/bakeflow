import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LocalizedNumberPipe } from '../../../core/i18n/localized-number.pipe';

@Component({
  selector: 'app-dashboard-metric-card',
  imports: [LocalizedNumberPipe],
  template: `
    <article class="metric" [attr.data-tone]="tone()">
      <div class="metric__label">
        @if (icon()) { <i [class]="'pi ' + icon()" aria-hidden="true"></i> }
        <span>{{ label() }}</span>
      </div>
      <strong>{{ value() | localizedNumber }}</strong>
    </article>
  `,
  styles: [`
    :host { display: block; min-width: 0; }
    .metric { background: var(--bf-surface); border: 1px solid var(--bf-border); border-radius: var(--bf-radius-lg); display: flex; flex-direction: column; gap: var(--space-3); min-height: 7rem; padding: 1.1rem 1.25rem; }
    .metric[data-tone='warning'] { border-left: 4px solid var(--bf-warning); }
    .metric[data-tone='danger'] { border-left: 4px solid var(--bf-danger); }
    .metric[data-tone='success'] { border-left: 4px solid var(--bf-success); }
    .metric__label { align-items: center; color: var(--bf-text-muted); display: flex; font-size: .84rem; font-weight: 650; gap: var(--space-2); line-height: 1.3; }
    .metric__label i { color: #9a5728; }
    strong { font-size: clamp(1.75rem, 3vw, 2.35rem); font-variant-numeric: tabular-nums; line-height: 1; }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardMetricCardComponent {
  readonly label = input.required<string>();
  readonly value = input.required<number>();
  readonly icon = input<string>();
  readonly tone = input<'neutral' | 'warning' | 'danger' | 'success'>('neutral');
}
