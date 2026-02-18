export interface Product {
  id: number;
  productCode: string;
  productName: string;
  category: string;
  price: number;
  stockQuantity: number;
  status: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: number;
  categoryCode: string;
  categoryName: string;
  sortOrder: number;
}

export interface PageResponse<T> {
  data: T[];
  total: number;
  success: boolean;
  current: number;
  pageSize: number;
}

export interface ImportError {
  row: number;
  field: string;
  message: string;
}

export interface ImportResult {
  success: boolean;
  totalRows: number;
  insertedCount: number;
  updatedCount: number;
  errorCount: number;
  errors: ImportError[];
}
