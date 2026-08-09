import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { ConfirmationService, MessageService } from 'primeng/api';
import { InventoryService } from '../../../features/inventory/inventory.service';
import { Location } from '../../../features/inventory/inventory.models';
import { Signal } from '@angular/core';
import { TreeNode } from 'primeng/api';
import { LocationsPageComponent } from './locations-page.component';
describe('LocationsPageComponent', () => {
  interface Harness {
    nodes: Signal<TreeNode<Location>[]>;
    form: { controls: { parentId: { value: string | null } } };
    editing: Signal<Location | undefined>;
    open(location?: Location, parent?: Location): void;
  }
  it('builds a hierarchy and preselects the parent when adding a child', async () => {
    const parent: Location = {
      id: 'parent',
      name: 'Warehouse',
      code: 'WH',
      type: 'WAREHOUSE',
      active: true,
      children: [
        {
          id: 'child',
          name: 'Shelf',
          code: 'S1',
          type: 'SHELF',
          parentId: 'parent',
          active: true,
          children: [],
        },
      ],
    };
    const api = {
      locationTree: vi.fn(() => of([parent])),
      locations: vi.fn(),
      saveLocation: vi.fn(() => of({})),
    };
    await TestBed.configureTestingModule({
      imports: [LocationsPageComponent],
      providers: [
        provideNoopAnimations(),
        { provide: InventoryService, useValue: api },
        MessageService,
        ConfirmationService,
      ],
    }).compileComponents();
    const c = TestBed.createComponent(LocationsPageComponent).componentInstance as unknown as Harness;
    expect(c.nodes()[0]?.children?.[0]?.label).toBe('Shelf');
    c.open(undefined, parent);
    expect(c.form.controls.parentId.value).toBe('parent');
    expect(c.editing()).toBeUndefined();
  });
});
