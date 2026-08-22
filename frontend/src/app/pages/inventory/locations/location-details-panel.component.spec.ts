import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { Location } from '../../../features/inventory/inventory.models';
import { LocationDetailsPanelComponent } from './location-details-panel.component';

describe('LocationDetailsPanelComponent', () => {
  const location: Location = {
    id: 'shelf',
    name: 'Prateleira A',
    code: 'SH-A',
    type: 'SHELF',
    parentId: 'warehouse',
    active: true,
    children: [],
  };

  async function setup(canManage: boolean) {
    await TestBed.configureTestingModule({
      imports: [LocationDetailsPanelComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();
    const fixture = TestBed.createComponent(LocationDetailsPanelComponent);
    fixture.componentRef.setInput('location', location);
    fixture.componentRef.setInput('parentName', 'Estoque');
    fixture.componentRef.setInput('hierarchyPath', [
      'Estrutura da padaria',
      'Estoque',
      'Prateleira A',
    ]);
    fixture.componentRef.setInput('typeLabel', 'Prateleira');
    fixture.componentRef.setInput('canManage', canManage);
    fixture.detectChanges();
    return fixture;
  }

  afterEach(() => TestBed.resetTestingModule());

  it('shows real location data, parent, direct children and hierarchy path', async () => {
    const fixture = await setup(false);
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Prateleira A');
    expect(text).toContain('Estoque');
    expect(text).toContain('0 locais');
    expect(text).toContain('Estrutura da padaria');
  });

  it('exposes contextual actions only to users who can manage locations', async () => {
    const viewer = await setup(false);
    expect(viewer.nativeElement.textContent).not.toContain('Adicionar local subordinado');
    TestBed.resetTestingModule();
    const admin = await setup(true);
    expect(admin.nativeElement.textContent).toContain('Editar');
    expect(admin.nativeElement.textContent).toContain('Adicionar local subordinado');
    expect(admin.nativeElement.textContent).toContain('Desativar');
  });
});
