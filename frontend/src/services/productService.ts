import { request } from '../utils/request';
import type { PageResponse, Product, Category, ImportResult } from '../types/product';

export interface SearchParams {
  keyword?: string;
  category?: string;
  status?: string;
  priceMin?: number;
  priceMax?: number;
  current?: number;
  pageSize?: number;
  sorter?: string;
}

export const productService = {
  async search(params: SearchParams): Promise<PageResponse<Product>> {
    const response = await request.get<PageResponse<Product>>('/products', { params });
    return response.data;
  },

  async exportExcel(params: Omit<SearchParams, 'current' | 'pageSize'>) {
    return request.get('/products/export', {
      params,
      responseType: 'blob',
    });
  },

  async importExcel(file: File): Promise<ImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await request.post<ImportResult>('/products/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  async getCategories(): Promise<Category[]> {
    const response = await request.get<Category[]>('/categories');
    return response.data;
  },
};
