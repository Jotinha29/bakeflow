import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from './i18n.service';

@Pipe({ name: 'roleLabel', standalone: true, pure: false })
export class RoleLabelPipe implements PipeTransform {
  private readonly i18n = inject(I18nService);
  transform(role?: string): string {
    return role ? this.i18n.translate(`common.roles.${role.toLowerCase()}`) : '';
  }
}
