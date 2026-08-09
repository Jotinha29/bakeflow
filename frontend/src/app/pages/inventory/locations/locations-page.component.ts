import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
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
  ],
  templateUrl: './locations-page.component.html',
  styleUrls: ['../inventory-page.scss', './locations-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LocationsPageComponent {
  private fb = inject(FormBuilder);
  private api = inject(InventoryService);
  private messages = inject(MessageService);
  private confirmations = inject(ConfirmationService);
  protected loading = signal(false);
  protected submitting = signal(false);
  protected nodes = signal<TreeNode<Location>[]>([]);
  protected allLocations = signal<Location[]>([]);
  protected dialog = signal(false);
  protected editing = signal<Location | undefined>(undefined);
  protected types = [
    'WAREHOUSE',
    'ROOM',
    'AISLE',
    'SHELF',
    'PALLET',
    'PRODUCTION_AREA',
    'COLD_STORAGE',
    'OTHER',
  ].map((value) => ({ label: this.label(value), value }));
  protected statuses = [
    { label: 'Active', value: true },
    { label: 'Inactive', value: false },
  ];
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
        error: () => this.error('Could not load locations.'),
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
        error: () => this.error('Could not filter locations.'),
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
            summary: 'Success',
            detail: `Location ${this.editing() ? 'updated' : 'created'} successfully.`,
          });
          this.loadTree();
        },
        error: (e) => this.error(e.error?.message ?? 'Could not save location.'),
      });
  }
  protected toggle(l: Location) {
    this.confirmations.confirm({
      header: `${l.active ? 'Deactivate' : 'Activate'} location?`,
      message: `${l.name} will be ${l.active ? 'unavailable' : 'available'} for new operations.`,
      accept: () =>
        this.api.setLocationActive(l.id, !l.active).subscribe({
          next: () => {
            this.messages.add({
              severity: 'success',
              summary: 'Success',
              detail: `Location ${l.active ? 'deactivated' : 'activated'} successfully.`,
            });
            this.loadTree();
          },
          error: () => this.error('Could not update location status.'),
        }),
    });
  }
  protected label(v: string) {
    return v
      .toLowerCase()
      .replaceAll('_', ' ')
      .replace(/\b\w/g, (c) => c.toUpperCase());
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
    this.messages.add({ severity: 'error', summary: 'Something went wrong', detail });
  }
}
