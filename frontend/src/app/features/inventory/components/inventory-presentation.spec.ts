import { TestBed } from '@angular/core/testing';
import { InventoryMovementTypeBadgeComponent } from './inventory-movement-type-badge/inventory-movement-type-badge.component';
import { StockQuantityDisplayComponent } from './stock-quantity-display/stock-quantity-display.component';

describe('inventory presentation components',()=>{
  it('translates movement types instead of exposing backend enums',()=>{const fixture=TestBed.createComponent(InventoryMovementTypeBadgeComponent);fixture.componentRef.setInput('type','PRODUCTION_OUTPUT');fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('Produção');expect(fixture.nativeElement.textContent).not.toContain('PRODUCTION_OUTPUT');});
  it('formats quantities and localized unit labels',()=>{const fixture=TestBed.createComponent(StockQuantityDisplayComponent);fixture.componentRef.setInput('quantity',3000);fixture.componentRef.setInput('unit','G');fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('3.000');expect(fixture.nativeElement.textContent).toContain('g');});
});
