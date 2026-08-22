import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { Location } from '../../../features/inventory/inventory.models';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { SectionCardComponent } from '../../../shared/ui/section-card/section-card.component';
import { StatusBadgeComponent } from '../../../shared/ui/status-badge/status-badge.component';

@Component({
  selector: 'app-location-details-panel',
  imports: [ButtonModule, TranslatePipe, SectionCardComponent, StatusBadgeComponent],
  templateUrl: './location-details-panel.component.html',
  styleUrl: './location-details-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LocationDetailsPanelComponent {
  readonly location = input.required<Location>();
  readonly parentName = input.required<string>();
  readonly hierarchyPath = input.required<string[]>();
  readonly typeLabel = input.required<string>();
  readonly canManage = input(false);
  readonly edit = output<void>();
  readonly addChild = output<void>();
  readonly toggleActive = output<void>();
}
