import { apiClient } from './client';
import type { VersionContentResponse, VersionResponse } from '../types/api';

export const versionsApi = {
  async createVersion(documentId: string, file: File) {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.post<VersionResponse>(
      `/versions/documents/${documentId}/versions`,
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      },
    );

    return response.data;
  },

  async submit(versionId: string) {
    const response = await apiClient.post<VersionResponse>(`/versions/${versionId}/submit`);
    return response.data;
  },

  async approve(versionId: string) {
    const response = await apiClient.post<VersionResponse>(`/versions/${versionId}/approve`);
    return response.data;
  },

  async reject(versionId: string) {
    const response = await apiClient.post<VersionResponse>(`/versions/${versionId}/reject`);
    return response.data;
  },

  async getVersions(documentId: string) {
    const response = await apiClient.get<VersionResponse[]>(`/versions/${documentId}/versions`);
    return response.data;
  },

  async getActiveVersion(documentId: string) {
    const response = await apiClient.get<VersionResponse>(`/versions/${documentId}/active`);
    return response.data;
  },

  async getVersionById(versionId: string) {
    const response = await apiClient.get<VersionResponse>(`/versions/${versionId}`);
    return response.data;
  },

  async getVersionContent(versionId: string) {
    const response = await apiClient.get<VersionContentResponse>(`/versions/${versionId}/content`);
    return response.data;
  },

  async updateDraftFile(versionId: string, file: File) {
    const formData = new FormData();
    formData.append('file', file);

    const response = await apiClient.put<VersionResponse>(`/versions/${versionId}/file`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  async exportPdf(versionId: string) {
    const response = await apiClient.get(`/versions/${versionId}/export/pdf`, {
      responseType: 'blob',
    });
    return response.data as Blob;
  },
};
