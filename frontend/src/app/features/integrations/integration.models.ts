export type ResultStatus = 'FOUND' | 'NOT_FOUND' | 'UNAVAILABLE';
export interface ExternalProduct {
  status: ResultStatus;
  barcode: string;
  name?: string;
  brand?: string;
  quantity?: string;
  imageUrl?: string;
  categories: string[];
  source: 'OPEN_FOOD_FACTS';
  fresh: boolean;
  errorCode?: string;
}
export interface ExternalCompany {
  status: ResultStatus;
  taxId: string;
  legalName?: string;
  tradeName?: string;
  companyStatus?: string;
  street?: string;
  number?: string;
  district?: string;
  city?: string;
  state?: string;
  postalCode?: string;
  source: 'BRASIL_API';
  fresh: boolean;
  errorCode?: string;
}
export interface IntegrationStatus {
  openFoodFacts: { configured: boolean; circuitState: string };
  brasilApi: { configured: boolean; circuitState: string };
  redisAvailable: boolean;
}
