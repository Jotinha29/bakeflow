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
