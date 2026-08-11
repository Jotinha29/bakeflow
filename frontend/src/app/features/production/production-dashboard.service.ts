import { HttpClient } from '@angular/common/http';import { inject,Injectable } from '@angular/core';import { ProductionOrder } from './production.models';
export interface ProductionSummary{planned:number;inProgress:number;completedToday:number;recent:ProductionOrder[];}
@Injectable({providedIn:'root'})export class ProductionDashboardService{private http=inject(HttpClient);summary(){return this.http.get<ProductionSummary>('/api/v1/production-dashboard');}}
