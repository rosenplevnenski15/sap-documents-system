import { toast } from 'sonner';
import { getErrorMessage } from './http-error';

function toastId(scope: string, event: string, entityId?: string) {
  if (!entityId) {
    return `${scope}:${event}`;
  }

  return `${scope}:${event}:${entityId}`;
}

export function notifySuccess(scope: string, event: string, message: string, entityId?: string) {
  toast.success(message, {
    id: toastId(scope, event, entityId),
  });
}

export function notifyError(scope: string, event: string, error: unknown, entityId?: string) {
  toast.error(getErrorMessage(error), {
    id: toastId(scope, event, entityId),
  });
}

export function notifyErrorMessage(scope: string, event: string, message: string, entityId?: string) {
  toast.error(message, {
    id: toastId(scope, event, entityId),
  });
}
