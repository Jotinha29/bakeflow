import { Routes } from '@angular/router';
import { authGuard, permissionGuard } from './core/auth/auth.guard';
export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/auth/login-page.component').then((m) => m.LoginPageComponent),
    title: 'Login | BakeFlow',
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/dashboard/dashboard-page.component').then((m) => m.DashboardPageComponent),
    title: 'BakeFlow',
  },
  {
    path: 'inventory/stock',
    canActivate: [authGuard, permissionGuard('STOCK_READ')],
    loadComponent: () => import('./pages/inventory/stock/stock-page.component').then((m) => m.StockPageComponent),
    title: 'Stock | BakeFlow',
  },
  {
    path: 'inventory/movements',
    canActivate: [authGuard, permissionGuard('MOVEMENT_READ')],
    loadComponent: () => import('./pages/inventory/movements/movements-page.component').then((m) => m.MovementsPageComponent),
    title: 'Stock Movements | BakeFlow',
  },
  {
    path: 'inventory/items',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/inventory/items/items-page.component').then((m) => m.ItemsPageComponent),
    title: 'Items | BakeFlow',
  },
  {
    path: 'inventory/batches',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/inventory/batches/batches-page.component').then(
        (m) => m.BatchesPageComponent,
      ),
    title: 'Batches | BakeFlow',
  },
  {
    path: 'inventory/locations',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/inventory/locations/locations-page.component').then(
        (m) => m.LocationsPageComponent,
      ),
    title: 'Locations | BakeFlow',
  },
  {
    path: 'recipes',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/production/recipes/recipes-page.component').then(
        (m) => m.RecipesPageComponent,
      ),
    title: 'Recipes | BakeFlow',
  },
  {
    path: 'production-orders',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/production/orders/production-orders-page.component').then(
        (m) => m.ProductionOrdersPageComponent,
      ),
    title: 'Production Orders | BakeFlow',
  },
  {
    path: 'integrations',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/integrations/integrations-page.component').then(
        (m) => m.IntegrationsPageComponent,
      ),
    title: 'Integrations | BakeFlow',
  },
  {
    path: 'users',
    canActivate: [authGuard, permissionGuard('USER_READ')],
    loadComponent: () =>
      import('./pages/users/users-page.component').then((m) => m.UsersPageComponent),
    title: 'Users | BakeFlow',
  },
  {
    path: 'account',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/account/account-page.component').then((m) => m.AccountPageComponent),
    title: 'Account | BakeFlow',
  },
  { path: '**', redirectTo: '' },
];
