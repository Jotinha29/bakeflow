import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ConfirmationService, MessageService, TreeNode } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TreeModule } from 'primeng/tree';
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
import { FilterPanelComponent } from '../../../shared/ui/filter-panel/filter-panel.component';
import { SectionCardComponent } from '../../../shared/ui/section-card/section-card.component';
import { EmptyStateComponent } from '../../../shared/ui/empty-state/empty-state.component';
import { LoadingStateComponent } from '../../../shared/ui/loading-state/loading-state.component';
import { StatusBadgeComponent } from '../../../shared/ui/status-badge/status-badge.component';
import { LocationDetailsPanelComponent } from './location-details-panel.component';
@Component({
  selector: 'app-locations-page',
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    SelectModule,
    TreeModule,
    TranslatePipe,
    PageHeaderComponent,
    FilterPanelComponent,
    SectionCardComponent,
    EmptyStateComponent,
    LoadingStateComponent,
    StatusBadgeComponent,
    LocationDetailsPanelComponent,
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
  private tree = signal<Location[]>([]);
  private expandedLocationIds = signal<Set<string> | undefined>(undefined);
  protected allLocations = signal<Location[]>([]);
  protected hasLocations = computed(() => this.tree().length > 0);
  protected selectedNode = signal<TreeNode<Location> | null>(null);
  protected selectedLocation = computed(() => this.selectedNode()?.data);
  protected selectedParentName = computed(() => {
    const selected = this.selectedLocation();
    return selected?.parentId
      ? (this.allLocations().find((location) => location.id === selected.parentId)?.name ??
          this.i18n.translate('locations.noParent'))
      : this.i18n.translate('locations.structure');
  });
  protected selectedPath = computed(() => {
    const selected = this.selectedLocation();
    if (!selected) return [];
    const path = [selected.name];
    let parentId = selected.parentId;
    const visited = new Set<string>();
    while (parentId && !visited.has(parentId)) {
      visited.add(parentId);
      const parent = this.allLocations().find((location) => location.id === parentId);
      if (!parent) break;
      path.unshift(parent.name);
      parentId = parent.parentId;
    }
    return [this.i18n.translate('locations.structure'), ...path];
  });
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
    const selectedId = this.selectedLocation()?.id;
    this.captureExpansion();
    this.loading.set(true);
    this.api
      .locationTree()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (tree) => {
          this.tree.set(tree);
          this.allLocations.set(this.flatten(tree));
          this.nodes.set(this.mapNodes(tree));
          this.restoreSelection(selectedId);
        },
        error: () => this.error(this.i18n.translate('locations.loadError')),
      });
  }
  protected applyFilters() {
    const f = this.filters.getRawValue();
    this.captureExpansion();
    const filtered = this.filterTree(
      this.tree(),
      f.search ?? '',
      f.type ?? '',
      f.active ?? undefined,
    );
    this.nodes.set(this.mapNodes(filtered));
    this.restoreSelection(this.selectedLocation()?.id);
  }
  protected clearFilters() {
    this.filters.reset({ search: '', type: '', active: undefined });
    this.applyFilters();
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
  protected selectNode(event: { node: TreeNode<Location> }) {
    this.selectedNode.set(event.node);
  }
  private mapNodes(values: Location[]): TreeNode<Location>[] {
    return values.map((v) => this.node(v));
  }
  private node(v: Location): TreeNode<Location> {
    const expandedIds = this.expandedLocationIds();
    return {
      key: v.id,
      label: v.name,
      data: v,
      expanded: expandedIds === undefined || expandedIds.has(v.id),
      icon: this.icon(v.type),
      children: this.mapNodes(v.children ?? []),
    };
  }
  private captureExpansion() {
    if (this.nodes().length) this.expandedLocationIds.set(this.expandedIds(this.nodes()));
  }
  private expandedIds(nodes: TreeNode<Location>[]) {
    const ids = new Set<string>();
    const visit = (values: TreeNode<Location>[]) =>
      values.forEach((value) => {
        if (value.expanded && value.key) ids.add(String(value.key));
        visit(value.children ?? []);
      });
    visit(nodes);
    return ids;
  }
  private restoreSelection(id?: string) {
    if (!id) return;
    this.selectedNode.set(this.findNode(this.nodes(), id) ?? null);
  }
  private findNode(nodes: TreeNode<Location>[], id: string): TreeNode<Location> | undefined {
    for (const node of nodes) {
      if (node.key === id) return node;
      const child = this.findNode(node.children ?? [], id);
      if (child) return child;
    }
    return undefined;
  }
  private filterTree(
    values: Location[],
    search: string,
    type: LocationType | '',
    active: boolean | undefined,
  ): Location[] {
    const term = search.trim().toLocaleLowerCase();
    return values.flatMap((location) => {
      const children = this.filterTree(location.children ?? [], search, type, active);
      const matches =
        (!term ||
          location.name.toLocaleLowerCase().includes(term) ||
          location.code.toLocaleLowerCase().includes(term)) &&
        (!type || location.type === type) &&
        (active === undefined || location.active === active);
      return matches || children.length ? [{ ...location, children }] : [];
    });
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
