import axios from 'axios';
import type { ErrorResponse } from '../types/api';

export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ErrorResponse>(error)) {
    const payload = error.response?.data;
    if (!payload) {
      return error.message || 'Unexpected network error';
    }

    if (payload.validationErrors) {
      const firstValidationMessage = Object.values(payload.validationErrors)[0];
      if (firstValidationMessage) {
        return firstValidationMessage;
      }
    }

    return payload.message || payload.error || 'Unexpected server error';
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'Something went wrong';
}

export function mapValidationErrors(error: unknown): Record<string, string> {
  if (!axios.isAxiosError<ErrorResponse>(error)) {
    return {};
  }
  return error.response?.data?.validationErrors || {};
}
