import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import {
  Batch,
  BatchInput,
  Item,
  ItemInput,
  Location,
  LocationInput,
  PageResult,
  ProductInformation,
  StockBalance,
  StockMovement,
  StockOperation,
} from './inventory.models';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1';
  items(filters: Record<string, string | number | boolean | null | undefined>) {
    return this.http.get<PageResult<Item>>(`${this.base}/items`, { params: this.params(filters) });
  }
  saveItem(input: ItemInput, id?: string) {
    return id
      ? this.http.put<Item>(`${this.base}/items/${id}`, input)
      : this.http.post<Item>(`${this.base}/items`, input);
  }
  setItemActive(id: string, active: boolean) {
    return this.http.patch<Item>(
      `${this.base}/items/${id}/${active ? 'activate' : 'deactivate'}`,
      {},
    );
  }
  lookupProduct(barcode: string) {
    return this.http.get<ProductInformation>(
      `${this.base}/product-information/barcode/${encodeURIComponent(barcode)}`,
    );
  }
  batches(filters: Record<string, string | number | boolean | null | undefined>) {
    return this.http.get<PageResult<Batch>>(`${this.base}/batches`, {
      params: this.params(filters),
    });
  }
  saveBatch(input: BatchInput, id?: string) {
    return id
      ? this.http.put<Batch>(`${this.base}/batches/${id}`, input)
      : this.http.post<Batch>(`${this.base}/batches`, input);
  }
  setBatchActive(id: string, active: boolean) {
    return this.http.patch<Batch>(
      `${this.base}/batches/${id}/${active ? 'activate' : 'deactivate'}`,
      {},
    );
  }
  locations(filters: Record<string, string | number | boolean | null | undefined>) {
    return this.http.get<PageResult<Location>>(`${this.base}/locations`, {
      params: this.params(filters),
    });
  }
  locationTree() {
    return this.http.get<Location[]>(`${this.base}/locations/tree`);
  }
  saveLocation(input: LocationInput, id?: string) {
    return id
      ? this.http.put<Location>(`${this.base}/locations/${id}`, input)
      : this.http.post<Location>(`${this.base}/locations`, input);
  }
  setLocationActive(id: string, active: boolean) {
    return this.http.patch<Location>(
      `${this.base}/locations/${id}/${active ? 'activate' : 'deactivate'}`,
      {},
    );
  }
  stockBalances(filters: Record<string, string | number | boolean | null | undefined>) {
    return this.http.get<PageResult<StockBalance>>(`${this.base}/stock/balances`, { params: this.params(filters) });
  }
  stockMovements(filters: Record<string, string | number | boolean | null | undefined>) {
    return this.http.get<PageResult<StockMovement>>(`${this.base}/stock/movements`, { params: this.params(filters) });
  }
  stockMovement(id: string) { return this.http.get<StockMovement>(`${this.base}/stock/movements/${id}`); }
  stockOperation(kind: 'entries' | 'exits' | 'transfers' | 'losses' | 'adjustments', input: StockOperation) {
    return this.http.post<StockMovement>(`${this.base}/stock/${kind}`, input);
  }
  private params(values: Record<string, string | number | boolean | null | undefined>) {
    let params = new HttpParams();
    Object.entries(values).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') params = params.set(key, value);
    });
    return params;
  }
}
