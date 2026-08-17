import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  template: `<header class="bf-page-header"><div class="bf-page-header__copy"><span class="eyebrow">{{ eyebrow() }}</span><h1>{{ title() }}</h1>@if(description()){<p>{{ description() }}</p>}<ng-content select="[pageHeaderContent]" /></div><div class="bf-page-header__actions"><ng-content /></div></header>`,
  styleUrl: './page-header.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageHeaderComponent { readonly eyebrow=input.required<string>(); readonly title=input.required<string>(); readonly description=input<string>(); }
