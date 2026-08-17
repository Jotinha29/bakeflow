import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule, TablePageEvent } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../../core/auth/auth.service';
import { LocalizedDatePipe } from '../../../core/i18n/localized-date.pipe';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { InventoryService } from '../../../features/inventory/inventory.service';
import { Batch, Item, Location, StockBalance, StockOperation } from '../../../features/inventory/inventory.models';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { FilterPanelComponent } from '../../../shared/ui/filter-panel/filter-panel.component';
import { TableContainerComponent } from '../../../shared/ui/table-container/table-container.component';
import { TableActionsComponent } from '../../../shared/ui/table-actions/table-actions.component';
import { EmptyStateComponent } from '../../../shared/ui/empty-state/empty-state.component';
import { StockQuantityDisplayComponent } from '../../../features/inventory/components/stock-quantity-display/stock-quantity-display.component';
import { I18nService } from '../../../core/i18n/i18n.service';

type Operation = 'entries' | 'exits' | 'transfers' | 'losses' | 'adjustments';
@Component({
  selector: 'app-stock-page',
  imports: [ReactiveFormsModule, ButtonModule, DialogModule, InputNumberModule, InputTextModule, SelectModule, TableModule, TooltipModule, TranslatePipe, LocalizedDatePipe, PageHeaderComponent, FilterPanelComponent, TableContainerComponent, TableActionsComponent, EmptyStateComponent, StockQuantityDisplayComponent],
  templateUrl: './stock-page.component.html',
  styleUrl: '../inventory-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StockPageComponent {
  private fb = inject(FormBuilder); private api = inject(InventoryService); private messages = inject(MessageService);
  protected i18n = inject(I18nService);
  protected auth = inject(AuthService); protected balances = signal<StockBalance[]>([]); protected items = signal<Item[]>([]);
  protected batches = signal<Batch[]>([]); protected locations = signal<Location[]>([]); protected total = signal(0);
  protected loading = signal(false); protected submitting = signal(false); protected dialog = signal(false); protected operation = signal<Operation>('entries');
  private applied: Record<string, string | number | undefined> = { page: 0, size: 10 };
  protected filters = this.fb.group({ sku: [''], batch: [''], locationId: [''], expiration: [''] });
  protected form = this.fb.group({ itemId: ['', Validators.required], batchId: ['', Validators.required], locationId: [''], sourceLocationId: [''], destinationLocationId: [''], quantity: [null as number | null], physicalQuantity: [null as number | null], reason: [''], notes: [''], justification: [''] });
  constructor() { forkJoin({ items: this.api.items({ active: true, page: 0, size: 100 }), batches: this.api.batches({ active: true, page: 0, size: 100 }), locations: this.api.locationTree() }).subscribe(({items,batches,locations}) => { this.items.set(items.content); this.batches.set(batches.content); this.locations.set(this.flatten(locations).filter(l => l.active)); }); this.load(); }
  protected load(page = 0) { this.loading.set(true); this.applied = {...this.applied,page}; this.api.stockBalances(this.applied).pipe(finalize(() => this.loading.set(false))).subscribe({next:r=>{this.balances.set(r.content);this.total.set(r.totalElements);},error:()=>this.error()}); }
  protected apply() { const f=this.filters.getRawValue(); this.applied={sku:f.sku||undefined,batch:f.batch||undefined,locationId:f.locationId||undefined,expiration:f.expiration||undefined,page:0,size:10}; this.load(); }
  protected clear() { this.filters.reset(); this.applied={page:0,size:10}; this.load(); }
  protected page(e: TablePageEvent) { this.load((e.first??0)/(e.rows??10)); }
  protected open(operation: Operation, balance?: StockBalance) { this.operation.set(operation); this.form.reset({itemId:balance?.itemId??'',batchId:balance?.batchId??'',locationId:balance?.locationId??'',sourceLocationId:balance?.locationId??'',destinationLocationId:'',quantity:null,physicalQuantity:balance?.quantity??null,reason:this.defaultReason(operation),notes:'',justification:''}); this.dialog.set(true); }
  protected save() { if (this.submitting()) return; const value=this.form.getRawValue(); const input: StockOperation={...value,itemId:value.itemId!,batchId:value.batchId!,locationId:value.locationId||undefined,sourceLocationId:value.sourceLocationId||undefined,destinationLocationId:value.destinationLocationId||undefined,quantity:value.quantity??undefined,physicalQuantity:value.physicalQuantity??undefined,reason:value.reason||undefined,notes:value.notes||undefined,justification:value.justification||undefined}; this.submitting.set(true); this.api.stockOperation(this.operation(),input).pipe(finalize(()=>this.submitting.set(false))).subscribe({next:()=>{this.dialog.set(false);this.messages.add({severity:'success',summary:'OK',detail:'Operação de estoque registrada.'});this.load();},error:()=>this.error()}); }
  protected can(permission:string){return this.auth.can(permission);} protected needsLocation(){return this.operation()!=='transfers';}
  protected operationLabel(){return this.i18n.translate('stock.operation.'+this.operation());}
  private defaultReason(op:Operation){return op==='entries'?'RECEIPT':op==='exits'?'INTERNAL_USE':op==='losses'?'DAMAGED':'';}
  private flatten(values:Location[]):Location[]{return values.flatMap(v=>[v,...this.flatten(v.children??[])]);}
  private error(){this.messages.add({severity:'error',summary:'Erro',detail:'Não foi possível concluir a operação.'});}
}
