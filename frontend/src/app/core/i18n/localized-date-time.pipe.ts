import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from './i18n.service';

@Pipe({ name: 'localizedDateTime', standalone: true, pure: false })
export class LocalizedDateTimePipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(value?: string | Date): string {
    if (!value) return this.i18n.translate('common.never');
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return this.i18n.translate('common.never');
    return new Intl.DateTimeFormat(this.i18n.locale(), {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(date);
  }
}
