import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { ConfirmationService, MessageService } from 'primeng/api';
import { InventoryService } from '../../../features/inventory/inventory.service';
import { Item } from '../../../features/inventory/inventory.models';
import { ItemsPageComponent } from './items-page.component';
describe('ItemsPageComponent', () => {
  interface Harness {
    filters: { controls: { search: { setValue(value: string): void; value: string | null } } };
    form: {
      controls: {
        barcode: { setValue(value: string): void };
        name: { value: string | null };
      };
      setValue(value: Record<string, string | number | null>): void;
    };
    applyFilters(): void;
    clearFilters(): void;
    lookup(): void;
    useProduct(): void;
    save(): void;
  }
  const empty = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 };
  let api: {
    items: ReturnType<typeof vi.fn>;
    saveItem: ReturnType<typeof vi.fn>;
    setItemActive: ReturnType<typeof vi.fn>;
    lookupProduct: ReturnType<typeof vi.fn>;
  };
  beforeEach(async () => {
    api = {
      items: vi.fn(() => of(empty)),
      saveItem: vi.fn(() => of({})),
      setItemActive: vi.fn(() => of({})),
      lookupProduct: vi.fn(() =>
        of({ status: 'FOUND', barcode: '789', name: 'External Product', categories: [] }),
      ),
    };
    await TestBed.configureTestingModule({
      imports: [ItemsPageComponent],
      providers: [
        provideNoopAnimations(),
        { provide: InventoryService, useValue: api },
        MessageService,
        ConfirmationService,
      ],
    }).compileComponents();
  });
  it('only requests new data when filters are explicitly applied and clears them', () => {
    const c = TestBed.createComponent(ItemsPageComponent).componentInstance as unknown as Harness;
    expect(api.items).toHaveBeenCalledTimes(1);
    c.filters.controls.search.setValue('Flour');
    expect(api.items).toHaveBeenCalledTimes(1);
    c.applyFilters();
    expect(api.items).toHaveBeenLastCalledWith(
      expect.objectContaining({ search: 'Flour', page: 0 }),
    );
    c.clearFilters();
    expect(c.filters.controls.search.value).toBe('');
    expect(api.items).toHaveBeenLastCalledWith({ page: 0, size: 10 });
  });
  it('uses barcode information without automatically saving', () => {
    const c = TestBed.createComponent(ItemsPageComponent).componentInstance as unknown as Harness;
    c.form.controls.barcode.setValue('789');
    c.lookup();
    expect(api.lookupProduct).toHaveBeenCalledWith('789');
    expect(api.saveItem).not.toHaveBeenCalled();
    c.useProduct();
    expect(c.form.controls.name.value).toBe('External Product');
  });
  it('prevents double submit while the first save is pending', () => {
    const pending = new Subject<Item>();
    api.saveItem.mockReturnValue(pending);
    const c = TestBed.createComponent(ItemsPageComponent).componentInstance as unknown as Harness;
    c.form.setValue({
      name: 'Flour',
      sku: 'FAR-001',
      barcode: '',
      type: 'RAW_MATERIAL',
      unit: 'KG',
      minimumStock: 1,
    });
    c.save();
    c.save();
    expect(api.saveItem).toHaveBeenCalledTimes(1);
  });
});
