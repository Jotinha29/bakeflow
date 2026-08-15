import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ExternalCompany, ExternalProduct, IntegrationStatus } from './integration.models';
@Injectable({ providedIn: 'root' })
export class IntegrationService {
  private http = inject(HttpClient);
  private base = '/api/v1/integrations';
  status() {
    return this.http.get<IntegrationStatus>('/api/v1/system/integrations');
  }
  product(barcode: string) {
    return this.http.get<ExternalProduct>(`${this.base}/product/${encodeURIComponent(barcode)}`);
  }
  company(cnpj: string) {
    return this.http.get<ExternalCompany>(`${this.base}/company/${encodeURIComponent(cnpj)}`);
  }
}
