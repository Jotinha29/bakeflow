import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LocalizedNumberPipe } from '../../../../core/i18n/localized-number.pipe';
import { UnitOfMeasure } from '../../inventory.models';
@Component({selector:'app-stock-quantity-display',imports:[LocalizedNumberPipe],template:`<span class="quantity">{{quantity()|localizedNumber}} <span>{{unitLabel()}}</span></span>`,styles:[`.quantity{font-variant-numeric:tabular-nums;font-weight:700;white-space:nowrap}.quantity span{color:var(--bf-text-muted);font-size:.82em;font-weight:650}`],changeDetection:ChangeDetectionStrategy.OnPush})
export class StockQuantityDisplayComponent {readonly quantity=input.required<number>();readonly unit=input.required<UnitOfMeasure>();readonly unitLabel=computed(()=>({UNIT:'un',KG:'kg',G:'g',L:'l',ML:'ml'}[this.unit()]));}
