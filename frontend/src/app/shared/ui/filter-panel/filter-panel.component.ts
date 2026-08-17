import { ChangeDetectionStrategy, Component } from '@angular/core';
@Component({selector:'app-filter-panel',template:`<section class="bf-filter-panel"><div class="bf-filter-panel__fields"><ng-content /></div><div class="bf-filter-panel__actions"><ng-content select="[filterActions]" /></div></section>`,styleUrl:'./filter-panel.component.scss',changeDetection:ChangeDetectionStrategy.OnPush})
export class FilterPanelComponent {}
