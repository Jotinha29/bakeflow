import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from './i18n.service';

@Pipe({ name: 'localizedDate', standalone: true, pure: false })
export class LocalizedDatePipe implements PipeTransform {
  private readonly i18n = inject(I18nService);
  transform(value?: string | Date): string {
    if (!value) return '—';
    const date = typeof value === 'string' ? new Date(`${value}T00:00:00`) : value;
    return new Intl.DateTimeFormat(this.i18n.locale()).format(date);
  }
}
