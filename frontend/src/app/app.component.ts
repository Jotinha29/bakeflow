import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { SelectModule } from 'primeng/select';
import { PrimeNG } from 'primeng/config';
import { I18nService } from './core/i18n/i18n.service';
import { TranslatePipe } from './core/i18n/translate.pipe';
import { Language, primeTranslations } from './core/i18n/translations';
import { Translation } from 'primeng/api';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, ConfirmDialogModule, ToastModule, SelectModule, TranslatePipe, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  protected readonly i18n = inject(I18nService);
  protected readonly languages: { label: string; value: Language }[] = [
    { label: 'Português (Brasil)', value: 'pt-BR' }, { label: 'English', value: 'en' },
  ];
  private readonly prime = inject(PrimeNG);
  constructor() {
    effect(() => {
      this.prime.setTranslation(primeTranslations[this.i18n.language()] as Translation);
    });
  }
  protected changeLanguage(language: Language): void { this.i18n.setLanguage(language); }
}
