import { create } from 'zustand';

interface ConfirmOptions {
  title: string;
  description?: string;
  confirmText?: string;
  cancelText?: string;
}

interface ConfirmState {
  isOpen: boolean;
  title: string;
  description: string;
  confirmText: string;
  cancelText: string;
  resolver: ((value: boolean) => void) | null;
  ask: (options: ConfirmOptions) => Promise<boolean>;
  close: (accepted: boolean) => void;
}

export const useConfirmStore = create<ConfirmState>((set, get) => ({
  isOpen: false,
  title: '',
  description: '',
  confirmText: 'Confirm',
  cancelText: 'Cancel',
  resolver: null,
  ask: (options) =>
    new Promise<boolean>((resolve) => {
      set({
        isOpen: true,
        title: options.title,
        description: options.description || '',
        confirmText: options.confirmText || 'Confirm',
        cancelText: options.cancelText || 'Cancel',
        resolver: resolve,
      });
    }),
  close: (accepted) => {
    const resolve = get().resolver;
    if (resolve) resolve(accepted);
    set({
      isOpen: false,
      resolver: null,
      title: '',
      description: '',
      confirmText: 'Confirm',
      cancelText: 'Cancel',
    });
  },
}));
