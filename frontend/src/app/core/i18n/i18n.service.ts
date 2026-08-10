import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Language, TranslationParams, translations } from './translations';

const STORAGE_KEY = 'bakeflow.language';

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly document = inject(DOCUMENT);
  private readonly current = signal<Language>(this.restore());
  readonly language = this.current.asReadonly();
  readonly locale = computed(() => (this.current() === 'en' ? 'en-US' : 'pt-BR'));
  readonly dateFormat = computed(() => (this.current() === 'en' ? 'mm/dd/yy' : 'dd/mm/yy'));

  constructor() { this.applyDocumentLanguage(); }

  setLanguage(language: Language): void {
    this.current.set(language);
    this.applyDocumentLanguage();
    try { globalThis.localStorage?.setItem(STORAGE_KEY, language); } catch { /* Storage may be unavailable. */ }
  }

  translate(key: string, params: TranslationParams = {}): string {
    const value = translations[this.current()][key] ?? translations['pt-BR'][key] ?? key;
    return Object.entries(params).reduce((text, [name, replacement]) =>
      text.replaceAll(`{{${name}}}`, String(replacement)), value);
  }

  translateError(value: unknown, fallbackKey: string): string {
    const code = typeof value === 'object' && value !== null && 'error' in value
      ? (value as { error?: { code?: string; message?: string } }).error
      : undefined;
    return code?.code ? this.translate(`error.${code.code}`) : code?.message ?? this.translate(fallbackKey);
  }

  private restore(): Language {
    try { return globalThis.localStorage?.getItem(STORAGE_KEY) === 'en' ? 'en' : 'pt-BR'; }
    catch { return 'pt-BR'; }
  }
  private applyDocumentLanguage(): void { this.document.documentElement.lang = this.current(); }
}
