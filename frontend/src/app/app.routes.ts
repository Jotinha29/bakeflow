import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/dashboard/dashboard-page.component').then((m) => m.DashboardPageComponent),
    title: 'BakeFlow',
  },
  { path: 'inventory/items', loadComponent: () => import('./pages/inventory/items/items-page.component').then((m) => m.ItemsPageComponent), title: 'Items | BakeFlow' },
  { path: 'inventory/batches', loadComponent: () => import('./pages/inventory/batches/batches-page.component').then((m) => m.BatchesPageComponent), title: 'Batches | BakeFlow' },
  { path: 'inventory/locations', loadComponent: () => import('./pages/inventory/locations/locations-page.component').then((m) => m.LocationsPageComponent), title: 'Locations | BakeFlow' },
  { path: '**', redirectTo: '' },
];
