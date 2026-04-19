import { apiClient } from './client';
import type { CommentResponse, CreateCommentRequest } from '../types/api';

export const commentsApi = {
  async addComment(versionId: string, payload: CreateCommentRequest) {
    const response = await apiClient.post<CommentResponse>(`/versions/${versionId}/comments`, payload);
    return response.data;
  },

  async getComments(versionId: string) {
    const response = await apiClient.get<CommentResponse[]>(`/versions/${versionId}/comments`);
    return response.data;
  },
};
