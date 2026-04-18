import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { Role } from '../types/api';

export type ActivityAction =
  | 'LOGIN'
  | 'REGISTER'
  | 'CREATE_DOCUMENT'
  | 'CREATE_VERSION'
  | 'SUBMIT_FOR_REVIEW'
  | 'APPROVE_VERSION'
  | 'REJECT_VERSION'
  | 'ADD_COMMENT'
  | 'EXPORT_PDF'
  | 'CHANGE_ROLE'
  | 'DEACTIVATE_USER'
  | 'ACTIVATE_USER'
  | 'LOGOUT';

export interface ActivityEntry {
  id: string;
  action: ActivityAction;
  description: string;
  role: Role;
  userId: string;
  username: string;
  createdAt: string;
}

interface ActivityState {
  entries: ActivityEntry[];
  addEntry: (entry: Omit<ActivityEntry, 'id' | 'createdAt'>) => void;
  clearMine: (userId: string) => void;
}

export const useActivityStore = create<ActivityState>()(
  persist(
    (set) => ({
      entries: [],
      addEntry: (entry) => {
        set((state) => ({
          entries: [
            {
              ...entry,
              id: crypto.randomUUID(),
              createdAt: new Date().toISOString(),
            },
            ...state.entries,
          ].slice(0, 500),
        }));
      },
      clearMine: (userId) => {
        set((state) => ({
          entries: state.entries.filter((item) => item.userId !== userId),
        }));
      },
    }),
    {
      name: 'document-system-activity',
    },
  ),
);
