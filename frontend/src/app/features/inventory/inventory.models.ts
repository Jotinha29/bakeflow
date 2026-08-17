export type ItemType = 'RAW_MATERIAL' | 'FINISHED_PRODUCT' | 'PACKAGING' | 'OTHER';
export type UnitOfMeasure = 'UNIT' | 'KG' | 'G' | 'L' | 'ML';
export type LocationType =
  | 'WAREHOUSE'
  | 'ROOM'
  | 'AISLE'
  | 'SHELF'
  | 'PALLET'
  | 'PRODUCTION_AREA'
  | 'COLD_STORAGE'
  | 'OTHER';
export interface PageResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type StockMovementType = 'ENTRY' | 'EXIT' | 'TRANSFER' | 'LOSS' | 'ADJUSTMENT' | 'PRODUCTION_CONSUMPTION' | 'PRODUCTION_OUTPUT';
export interface StockBalance { id: string; itemId: string; itemName: string; sku: string; batchId: string; batchCode: string; expirationDate?: string; expirationStatus: string; locationId: string; locationName: string; quantity: number; unit: UnitOfMeasure; updatedAt: string; }
export interface StockMovement { id: string; type: StockMovementType; itemId: string; itemName: string; sku: string; batchId: string; batchCode: string; sourceLocationId?: string; sourceLocationName?: string; destinationLocationId?: string; destinationLocationName?: string; quantity: number; unit: UnitOfMeasure; actorUserId?: string; actorName?: string; reason?: string; notes?: string; reference?: string; previousQuantity?: number; resultingQuantity?: number; createdAt: string; }
export interface StockOperation { itemId: string; batchId: string; locationId?: string; sourceLocationId?: string; destinationLocationId?: string; quantity?: number; physicalQuantity?: number; reason?: string; notes?: string; justification?: string; }
export interface Item {
  id: string;
  name: string;
  sku?: string;
  barcode?: string;
  type: ItemType;
  unit: UnitOfMeasure;
  minimumStock?: number;
  active: boolean;
}
export type ItemInput = Omit<Item, 'id' | 'active'>;
export interface Batch {
  id: string;
  itemId: string;
  itemName: string;
  code: string;
  manufacturingDate?: string;
  expirationDate?: string;
  active: boolean;
}
export interface BatchInput {
  itemId: string;
  code: string;
  manufacturingDate?: string;
  expirationDate?: string;
}
export interface Location {
  id: string;
  name: string;
  code: string;
  type: LocationType;
  parentId?: string;
  active: boolean;
  children: Location[];
}
export interface LocationInput {
  name: string;
  code: string;
  type: LocationType;
  parentId?: string;
}
export interface ProductInformation {
  status: 'FOUND' | 'NOT_FOUND' | 'UNAVAILABLE';
  barcode: string;
  name?: string;
  brand?: string;
  imageUrl?: string;
  quantity?: string;
  categories: string[];
  message?: string;
}
