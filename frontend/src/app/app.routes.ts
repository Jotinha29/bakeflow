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
  { path: 'recipes', loadComponent: () => import('./pages/production/recipes/recipes-page.component').then((m) => m.RecipesPageComponent), title: 'Recipes | BakeFlow' },
  { path: 'production-orders', loadComponent: () => import('./pages/production/orders/production-orders-page.component').then((m) => m.ProductionOrdersPageComponent), title: 'Production Orders | BakeFlow' },
  { path: 'integrations', loadComponent: () => import('./pages/integrations/integrations-page.component').then((m) => m.IntegrationsPageComponent), title: 'Integrations | BakeFlow' },
  { path: '**', redirectTo: '' },
];
