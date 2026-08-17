import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';
import { I18nService } from '../../../core/i18n/i18n.service';
import { LocalizedDatePipe } from '../../../core/i18n/localized-date.pipe';
import { LocalizedNumberPipe } from '../../../core/i18n/localized-number.pipe';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { InventoryService } from '../../../features/inventory/inventory.service';
import { Location } from '../../../features/inventory/inventory.models';
import {
  ProductionOrder,
  ProductionPreview,
  ProductionStatus,
  Recipe,
} from '../../../features/production/production.models';
import { ProductionService } from '../../../features/production/production.service';
import { AuthService } from '../../../core/auth/auth.service';
@Component({
  selector: 'app-production-orders-page',
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputNumberModule,
    InputTextModule,
    SelectModule,
    TableModule,
    TagModule,
    TextareaModule,
    TooltipModule,
    TranslatePipe,
    PageHeaderComponent,
    LocalizedDatePipe,
    LocalizedNumberPipe,
  ],
  templateUrl: './production-orders-page.component.html',
  styleUrls: ['../../inventory/inventory-page.scss', '../production-page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductionOrdersPageComponent {
  private fb = inject(FormBuilder);
  private api = inject(ProductionService);
  private inventory = inject(InventoryService);
  private messages = inject(MessageService);
  private confirmations = inject(ConfirmationService);
  protected i18n = inject(I18nService);
  protected auth = inject(AuthService);
  protected loading = signal(false);
  protected submitting = signal(false);
  protected planPreview = signal<ProductionPreview | undefined>(undefined);
  protected orders = signal<ProductionOrder[]>([]);
  protected recipes = signal<Recipe[]>([]);
  protected locations = signal<Location[]>([]);
  protected createDialog = signal(false);
  protected detailDialog = signal(false);
  protected completeDialog = signal(false);
  protected selected = signal<ProductionOrder | undefined>(undefined);
  protected statuses = computed(() =>
    ['PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'].map((value) => ({
      label: this.i18n.translate(`enum.productionStatus.${value}`),
      value,
    })),
  );
  protected filters = this.fb.group({
    code: [''],
    recipeId: [''],
    status: ['' as ProductionStatus | ''],
    plannedDate: [''],
  });
  protected createForm = this.fb.group({
    recipeId: ['', Validators.required],
    plannedQuantity: [null as number | null, [Validators.required, Validators.min(0.001)]],
    plannedDate: [new Date().toISOString().slice(0, 10), Validators.required],
    notes: [''],
  });
  protected completeForm = this.fb.group({
    actualQuantity: [null as number | null, [Validators.required, Validators.min(0.001)]],
    destinationLocationId: ['', Validators.required],
    differenceReason: [''],
    notes: [''],
  });
  constructor() {
    this.load();
  }
  protected load() {
    this.loading.set(true);
    forkJoin({
      orders: this.api.orders(this.filters.getRawValue() as Record<string, string | undefined>),
      recipes: this.api.recipes({ active: true }),
      locations: this.inventory.locationTree(),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => {
          this.orders.set(r.orders);
          this.recipes.set(r.recipes);
          this.locations.set(this.flatten(r.locations).filter((l) => l.active));
        },
        error: () => this.error('production.loadError'),
      });
  }
  protected clear() {
    this.filters.reset({ code: '', recipeId: '', status: '', plannedDate: '' });
    this.load();
  }
  protected openCreate() {
    this.planPreview.set(undefined);
    this.createForm.reset({
      recipeId: '',
      plannedQuantity: null,
      plannedDate: new Date().toISOString().slice(0, 10),
      notes: '',
    });
    this.createDialog.set(true);
  }
  protected previewPlan() {
    const f = this.createForm.getRawValue();
    if (!f.recipeId || !f.plannedQuantity) return;
    this.loading.set(true);
    this.api
      .preview(f.recipeId, f.plannedQuantity)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (v) => this.planPreview.set(v),
        error: () => this.error('orders.previewError'),
      });
  }
  protected create() {
    if (this.createForm.invalid || this.submitting()) return;
    this.submitting.set(true);
    const f = this.createForm.getRawValue();
    this.api
      .createOrder({
        recipeId: f.recipeId!,
        plannedQuantity: f.plannedQuantity!,
        plannedDate: f.plannedDate!,
        notes: f.notes || undefined,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (o) => {
          this.createDialog.set(false);
          this.success('orders.created');
          this.openDetails(o);
          this.load();
        },
        error: () => this.error('orders.saveError'),
      });
  }
  protected openDetails(order: ProductionOrder) {
    this.loading.set(true);
    this.api
      .order(order.id)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (o) => {
          this.selected.set(o);
          this.detailDialog.set(true);
        },
        error: () => this.error('production.loadError'),
      });
  }
  protected start() {
    const order = this.selected();
    if (!order || this.submitting()) return;
    this.confirmations.confirm({
      header: this.i18n.translate('orders.start'),
      message: this.i18n.translate('orders.startConfirm'),
      accept: () => this.run(() => this.api.start(order.id), 'orders.started'),
    });
  }
  protected cancel() {
    const order = this.selected();
    if (!order || this.submitting()) return;
    this.confirmations.confirm({
      header: this.i18n.translate('orders.cancel'),
      message: this.i18n.translate('orders.cancelConfirm'),
      accept: () => this.run(() => this.api.cancel(order.id), 'orders.cancelled'),
    });
  }
  protected openComplete() {
    const o = this.selected();
    this.completeForm.reset({
      actualQuantity: o?.plannedQuantity ?? null,
      destinationLocationId: '',
      differenceReason: '',
      notes: '',
    });
    this.completeDialog.set(true);
  }
  protected complete() {
    const order = this.selected();
    if (!order || this.completeForm.invalid || this.submitting()) return;
    const f = this.completeForm.getRawValue();
    this.submitting.set(true);
    this.api
      .complete(order.id, {
        actualQuantity: f.actualQuantity!,
        destinationLocationId: f.destinationLocationId!,
        differenceReason: f.differenceReason || undefined,
        notes: f.notes || undefined,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (o) => {
          this.completeDialog.set(false);
          this.selected.set(o);
          this.success('orders.completed');
          this.load();
        },
        error: () => this.error('orders.completeError'),
      });
  }
  protected severity(status: ProductionStatus) {
    return status === 'COMPLETED'
      ? 'success'
      : status === 'IN_PROGRESS'
        ? 'warn'
        : status === 'CANCELLED'
          ? 'danger'
          : 'info';
  }
  private run(call: () => ReturnType<ProductionService['start']>, key: string) {
    this.submitting.set(true);
    call()
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (o) => {
          this.selected.set(o);
          this.success(key);
          this.load();
        },
        error: () => this.error('orders.transitionError'),
      });
  }
  private flatten(values: Location[]): Location[] {
    return values.flatMap((v) => [v, ...this.flatten(v.children ?? [])]);
  }
  private success(key: string) {
    this.messages.add({
      severity: 'success',
      summary: this.i18n.translate('common.success'),
      detail: this.i18n.translate(key),
    });
  }
  private error(key: string) {
    this.messages.add({
      severity: 'error',
      summary: this.i18n.translate('common.error'),
      detail: this.i18n.translate(key),
    });
  }
}
