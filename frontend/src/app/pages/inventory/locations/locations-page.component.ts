import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ConfirmationService, MessageService, TreeNode } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TreeModule } from 'primeng/tree';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { InventoryService } from '../../../features/inventory/inventory.service';
import {
  Location,
  LocationInput,
  LocationType,
} from '../../../features/inventory/inventory.models';
import { I18nService } from '../../../core/i18n/i18n.service';
import { TranslatePipe } from '../../../core/i18n/translate.pipe';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header.component';
import { AuthService } from '../../../core/auth/auth.service';
@Component({
  selector: 'app-locations-page',
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    SelectModule,
    TreeModule,
    TagModule,
    TooltipModule,
    TranslatePipe,
    PageHeaderComponent,
  ],
  templateUrl: './locations-page.component.html',
  styleUrls: ['../inventory-page.scss', './locations-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LocationsPageComponent {
  protected auth = inject(AuthService);
  private fb = inject(FormBuilder);
  private api = inject(InventoryService);
  private messages = inject(MessageService);
  private confirmations = inject(ConfirmationService);
  protected i18n = inject(I18nService);
  protected loading = signal(false);
  protected submitting = signal(false);
  protected nodes = signal<TreeNode<Location>[]>([]);
  protected allLocations = signal<Location[]>([]);
  protected dialog = signal(false);
  protected editing = signal<Location | undefined>(undefined);
  protected types = computed(() =>
    [
      'WAREHOUSE',
      'ROOM',
      'AISLE',
      'SHELF',
      'PALLET',
      'PRODUCTION_AREA',
      'COLD_STORAGE',
      'OTHER',
    ].map((value) => ({ label: this.i18n.translate(`enum.locationType.${value}`), value })),
  );
  protected statuses = computed(() => [
    { label: this.i18n.translate('common.active'), value: true },
    { label: this.i18n.translate('common.inactive'), value: false },
  ]);
  protected filters = this.fb.group({
    search: [''],
    type: ['' as LocationType | ''],
    active: [undefined as boolean | undefined],
  });
  protected form = this.fb.group({
    name: ['', Validators.required],
    code: ['', Validators.required],
    type: ['WAREHOUSE' as LocationType, Validators.required],
    parentId: [''],
  });
  constructor() {
    this.loadTree();
  }
  protected loadTree() {
    this.loading.set(true);
    this.api
      .locationTree()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (tree) => {
          this.allLocations.set(this.flatten(tree));
          this.nodes.set(this.mapNodes(tree));
        },
        error: () => this.error(this.i18n.translate('locations.loadError')),
      });
  }
  protected applyFilters() {
    const f = this.filters.getRawValue();
    if (!f.search && !f.type && f.active === undefined) {
      this.loadTree();
      return;
    }
    this.loading.set(true);
    this.api
      .locations({
        search: f.search || undefined,
        type: f.type || undefined,
        active: f.active ?? undefined,
        page: 0,
        size: 100,
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => this.nodes.set(r.content.map((l) => this.node({ ...l, children: [] }))),
        error: () => this.error(this.i18n.translate('locations.filterError')),
      });
  }
  protected clearFilters() {
    this.filters.reset({ search: '', type: '', active: undefined });
    this.loadTree();
  }
  protected open(location?: Location, parent?: Location) {
    this.editing.set(location);
    this.form.reset(
      location
        ? {
            name: location.name,
            code: location.code,
            type: location.type,
            parentId: location.parentId ?? '',
          }
        : { name: '', code: '', type: 'WAREHOUSE', parentId: parent?.id ?? '' },
    );
    this.dialog.set(true);
  }
  protected save() {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    const f = this.form.getRawValue();
    const input: LocationInput = {
      name: f.name!,
      code: f.code!,
      type: f.type!,
      parentId: f.parentId || undefined,
    };
    this.api
      .saveLocation(input, this.editing()?.id)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => {
          this.dialog.set(false);
          this.messages.add({
            severity: 'success',
            summary: this.i18n.translate('common.success'),
            detail: this.i18n.translate(this.editing() ? 'locations.updated' : 'locations.created'),
          });
          this.loadTree();
        },
        error: (e) => this.error(this.i18n.translateError(e, 'locations.saveError')),
      });
  }
  protected toggle(l: Location) {
    this.confirmations.confirm({
      header: this.i18n.translate('locations.confirmHeader', {
        action: this.i18n.translate(l.active ? 'common.deactivate' : 'common.activate'),
      }),
      message: this.i18n.translate('locations.confirmMessage', {
        name: l.name,
        availability: this.i18n.translate(l.active ? 'items.unavailable' : 'items.available'),
      }),
      accept: () =>
        this.api.setLocationActive(l.id, !l.active).subscribe({
          next: () => {
            this.messages.add({
              severity: 'success',
              summary: this.i18n.translate('common.success'),
              detail: this.i18n.translate(
                l.active ? 'locations.deactivated' : 'locations.activated',
              ),
            });
            this.loadTree();
          },
          error: () => this.error(this.i18n.translate('locations.statusError')),
        }),
    });
  }
  protected label(v: string) {
    return this.i18n.translate(`enum.locationType.${v}`);
  }
  private mapNodes(values: Location[]): TreeNode<Location>[] {
    return values.map((v) => this.node(v));
  }
  private node(v: Location): TreeNode<Location> {
    return {
      key: v.id,
      label: v.name,
      data: v,
      expanded: true,
      icon: this.icon(v.type),
      children: this.mapNodes(v.children ?? []),
    };
  }
  private flatten(values: Location[]): Location[] {
    return values.flatMap((v) => [v, ...this.flatten(v.children ?? [])]);
  }
  private icon(type: LocationType) {
    return type === 'SHELF'
      ? 'pi pi-bars'
      : type === 'PALLET'
        ? 'pi pi-th-large'
        : type === 'COLD_STORAGE'
          ? 'pi pi-snowflake'
          : type === 'PRODUCTION_AREA'
            ? 'pi pi-cog'
            : 'pi pi-building';
  }
  private error(detail: string) {
    this.messages.add({ severity: 'error', summary: this.i18n.translate('common.error'), detail });
  }
}
