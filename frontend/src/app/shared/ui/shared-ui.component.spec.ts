import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { PageHeaderComponent } from './page-header/page-header.component';
import { FilterPanelComponent } from './filter-panel/filter-panel.component';
import { TableContainerComponent } from './table-container/table-container.component';
import { TableActionsComponent } from './table-actions/table-actions.component';
import { StatusBadgeComponent } from './status-badge/status-badge.component';
import { EmptyStateComponent } from './empty-state/empty-state.component';
import { LoadingStateComponent } from './loading-state/loading-state.component';
import { SectionCardComponent } from './section-card/section-card.component';

@Component({imports:[PageHeaderComponent,FilterPanelComponent,TableContainerComponent,TableActionsComponent,StatusBadgeComponent,EmptyStateComponent,LoadingStateComponent,SectionCardComponent],template:`<app-page-header eyebrow="Inventory" title="Stock" description="Description"><button>Action</button></app-page-header><app-filter-panel><input aria-label="Search"><button filterActions>Filter</button></app-filter-panel><app-table-container><table><tr><td>Row</td></tr></table></app-table-container><app-table-actions><button>Edit</button></app-table-actions><app-status-badge label="Active" tone="success"/><app-empty-state title="Empty" description="Nothing here" icon="pi-inbox"/><app-loading-state label="Loading"/><app-section-card title="Section"><span>Content</span></app-section-card>`})
class HostComponent {}

describe('shared UI composition',()=>{
  it('renders semantic content and projected actions',async()=>{await TestBed.configureTestingModule({imports:[HostComponent]}).compileComponents();const fixture=TestBed.createComponent(HostComponent);fixture.detectChanges();const element=fixture.nativeElement as HTMLElement;expect(element.querySelector('h1')?.textContent).toContain('Stock');expect(element.textContent).toContain('Action');expect(element.textContent).toContain('Filter');expect(element.textContent).toContain('Row');expect(element.textContent).toContain('Empty');expect(element.querySelector('[role="status"]')).toBeTruthy();expect(element.textContent).toContain('Section');});
});
