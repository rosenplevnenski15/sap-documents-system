import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { DocumentResponse } from '../types/api';

export interface KnownDocument {
  id: string;
  title: string;
  createdByMe: boolean;
  hasBeenLoaded: boolean;
  lastLoadedAt?: string;
}

interface WorkspaceState {
  trackedDocumentIds: string[];
  knownDocuments: Record<string, KnownDocument>;
  addTrackedDocumentId: (documentId: string) => void;
  removeTrackedDocumentId: (documentId: string) => void;
  setKnownDocument: (document: KnownDocument) => void;
  markDocumentLoaded: (documentId: string, title?: string) => void;
  addCreatedDocument: (document: DocumentResponse) => void;
}

export const useWorkspaceStore = create<WorkspaceState>()(
  persist(
    (set, get) => ({
      trackedDocumentIds: [],
      knownDocuments: {},
      addTrackedDocumentId: (documentId) => {
        const normalized = documentId.trim();
        if (!normalized) return;
        if (get().trackedDocumentIds.includes(normalized)) return;
        set((state) => ({
          trackedDocumentIds: [normalized, ...state.trackedDocumentIds],
        }));
      },
      removeTrackedDocumentId: (documentId) => {
        set((state) => ({
          trackedDocumentIds: state.trackedDocumentIds.filter((id) => id !== documentId),
        }));
      },
      setKnownDocument: (document) => {
        set((state) => ({
          knownDocuments: {
            ...state.knownDocuments,
            [document.id]: document,
          },
        }));
      },
      markDocumentLoaded: (documentId, title) => {
        const existing = get().knownDocuments[documentId];
        set((state) => ({
          knownDocuments: {
            ...state.knownDocuments,
            [documentId]: {
              id: documentId,
              title: title || existing?.title || 'Loaded by ID',
              createdByMe: existing?.createdByMe || false,
              hasBeenLoaded: true,
              lastLoadedAt: new Date().toISOString(),
            },
          },
        }));
      },
      addCreatedDocument: (document) => {
        set((state) => ({
          knownDocuments: {
            ...state.knownDocuments,
            [document.id]: {
              id: document.id,
              title: document.title,
              createdByMe: true,
              hasBeenLoaded: false,
            },
          },
          trackedDocumentIds: [document.id, ...state.trackedDocumentIds.filter((id) => id !== document.id)],
        }));
      },
    }),
    {
      name: 'document-system-workspace',
      partialize: (state) => ({
        trackedDocumentIds: state.trackedDocumentIds,
        knownDocuments: state.knownDocuments,
      }),
    },
  ),
);
