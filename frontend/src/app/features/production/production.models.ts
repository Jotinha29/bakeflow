import { UnitOfMeasure } from '../inventory/inventory.models';
export type ProductionStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export interface IngredientInput { itemId: string; quantity: number; unit: UnitOfMeasure; }
export interface RecipeInput { name: string; outputItemId: string; yieldQuantity: number; yieldUnit: UnitOfMeasure; shelfLifeDays?: number; active: boolean; notes?: string; ingredients: IngredientInput[]; }
export interface Ingredient extends IngredientInput { id: string; itemName: string; }
export interface Recipe extends RecipeInput { id: string; outputItemName: string; ingredients: Ingredient[]; createdAt: string; updatedAt: string; }
export interface Allocation { itemId: string; itemName: string; batchId: string; batchCode: string; locationId: string; locationName: string; expirationDate?: string; quantity: number; }
export interface Requirement { itemId: string; itemName: string; required: number; available: number; unit: UnitOfMeasure; sufficient: boolean; allocations: Allocation[]; }
export interface ProductionOutput { itemId: string; itemName: string; batchId: string; batchCode: string; locationId: string; locationName: string; quantity: number; }
export interface ProductionOrder { id: string; code: string; recipeId: string; recipeName: string; outputItemId: string; outputItemName: string; plannedQuantity: number; actualQuantity?: number; unit: UnitOfMeasure; status: ProductionStatus; plannedDate: string; startedAt?: string; completedAt?: string; differenceReason?: string; notes?: string; requirements: Requirement[]; consumptions: Allocation[]; output?: ProductionOutput; createdAt: string; updatedAt: string; }
export interface OrderInput { recipeId: string; plannedQuantity: number; plannedDate: string; notes?: string; }
export interface ProductionPreview { recipe: Recipe; plannedQuantity: number; requirements: Requirement[]; }
export interface CompleteInput { actualQuantity: number; destinationLocationId: string; differenceReason?: string; notes?: string; }
