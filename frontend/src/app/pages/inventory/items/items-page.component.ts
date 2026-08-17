import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule, Validators } from '@angular/forms';
import { FormBuilder } from '@angular/forms';
import { finalize } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { SelectModule } from 'primeng/select';
import { TableModule, TablePageEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { InventoryService } from '../../../features/inventory/inventory.service';
import {
  Item,
  ItemInput,
  ItemType,
  ProductInformation,
  UnitOfMeasure,
} from '../../../features/inventory/inventory.models';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { LocalizedNumberPipe } from '../../../core/i18n/localized-number.pipe';
import { AuthService } from '../../../core/auth/auth.service';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';

@Component({
  selector: 'app-items-page',
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    TableModule,
    TagModule,
    TooltipModule,
    TranslatePipe,
    LocalizedNumberPipe,
    PageHeaderComponent,
  ],
  templateUrl: './items-page.component.html',
  styleUrl: '../inventory-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ItemsPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(InventoryService);
  private readonly messages = inject(MessageService);
  private readonly confirmations = inject(ConfirmationService);
  protected readonly i18n = inject(I18nService);
  protected readonly auth = inject(AuthService);
  protected readonly loading = signal(false);
  protected readonly submitting = signal(false);
  protected readonly lookupLoading = signal(false);
  protected readonly items = signal<Item[]>([]);
  protected readonly total = signal(0);
  protected readonly dialog = signal(false);
  protected readonly editing = signal<Item | undefined>(undefined);
  protected readonly product = signal<ProductInformation | undefined>(undefined);
  private applied: Record<string, string | number | boolean | undefined> = { page: 0, size: 10 };
  protected readonly types = computed(() =>
    ['RAW_MATERIAL', 'FINISHED_PRODUCT', 'PACKAGING', 'OTHER'].map((value) => ({
      label: this.i18n.translate(`enum.itemType.${value}`),
      value,
    })),
  );
  protected readonly units = computed(() =>
    ['UNIT', 'KG', 'G', 'L', 'ML'].map((value) => ({
      label: this.i18n.translate(`enum.unit.${value}`),
      value,
    })),
  );
  protected readonly statuses = computed(() => [
    { label: this.i18n.translate('common.active'), value: true },
    { label: this.i18n.translate('common.inactive'), value: false },
  ]);
  protected readonly filters = this.fb.group({
    search: [''],
    sku: [''],
    type: ['' as ItemType | ''],
    active: [undefined as boolean | undefined],
  });
  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(160)]],
    sku: ['', Validators.maxLength(80)],
    barcode: ['', Validators.maxLength(80)],
    type: ['RAW_MATERIAL' as ItemType, Validators.required],
    unit: ['KG' as UnitOfMeasure, Validators.required],
    minimumStock: [null as number | null, Validators.min(0)],
  });
  constructor() {
    this.load();
  }
  protected load(page = 0) {
    this.loading.set(true);
    this.applied = { ...this.applied, page };
    this.api
      .items(this.applied)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => {
          this.items.set(r.content);
          this.total.set(r.totalElements);
        },
        error: () => this.error(this.i18n.translate('items.loadError')),
      });
  }
  protected applyFilters() {
    const filters = this.filters.getRawValue();
    this.applied = {
      search: filters.search || undefined,
      sku: filters.sku || undefined,
      type: filters.type || undefined,
      active: filters.active ?? undefined,
      size: 10,
      page: 0,
    };
    this.load();
  }
  protected clearFilters() {
    this.filters.reset({ search: '', sku: '', type: '', active: undefined });
    this.applied = { page: 0, size: 10 };
    this.load();
  }
  protected page(event: TablePageEvent) {
    this.load((event.first ?? 0) / (event.rows ?? 10));
  }
  protected open(item?: Item) {
    this.editing.set(item);
    this.product.set(undefined);
    this.form.reset(
      item
        ? {
            name: item.name,
            sku: item.sku ?? '',
            barcode: item.barcode ?? '',
            type: item.type,
            unit: item.unit,
            minimumStock: item.minimumStock ?? null,
          }
        : { name: '', sku: '', barcode: '', type: 'RAW_MATERIAL', unit: 'KG', minimumStock: null },
    );
    this.dialog.set(true);
  }
  protected save() {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    const raw = this.form.getRawValue();
    const input: ItemInput = {
      name: raw.name!,
      sku: raw.sku || undefined,
      barcode: raw.barcode || undefined,
      type: raw.type!,
      unit: raw.unit!,
      minimumStock: raw.minimumStock ?? undefined,
    };
    this.api
      .saveItem(input, this.editing()?.id)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          this.dialog.set(false);
          this.messages.add({
            severity: 'success',
            summary: this.i18n.translate('common.success'),
            detail: this.i18n.translate(this.editing() ? 'items.updated' : 'items.created'),
          });
          this.load();
        },
        error: (e) => this.error(this.i18n.translateError(e, 'items.saveError')),
      });
  }
  protected toggle(item: Item) {
    this.confirmations.confirm({
      header: this.i18n.translate('items.confirmHeader', {
        action: this.i18n.translate(item.active ? 'common.deactivate' : 'common.activate'),
      }),
      message: this.i18n.translate('items.confirmMessage', {
        name: item.name,
        availability: this.i18n.translate(item.active ? 'items.unavailable' : 'items.available'),
      }),
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: this.i18n.translate(item.active ? 'common.deactivate' : 'common.activate'),
      accept: () =>
        this.api.setItemActive(item.id, !item.active).subscribe({
          next: () => {
            this.messages.add({
              severity: 'success',
              summary: this.i18n.translate('common.success'),
              detail: this.i18n.translate(item.active ? 'items.deactivated' : 'items.activated'),
            });
            this.load();
          },
          error: () => this.error(this.i18n.translate('items.statusError')),
        }),
    });
  }
  protected lookup() {
    const barcode = this.form.controls.barcode.value;
    if (!barcode || this.lookupLoading()) return;
    this.lookupLoading.set(true);
    this.product.set(undefined);
    this.api
      .lookupProduct(barcode)
      .pipe(finalize(() => this.lookupLoading.set(false)))
      .subscribe({
        next: (p) => this.product.set(p),
        error: () =>
          this.product.set({
            status: 'UNAVAILABLE',
            barcode,
            categories: [],
            message: this.i18n.translate('items.lookupError'),
          }),
      });
  }
  protected useProduct() {
    const p = this.product();
    if (p?.name) this.form.controls.name.setValue(p.name);
  }
  protected label(v: string) {
    return this.i18n.translate(`enum.itemType.${v}`);
  }
  private error(detail: string) {
    this.messages.add({ severity: 'error', summary: this.i18n.translate('common.error'), detail });
  }
}
