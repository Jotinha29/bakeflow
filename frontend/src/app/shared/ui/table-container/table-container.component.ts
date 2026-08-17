import { ChangeDetectionStrategy, Component } from '@angular/core';
@Component({selector:'app-table-container',template:`<section class="bf-table-container"><ng-content /></section>`,styleUrl:'./table-container.component.scss',changeDetection:ChangeDetectionStrategy.OnPush})
export class TableContainerComponent {}
