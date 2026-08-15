import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import {
  CompleteInput,
  OrderInput,
  ProductionOrder,
  ProductionPreview,
  Recipe,
  RecipeInput,
} from './production.models';
@Injectable({ providedIn: 'root' })
export class ProductionService {
  private http = inject(HttpClient);
  private base = '/api/v1';
  recipes(filters: Record<string, string | boolean | null | undefined> = {}) {
    return this.http.get<Recipe[]>(`${this.base}/recipes`, { params: this.params(filters) });
  }
  saveRecipe(input: RecipeInput, id?: string) {
    return id
      ? this.http.put<Recipe>(`${this.base}/recipes/${id}`, input)
      : this.http.post<Recipe>(`${this.base}/recipes`, input);
  }
  setRecipeActive(id: string, active: boolean) {
    return this.http.patch<void>(
      `${this.base}/recipes/${id}/${active ? 'activate' : 'deactivate'}`,
      {},
    );
  }
  orders(filters: Record<string, string | undefined> = {}) {
    return this.http.get<ProductionOrder[]>(`${this.base}/production-orders`, {
      params: this.params(filters),
    });
  }
  order(id: string) {
    return this.http.get<ProductionOrder>(`${this.base}/production-orders/${id}`);
  }
  createOrder(input: OrderInput) {
    return this.http.post<ProductionOrder>(`${this.base}/production-orders`, input);
  }
  preview(recipeId: string, plannedQuantity: number) {
    return this.http.post<ProductionPreview>(`${this.base}/production-orders/preview`, {
      recipeId,
      plannedQuantity,
    });
  }
  start(id: string) {
    return this.http.post<ProductionOrder>(`${this.base}/production-orders/${id}/start`, {});
  }
  complete(id: string, input: CompleteInput) {
    return this.http.post<ProductionOrder>(`${this.base}/production-orders/${id}/complete`, input);
  }
  cancel(id: string) {
    return this.http.post<ProductionOrder>(`${this.base}/production-orders/${id}/cancel`, {});
  }
  private params(values: Record<string, string | boolean | null | undefined>) {
    let p = new HttpParams();
    Object.entries(values).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') p = p.set(k, v);
    });
    return p;
  }
}
