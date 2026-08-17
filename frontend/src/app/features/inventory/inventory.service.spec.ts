import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { InventoryService } from './inventory.service';

describe('InventoryService', () => {
  let service: InventoryService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(InventoryService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('omits null and undefined filters while preserving false', () => {
    service.items({ active: null, page: undefined }).subscribe();
    expect(http.expectOne('/api/v1/items').request.params.keys()).toEqual([]);
    service.items({ active: false }).subscribe();
    expect(
      http.expectOne(
        (request) => request.url === '/api/v1/items' && request.params.get('active') === 'false',
      ),
    ).toBeTruthy();
  });

  it('uses the stock balance, history and operation endpoints', () => {
    service.stockBalances({ page: 0, sku: 'FLOUR' }).subscribe();
    expect(http.expectOne((r) => r.url === '/api/v1/stock/balances' && r.params.get('sku') === 'FLOUR').request.method).toBe('GET');
    service.stockMovements({ type: 'TRANSFER' }).subscribe();
    expect(http.expectOne((r) => r.url === '/api/v1/stock/movements' && r.params.get('type') === 'TRANSFER').request.method).toBe('GET');
    service.stockOperation('transfers', { itemId: 'item', batchId: 'batch', sourceLocationId: 'a', destinationLocationId: 'b', quantity: 2 }).subscribe();
    const operation = http.expectOne('/api/v1/stock/transfers');
    expect(operation.request.method).toBe('POST');
    expect(operation.request.body.quantity).toBe(2);
  });
});
