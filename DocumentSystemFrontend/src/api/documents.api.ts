import { apiClient } from './client';
import type { CompareResponse, DocumentResponse } from '../types/api';

export const documentsApi = {
  async createDocument(formData: FormData) {
    const response = await apiClient.post<DocumentResponse>('/documents', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response.data;
  },

  async compareLatest(documentId: string) {
    const response = await apiClient.get<CompareResponse>(`/documents/${documentId}/compare`);
    return response.data;
  },
};
