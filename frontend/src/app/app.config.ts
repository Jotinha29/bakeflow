import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './core/auth/auth.interceptor';
import { providePrimeNG } from 'primeng/config';
import Aura from '@primeuix/themes/aura';
import { definePreset } from '@primeuix/themes';
import { ConfirmationService, MessageService } from 'primeng/api';

import { routes } from './app.routes';

const BakeFlowPreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#eef8f2',
      100: '#d8eee0',
      200: '#b4ddc4',
      300: '#84c5a0',
      400: '#52a97a',
      500: '#26734d',
      600: '#1f6040',
      700: '#1d4d36',
      800: '#1a3e2e',
      900: '#173429',
      950: '#0b1d16',
    },
    colorScheme: {
      light: {
        surface: {
          0: '#fffdf9',
          50: '#faf7f1',
          100: '#f5f2eb',
          200: '#e9e4da',
          300: '#ddd8ce',
          400: '#aaa9a2',
          500: '#777c77',
          600: '#5e6a65',
          700: '#43504b',
          800: '#2b3733',
          900: '#17201d',
          950: '#0e1512',
        },
      },
    },
  },
  components: {
    button: { root: { borderRadius: '0.75rem' } },
    inputtext: { root: { borderRadius: '0.625rem' } },
    select: { root: { borderRadius: '0.625rem' } },
    multiselect: { root: { borderRadius: '0.625rem' } },
    dialog: { root: { borderRadius: '1rem' } },
  },
});

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    providePrimeNG({ theme: { preset: BakeFlowPreset, options: { darkModeSelector: false } } }),
    ConfirmationService,
    MessageService,
  ],
};
