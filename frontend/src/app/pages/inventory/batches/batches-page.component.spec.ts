import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { ConfirmationService, MessageService } from 'primeng/api';
import { InventoryService } from '../../../features/inventory/inventory.service';
import { BatchesPageComponent } from './batches-page.component';
describe('BatchesPageComponent', () => {
  interface Harness {
    filters: { controls: { code: { setValue(value: string): void } } };
    form: { patchValue(value: Record<string, string | Date>): void };
    applyFilters(): void;
    invalidDates(): boolean;
    save(): void;
  }
  it('validates dates and preserves filters until Filter is applied', async () => {
    const api = {
      items: vi.fn(() => of({ content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 })),
      batches: vi.fn(() => of({ content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 })),
      saveBatch: vi.fn(() => of({})),
    };
    await TestBed.configureTestingModule({
      imports: [BatchesPageComponent],
      providers: [
        provideNoopAnimations(),
        { provide: InventoryService, useValue: api },
        MessageService,
        ConfirmationService,
      ],
    }).compileComponents();
    const c = TestBed.createComponent(BatchesPageComponent).componentInstance as unknown as Harness;
    const initial = api.batches.mock.calls.length;
    c.filters.controls.code.setValue('LOT');
    expect(api.batches).toHaveBeenCalledTimes(initial);
    c.applyFilters();
    expect(api.batches).toHaveBeenLastCalledWith(expect.objectContaining({ code: 'LOT' }));
    c.form.patchValue({
      itemId: 'id',
      code: '001',
      manufacturingDate: new Date('2026-02-02'),
      expirationDate: new Date('2026-02-01'),
    });
    expect(c.invalidDates()).toBe(true);
    c.save();
    expect(api.saveBatch).not.toHaveBeenCalled();
  });
});
