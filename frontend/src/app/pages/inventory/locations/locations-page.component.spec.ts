import { Signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ConfirmationService, MessageService, TreeNode } from 'primeng/api';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { Location } from '../../../features/inventory/inventory.models';
import { InventoryService } from '../../../features/inventory/inventory.service';
import { LocationsPageComponent } from './locations-page.component';

describe('LocationsPageComponent', () => {
  const shelf: Location = {
    id: 'shelf',
    name: 'Prateleira A',
    code: 'SH-A',
    type: 'SHELF',
    parentId: 'warehouse',
    active: true,
    children: [],
  };
  const warehouse: Location = {
    id: 'warehouse',
    name: 'Estoque',
    code: 'WH',
    type: 'WAREHOUSE',
    active: true,
    children: [shelf],
  };

  interface Harness {
    nodes: Signal<TreeNode<Location>[]>;
    selectedLocation: Signal<Location | undefined>;
    selectedPath: Signal<string[]>;
    form: { controls: { parentId: { value: string | null } } };
    filters: { patchValue(value: object): void };
    open(location?: Location, parent?: Location): void;
    selectNode(event: { node: TreeNode<Location> }): void;
    applyFilters(): void;
    clearFilters(): void;
    toggle(location: Location): void;
  }

  async function setup(tree: Location[] = [warehouse]) {
    const api = {
      locationTree: vi.fn(() => of(tree)),
      locations: vi.fn(),
      saveLocation: vi.fn(() => of(warehouse)),
      setLocationActive: vi.fn(() => of({ ...warehouse, active: false })),
    };
    const confirmation = { confirm: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [LocationsPageComponent],
      providers: [
        provideNoopAnimations(),
        { provide: InventoryService, useValue: api },
        { provide: ConfirmationService, useValue: confirmation },
        MessageService,
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(LocationsPageComponent);
    fixture.detectChanges();
    return {
      fixture,
      component: fixture.componentInstance as unknown as Harness,
      api,
      confirmation,
    };
  }

  afterEach(() => TestBed.resetTestingModule());

  it('renders the visual root and multiple hierarchy levels without persisting a root entity', async () => {
    const { fixture, component, api } = await setup();
    expect(fixture.nativeElement.textContent).toContain('Estrutura da padaria');
    expect(component.nodes()[0]?.children?.[0]?.label).toBe('Prateleira A');
    expect(api.locationTree).toHaveBeenCalledOnce();
    expect(api.saveLocation).not.toHaveBeenCalled();
  });

  it('selects a node, updates details and builds its complete hierarchy path', async () => {
    const { component } = await setup();
    const child = component.nodes()[0].children![0];
    component.selectNode({ node: child });
    expect(component.selectedLocation()?.id).toBe('shelf');
    expect(component.selectedPath()).toEqual(['Estrutura da padaria', 'Estoque', 'Prateleira A']);
  });

  it('keeps expansion and selection after a refresh when the location still exists', async () => {
    const { component } = await setup();
    const root = component.nodes()[0];
    root.expanded = false;
    component.selectNode({ node: root });
    component.clearFilters();
    expect(component.nodes()[0].expanded).toBe(false);
    expect(component.selectedLocation()?.id).toBe('warehouse');
  });

  it('keeps ancestors for context and clears selection when a filter removes it', async () => {
    const { component, api } = await setup();
    component.selectNode({ node: component.nodes()[0].children![0] });
    component.filters.patchValue({ search: 'Estoque' });
    component.applyFilters();
    expect(component.nodes()[0].label).toBe('Estoque');
    expect(component.nodes()[0].children).toEqual([]);
    expect(component.selectedLocation()).toBeUndefined();
    expect(api.locations).not.toHaveBeenCalled();
  });

  it('preselects the selected location as parent when adding a child', async () => {
    const { component } = await setup();
    component.open(undefined, warehouse);
    expect(component.form.controls.parentId.value).toBe('warehouse');
  });

  it('waits for confirmation before changing status', async () => {
    const { component, api, confirmation } = await setup();
    component.toggle(warehouse);
    expect(api.setLocationActive).not.toHaveBeenCalled();
    const request = confirmation.confirm.mock.calls[0][0];
    request.accept();
    expect(api.setLocationActive).toHaveBeenCalledWith('warehouse', false);
    expect(api.locationTree).toHaveBeenCalledTimes(2);
  });

  it('shows the no-selection state and hides administrative actions without ITEM_WRITE', async () => {
    const { fixture } = await setup();
    TestBed.inject(AuthService).user.set({
      id: 'viewer',
      name: 'Viewer',
      email: 'viewer@bakeflow.local',
      roles: ['VIEWER'],
      permissions: ['ITEM_READ'],
      active: true,
    });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Selecione um local');
    expect(fixture.nativeElement.textContent).not.toContain('Novo local');
  });

  it('uses the shared empty state and only offers creation to authorized users', async () => {
    const { fixture } = await setup([]);
    expect(fixture.nativeElement.textContent).toContain('Nenhum local cadastrado');
    expect(fixture.nativeElement.textContent).not.toContain('Novo local');
  });
});
