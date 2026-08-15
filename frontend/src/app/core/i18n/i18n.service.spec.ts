import { TestBed } from '@angular/core/testing';
import { I18nService } from './i18n.service';
import { LocalizedDatePipe } from './localized-date.pipe';
import { LocalizedNumberPipe } from './localized-number.pipe';
import { LocalizedDateTimePipe } from './localized-date-time.pipe';
import { RoleLabelPipe } from './role-label.pipe';

describe('I18nService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.resetTestingModule();
  });

  it('uses pt-BR by default and translates enums', () => {
    const service = TestBed.inject(I18nService);
    expect(service.language()).toBe('pt-BR');
    expect(service.translate('enum.itemType.RAW_MATERIAL')).toBe('Matéria-prima');
  });

  it('changes language without reload and persists the preference', () => {
    const service = TestBed.inject(I18nService);
    service.setLanguage('en');
    expect(service.translate('nav.items')).toBe('Items');
    expect(localStorage.getItem('bakeflow.language')).toBe('en');
    service.setLanguage('pt-BR');
    expect(service.translate('nav.items')).toBe('Itens');
  });

  it('restores a valid saved preference', () => {
    localStorage.setItem('bakeflow.language', 'en');
    expect(TestBed.inject(I18nService).language()).toBe('en');
  });

  it('formats dates and numbers with the selected locale', () => {
    const service = TestBed.inject(I18nService);
    const date = TestBed.runInInjectionContext(() => new LocalizedDatePipe());
    const number = TestBed.runInInjectionContext(() => new LocalizedNumberPipe());
    expect(date.transform('2026-08-09')).toBe('09/08/2026');
    expect(number.transform(1250.5)).toBe('1.250,5');
    service.setLanguage('en');
    expect(date.transform('2026-08-09')).toBe('8/9/2026');
    expect(number.transform(1250.5)).toBe('1,250.5');
  });

  it('formats timestamps in the active locale and translates role labels centrally', () => {
    const service = TestBed.inject(I18nService);
    const dateTime = TestBed.runInInjectionContext(() => new LocalizedDateTimePipe());
    const role = TestBed.runInInjectionContext(() => new RoleLabelPipe());
    const timestamp = '2026-08-14T22:32:42Z';
    expect(dateTime.transform(timestamp)).toBe(
      new Intl.DateTimeFormat('pt-BR', {
        dateStyle: 'short',
        timeStyle: 'short',
      }).format(new Date(timestamp)),
    );
    expect(role.transform('ADMIN')).toBe('Administrador');
    service.setLanguage('en');
    expect(role.transform('VIEWER')).toBe('Viewer');
    expect(dateTime.transform(undefined)).toBe('Never');
  });

  it('does not translate persisted user data', () => {
    const service = TestBed.inject(I18nService);
    const itemName = 'Farinha de trigo';
    service.setLanguage('en');
    expect(itemName).toBe('Farinha de trigo');
  });
  it('translates production navigation and statuses in both languages', () => {
    const service = TestBed.inject(I18nService);
    expect(service.translate('nav.recipes')).toBe('Receitas');
    expect(service.translate('enum.productionStatus.IN_PROGRESS')).toBe('Em produção');
    service.setLanguage('en');
    expect(service.translate('nav.productionOrders')).toBe('Production Orders');
    expect(service.translate('enum.productionStatus.IN_PROGRESS')).toBe('In progress');
  });
});
