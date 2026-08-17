import { ChangeDetectionStrategy, Component, input } from '@angular/core'; import { TagModule } from 'primeng/tag';
export type BadgeTone='success'|'info'|'warn'|'danger'|'secondary'|'contrast';
@Component({selector:'app-status-badge',imports:[TagModule],template:`<p-tag [value]="label()" [severity]="tone()" />`,changeDetection:ChangeDetectionStrategy.OnPush})
export class StatusBadgeComponent {readonly label=input.required<string>();readonly tone=input<BadgeTone>('secondary');}
