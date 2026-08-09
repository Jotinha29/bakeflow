import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
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
  protected readonly loading = signal(false);
  protected readonly submitting = signal(false);
  protected readonly lookupLoading = signal(false);
  protected readonly items = signal<Item[]>([]);
  protected readonly total = signal(0);
  protected readonly dialog = signal(false);
  protected readonly editing = signal<Item | undefined>(undefined);
  protected readonly product = signal<ProductInformation | undefined>(undefined);
  private applied: Record<string, string | number | boolean | undefined> = { page: 0, size: 10 };
  protected readonly types = ['RAW_MATERIAL', 'FINISHED_PRODUCT', 'PACKAGING', 'OTHER'].map(
    (value) => ({ label: this.label(value), value }),
  );
  protected readonly units = ['UNIT', 'KG', 'G', 'L', 'ML'].map((value) => ({
    label: value,
    value,
  }));
  protected readonly statuses = [
    { label: 'Active', value: true },
    { label: 'Inactive', value: false },
  ];
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
        error: () => this.error('Could not load items.'),
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
            summary: 'Success',
            detail: `Item ${this.editing() ? 'updated' : 'created'} successfully.`,
          });
          this.load();
        },
        error: (e) => this.error(e.error?.message ?? 'Could not save item.'),
      });
  }
  protected toggle(item: Item) {
    this.confirmations.confirm({
      header: `${item.active ? 'Deactivate' : 'Activate'} item?`,
      message: `${item.name} will ${item.active ? 'no longer' : 'again'} be available for new operations.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: item.active ? 'Deactivate' : 'Activate',
      accept: () =>
        this.api.setItemActive(item.id, !item.active).subscribe({
          next: () => {
            this.messages.add({
              severity: 'success',
              summary: 'Success',
              detail: `Item ${item.active ? 'deactivated' : 'activated'} successfully.`,
            });
            this.load();
          },
          error: () => this.error('Could not update item status.'),
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
            message: 'External product information is unavailable at the moment.',
          }),
      });
  }
  protected useProduct() {
    const p = this.product();
    if (p?.name) this.form.controls.name.setValue(p.name);
  }
  protected label(v: string) {
    return v
      .toLowerCase()
      .replaceAll('_', ' ')
      .replace(/\b\w/g, (c) => c.toUpperCase());
  }
  private error(detail: string) {
    this.messages.add({ severity: 'error', summary: 'Something went wrong', detail });
  }
}
