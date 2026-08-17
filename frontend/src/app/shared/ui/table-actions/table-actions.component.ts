import { ChangeDetectionStrategy, Component } from '@angular/core';
@Component({selector:'app-table-actions',template:`<div class="bf-table-actions"><ng-content /></div>`,styles:[`.bf-table-actions{align-items:center;display:flex;gap:var(--space-1);justify-content:flex-end;white-space:nowrap}.bf-table-actions ::ng-deep .p-button{min-height:2.25rem;min-width:2.25rem}`],changeDetection:ChangeDetectionStrategy.OnPush})
export class TableActionsComponent {}
