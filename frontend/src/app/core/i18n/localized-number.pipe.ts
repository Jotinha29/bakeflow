import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from './i18n.service';

@Pipe({ name: 'localizedNumber', standalone: true, pure: false })
export class LocalizedNumberPipe implements PipeTransform {
  private readonly i18n = inject(I18nService);
  transform(value?: number | null, maximumFractionDigits = 3): string {
    return value == null ? '—' : new Intl.NumberFormat(this.i18n.locale(), { maximumFractionDigits }).format(value);
  }
}
