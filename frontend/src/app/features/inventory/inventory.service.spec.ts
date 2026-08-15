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
});
