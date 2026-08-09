import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule, TablePageEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { InventoryService } from '../../../features/inventory/inventory.service';
import { Batch, BatchInput, Item } from '../../../features/inventory/inventory.models';
@Component({
  selector: 'app-batches-page',
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    DatePickerModule,
    DialogModule,
    InputTextModule,
    SelectModule,
    TableModule,
    TagModule,
    TooltipModule,
  ],
  templateUrl: './batches-page.component.html',
  styleUrl: '../inventory-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BatchesPageComponent {
  private fb = inject(FormBuilder);
  private api = inject(InventoryService);
  private messages = inject(MessageService);
  private confirmations = inject(ConfirmationService);
  protected loading = signal(false);
  protected submitting = signal(false);
  protected batches = signal<Batch[]>([]);
  protected items = signal<Item[]>([]);
  protected total = signal(0);
  protected dialog = signal(false);
  protected editing = signal<Batch | undefined>(undefined);
  private applied: Record<string, string | number | boolean | undefined> = { page: 0, size: 10 };
  protected statuses = [
    { label: 'Active', value: true },
    { label: 'Inactive', value: false },
  ];
  protected filters = this.fb.group({
    itemId: [''],
    code: [''],
    active: [undefined as boolean | undefined],
    expirationFrom: [null as Date | null],
    expirationTo: [null as Date | null],
  });
  protected form = this.fb.group({
    itemId: ['', Validators.required],
    code: ['', Validators.required],
    manufacturingDate: [null as Date | null],
    expirationDate: [null as Date | null],
  });
  constructor() {
    this.api
      .items({ active: true, size: 100, page: 0 })
      .subscribe((r) => this.items.set(r.content));
    this.load();
  }
  protected load(page = 0) {
    this.loading.set(true);
    this.applied = { ...this.applied, page };
    this.api
      .batches(this.applied)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => {
          this.batches.set(r.content);
          this.total.set(r.totalElements);
        },
        error: () => this.error('Could not load batches.'),
      });
  }
  protected applyFilters() {
    const f = this.filters.getRawValue();
    this.applied = {
      itemId: f.itemId || undefined,
      code: f.code || undefined,
      active: f.active ?? undefined,
      expirationFrom: this.date(f.expirationFrom),
      expirationTo: this.date(f.expirationTo),
      page: 0,
      size: 10,
    };
    this.load();
  }
  protected clearFilters() {
    this.filters.reset({
      itemId: '',
      code: '',
      active: undefined,
      expirationFrom: null,
      expirationTo: null,
    });
    this.applied = { page: 0, size: 10 };
    this.load();
  }
  protected page(e: TablePageEvent) {
    this.load((e.first ?? 0) / (e.rows ?? 10));
  }
  protected open(batch?: Batch) {
    this.editing.set(batch);
    this.form.reset(
      batch
        ? {
            itemId: batch.itemId,
            code: batch.code,
            manufacturingDate: batch.manufacturingDate
              ? new Date(batch.manufacturingDate + 'T00:00:00')
              : null,
            expirationDate: batch.expirationDate
              ? new Date(batch.expirationDate + 'T00:00:00')
              : null,
          }
        : { itemId: '', code: '', manufacturingDate: null, expirationDate: null },
    );
    this.dialog.set(true);
  }
  protected invalidDates() {
    const f = this.form.getRawValue();
    return !!(f.manufacturingDate && f.expirationDate && f.expirationDate < f.manufacturingDate);
  }
  protected save() {
    if (this.form.invalid || this.invalidDates() || this.submitting()) return;
    this.submitting.set(true);
    const f = this.form.getRawValue();
    const input: BatchInput = {
      itemId: f.itemId!,
      code: f.code!,
      manufacturingDate: this.date(f.manufacturingDate),
      expirationDate: this.date(f.expirationDate),
    };
    this.api
      .saveBatch(input, this.editing()?.id)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          this.dialog.set(false);
          this.messages.add({
            severity: 'success',
            summary: 'Success',
            detail: `Batch ${this.editing() ? 'updated' : 'created'} successfully.`,
          });
          this.load();
        },
        error: (e) => this.error(e.error?.message ?? 'Could not save batch.'),
      });
  }
  protected toggle(b: Batch) {
    this.confirmations.confirm({
      header: `${b.active ? 'Deactivate' : 'Activate'} batch?`,
      message: `Batch ${b.code} will be ${b.active ? 'unavailable' : 'available'} for new operations.`,
      accept: () =>
        this.api.setBatchActive(b.id, !b.active).subscribe({
          next: () => {
            this.messages.add({
              severity: 'success',
              summary: 'Success',
              detail: `Batch ${b.active ? 'deactivated' : 'activated'} successfully.`,
            });
            this.load();
          },
          error: () => this.error('Could not update batch status.'),
        }),
    });
  }
  private date(d: Date | null | undefined) {
    return d
      ? `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
      : undefined;
  }
  private error(detail: string) {
    this.messages.add({ severity: 'error', summary: 'Something went wrong', detail });
  }
}
