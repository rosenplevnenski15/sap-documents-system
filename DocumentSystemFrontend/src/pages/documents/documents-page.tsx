import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Check, Eye, FileDown, FileText, Loader2, Pencil, X } from 'lucide-react';
import { createDocumentSchema, createVersionSchema, commentSchema } from '../../lib/validation';
import { documentsApi } from '../../api/documents.api';
import { versionsApi } from '../../api/versions.api';
import { commentsApi } from '../../api/comments.api';
import { useAuthStore } from '../../store/auth.store';
import { useWorkspaceStore } from '../../store/workspace.store';
import { useActivityStore } from '../../store/activity.store';
import { downloadBlob, formatDate } from '../../lib/utils';
import { notifyError, notifyErrorMessage, notifySuccess } from '../../lib/notify';
import { Button } from '../../components/common/button';
import { TextInput } from '../../components/common/text-input';
import { LoadingBlock } from '../../components/common/loading-block';
import { RoleBadge } from '../../components/common/role-badge';
import { Tooltip } from '../../components/common/tooltip';
import { RichTextEditor } from '../../components/documents/rich-text-editor';
import type { CommentResponse, CompareResponse, VersionContentResponse, VersionResponse } from '../../types/api';

type CreateDocumentValues = z.infer<typeof createDocumentSchema>;
type CreateVersionValues = z.infer<typeof createVersionSchema>;
type CommentValues = z.infer<typeof commentSchema>;

interface DocumentWorkspaceData {
  active?: VersionResponse;
  versions: VersionResponse[];
  error?: string;
  loading: boolean;
}

function filterVisibleVersions(
  role: string | undefined,
  userId: string | undefined,
  versions: VersionResponse[],
) {
  if (!role) return [];

  if (role === 'READER') {
    return versions.filter((item) => item.isActive);
  }

  if (role === 'AUTHOR') {
    return versions.filter((item) => item.isActive || item.createdBy.id === userId);
  }

  if (role === 'REVIEWER') {
    return versions.filter((item) => item.isActive || item.status === 'IN_REVIEW');
  }

  if (role === 'ADMIN') {
    return versions;
  }

  return [];
}

function canAccessVersionByRole(
  role: string | undefined,
  userId: string | undefined,
  version: VersionResponse,
) {
  return (
    role === 'ADMIN' ||
    version.isActive ||
    (role === 'AUTHOR' && version.createdBy.id === userId) ||
    (role === 'REVIEWER' && version.status === 'IN_REVIEW')
  );
}

function getVersionStatusLabel(version: VersionResponse) {
  if (version.isActive) {
    return `${version.status} (ACTIVE)`;
  }

  return version.status;
}

export function DocumentsPage() {
  const user = useAuthStore((state) => state.user);
  const trackedDocumentIds = useWorkspaceStore((state) => state.trackedDocumentIds);
  const addTrackedDocumentId = useWorkspaceStore((state) => state.addTrackedDocumentId);
  const removeTrackedDocumentId = useWorkspaceStore((state) => state.removeTrackedDocumentId);
  const markDocumentLoaded = useWorkspaceStore((state) => state.markDocumentLoaded);
  const addCreatedDocument = useWorkspaceStore((state) => state.addCreatedDocument);
  const addActivity = useActivityStore((state) => state.addEntry);

  const [documentMap, setDocumentMap] = useState<Record<string, DocumentWorkspaceData>>({});
  const [selectedDocumentId, setSelectedDocumentId] = useState<string>('');
  const [compareResult, setCompareResult] = useState<CompareResponse | null>(null);
  const [, setCompareResultDocumentId] = useState<string>('');
  const [previewUrl, setPreviewUrl] = useState<string>('');
  const [previewVersionId, setPreviewVersionId] = useState<string>('');
  const [isViewerOpen, setIsViewerOpen] = useState(false);
  const [selectedInlineVersion, setSelectedInlineVersion] = useState<VersionContentResponse | null>(null);
  const [isInlineViewerOpen, setIsInlineViewerOpen] = useState(false);
  const [isInlineEditMode, setIsInlineEditMode] = useState(false);
  const [isInlineContentLoading, setIsInlineContentLoading] = useState(false);
  const [inlineContentError, setInlineContentError] = useState<string | null>(null);
  const [inlineEditorContent, setInlineEditorContent] = useState('');
  const [inlineEditorInitialContent, setInlineEditorInitialContent] = useState('');
  const [inlineEditorError, setInlineEditorError] = useState<string | null>(null);
  const [inlineEditVersionId, setInlineEditVersionId] = useState<string>('');
  const [isInlineSavePending, setIsInlineSavePending] = useState(false);
  const [isInlineSubmitPending, setIsInlineSubmitPending] = useState(false);
  const [commentVersionId, setCommentVersionId] = useState<string>('');
  const [commentsByVersionId, setCommentsByVersionId] = useState<Record<string, CommentResponse[]>>({});
  const [commentLoading, setCommentLoading] = useState(false);
  const [showCreateDocumentForm, setShowCreateDocumentForm] = useState(false);
  const [isComparePending, setIsComparePending] = useState(false);
  const [pendingVersionActions, setPendingVersionActions] = useState<Record<string, boolean>>({});
  const [highlightedDocumentId] = useState<string>('');
  const createSectionRef = useRef<HTMLElement | null>(null);
  const trackedCardRefs = useRef<Record<string, HTMLDivElement | null>>({});
  const highlightTimeoutRef = useRef<number | null>(null);
  const inlineContentRequestRef = useRef(0);
  const commentsRequestRef = useRef(0);

  const createDocumentForm = useForm<CreateDocumentValues>({
    resolver: zodResolver(createDocumentSchema),
    defaultValues: {
      title: '',
      content: '',
    },
  });

  const createVersionForm = useForm<CreateVersionValues>({
    resolver: zodResolver(createVersionSchema),
    defaultValues: {
      documentId: '',
      content: '',
    },
  });

  const commentForm = useForm<CommentValues>({
    resolver: zodResolver(commentSchema),
    defaultValues: {
      content: '',
    },
  });

  function stripHtmlToPlainText(content: string) {
    return content.replace(/<[^>]+>/g, '').trim();
  }

  const createDocumentTitle = createDocumentForm.watch('title');
  const hasCreateDocumentTitle = createDocumentTitle.trim().length > 0;
  const createDocumentContent = createDocumentForm.watch('content');
  const hasCreateDocumentContent = stripHtmlToPlainText(createDocumentContent).length > 0;
  const canSubmitCreateDocument =
    hasCreateDocumentTitle && hasCreateDocumentContent && !createDocumentForm.formState.isSubmitting;
  const createVersionDocumentId = createVersionForm.watch('documentId');
  const createVersionContent = createVersionForm.watch('content');
  const hasCreateVersionContent = stripHtmlToPlainText(createVersionContent).length > 0;
  const canSubmitCreateVersion =
    createVersionDocumentId.trim().length > 0 &&
    hasCreateVersionContent &&
    !createVersionForm.formState.isSubmitting;

  const refreshDocument = async (documentId: string) => {
    setDocumentMap((prev) => ({
      ...prev,
      [documentId]: {
        ...(prev[documentId] || { versions: [] }),
        loading: true,
      },
    }));

    try {
      const [activeResult, versionsResult] = await Promise.allSettled([
        versionsApi.getActiveVersion(documentId),
        versionsApi.getVersions(documentId),
      ]);

      const active = activeResult.status === 'fulfilled' ? activeResult.value : null;
      let versions = versionsResult.status === 'fulfilled' ? versionsResult.value : [];

      if (active && !versions.some((item) => item.id === active.id)) {
        versions.push(active);
      }

      const filtered = filterVisibleVersions(user?.role, user?.id, versions);

      console.log('ROLE:', user?.role);
      console.log('VISIBLE:', filtered);

      if (activeResult.status === 'rejected' && versionsResult.status === 'rejected') {
        setDocumentMap((prev) => {
          const next = { ...prev };
          delete next[documentId];
          return next;
        });

        return { error: 'Unable to load this document ID.', title: '' };
      }

      setDocumentMap((prev) => ({
        ...prev,
        [documentId]: {
          active: active || undefined,
          versions: filtered,
          loading: false,
        },
      }));

      return { error: undefined, title: active?.document?.title || versions[0]?.document?.title || '' };
    } catch {
      setDocumentMap((prev) => {
        const next = { ...prev };
        delete next[documentId];
        return next;
      });

      return { error: 'Unable to load this document ID.', title: '' };
    }
  };

  useEffect(() => {
    trackedDocumentIds.forEach((documentId) => {
      void refreshDocument(documentId);
    });
  }, [trackedDocumentIds]);

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }

      if (highlightTimeoutRef.current) {
        window.clearTimeout(highlightTimeoutRef.current);
      }
    };
  }, [previewUrl]);

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        closeInlineViewer();
        setIsViewerOpen(false);
        setPreviewUrl('');
      }
    };

    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  useEffect(() => {
    if (isViewerOpen || isInlineViewerOpen) {
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = 'auto';
      };
    }

    document.body.style.overflow = 'auto';
  }, [isViewerOpen, isInlineViewerOpen]);

  const selectedData = selectedDocumentId ? documentMap[selectedDocumentId] : undefined;
  const selectedViewerVersion =
    selectedDocumentId && previewVersionId
      ? getVersionById(selectedDocumentId, previewVersionId)
      : undefined;
  const selectedFileName = selectedViewerVersion?.fileName;
  const selectedIsPdf = isPdfFileName(selectedFileName);
  const selectedIsText = isPlainTextFileName(selectedFileName);
  const selectedRenderableContent = selectedViewerVersion?.content;
  const selectedRenderableFileName = selectedViewerVersion?.fileName;

  useEffect(() => {
    if (!selectedDocumentId) {
      return;
    }

    void refreshDocument(selectedDocumentId);

    if (!selectedViewerVersion) {
      setIsViewerOpen(false);
      setPreviewUrl('');
      setPreviewVersionId('');
      setCompareResult(null);
      setCompareResultDocumentId('');
      return;
    }

    if (previewVersionId !== selectedViewerVersion.id) {
      void handlePdfPreview(selectedViewerVersion.id);
    }
  }, [selectedDocumentId, selectedViewerVersion?.id, previewVersionId]);

  const visibleVersions = useMemo(() => {
    if (!selectedData) return [];
    return filterVisibleVersions(user?.role, user?.id, selectedData.versions);
  }, [selectedData, user?.id, user?.role]);

  const commentList = commentVersionId ? (commentsByVersionId[commentVersionId] ?? []) : [];

  const availableDocuments = useMemo(() => {
    return Object.entries(documentMap)
      .flatMap(([documentId, data]) => {
        const hasBackendData = (data.versions?.length || 0) > 0 || !!data.active;
        if (!hasBackendData) {
          return [];
        }

        const latestVersion = data.versions?.length
          ? [...data.versions].sort((left, right) => right.versionNumber - left.versionNumber)[0]
          : data.active;

        const title = latestVersion?.document?.title || data.active?.document?.title;

        if (!title) {
          return [];
        }

        return [{
          id: documentId,
          title,
          createdByMe: !!user?.id &&
            (data.versions.some((version) => version.createdBy.id === user.id) || data.active?.createdBy.id === user.id),
          hasBeenLoaded: true,
          versionNumber: latestVersion?.versionNumber || null,
          status: latestVersion?.status || data.active?.status || 'UNKNOWN',
          isActive: !!latestVersion?.isActive,
        }];
      })
      .sort((left, right) => {
        const titleCompare = left.title.localeCompare(right.title);
        return titleCompare !== 0 ? titleCompare : left.id.localeCompare(right.id);
      });
  }, [documentMap, user?.id]);

  const realDocuments = useMemo(() => {
    return availableDocuments;
  }, [availableDocuments]);

  const myDocuments = useMemo(() => {
    return availableDocuments.filter((document) => document.createdByMe);
  }, [availableDocuments]);

  const recentlyViewedDocuments = useMemo(() => {
    const availableById = new Map(availableDocuments.map((document) => [document.id, document]));

    return trackedDocumentIds
      .map((documentId) => availableById.get(documentId))
      .filter((document): document is (typeof availableDocuments)[number] => !!document)
      .slice(0, 10);
  }, [availableDocuments, trackedDocumentIds]);

  useEffect(() => {
    if (!availableDocuments.length) {
      setSelectedDocumentId('');
      createVersionForm.setValue('documentId', '');
      return;
    }

    const selectedExists = availableDocuments.some((document) => document.id === selectedDocumentId);

    if (!selectedDocumentId || !selectedExists) {
      setSelectedDocumentId(availableDocuments[0].id);
      createVersionForm.setValue('documentId', availableDocuments[0].id);
    }
  }, [availableDocuments, createVersionForm, selectedDocumentId]);

  const createVersionDocuments = useMemo(() => {
    if (user?.role === 'ADMIN') {
      return availableDocuments;
    }

    if (user?.role === 'AUTHOR') {
      return availableDocuments.filter((document) => document.createdByMe);
    }

    if (user?.role === 'REVIEWER') {
      return availableDocuments.filter((document) => document.status === 'IN_REVIEW');
    }

    return [];
  }, [availableDocuments, user?.role]);

  function getDocumentStatus(documentId: string) {
    const data = documentMap[documentId];

    if (data?.versions?.some((version) => version.status === 'IN_REVIEW')) {
      return 'IN_REVIEW';
    }

    if (data?.active?.status) {
      return data.active.status;
    }

    if (!data?.versions?.length) {
      return 'UNKNOWN';
    }

    const latestVersion = [...data.versions].sort((a, b) => b.versionNumber - a.versionNumber)[0];
    return latestVersion?.status || 'UNKNOWN';
  }

  function getStatusBadgeClass(status: string) {
    if (status === 'APPROVED' || status === 'ACTIVE') return 'bg-emerald-100 text-emerald-800';
    if (status === 'IN_REVIEW') return 'bg-amber-100 text-amber-800';
    if (status === 'REJECTED') return 'bg-red-100 text-red-800';
    if (status === 'DRAFT') return 'bg-slate-200 text-slate-700';
    return 'bg-slate-100 text-slate-600';
  }

  function getCommentsAllowed(version: VersionResponse) {
    return canAccessVersionByRole(user?.role, user?.id, version);
  }

  function canEditDraft(version: VersionResponse) {
    return version.status === 'DRAFT' && (user?.role === 'AUTHOR' || user?.role === 'ADMIN');
  }

  function getDocumentVersions(documentId: string) {
    return documentMap[documentId]?.versions || [];
  }

  function getLatestVersion(documentId: string) {
    const versions = getDocumentVersions(documentId);
    if (!versions.length) return undefined;
    return [...versions].sort((a, b) => b.versionNumber - a.versionNumber)[0];
  }

  function getInReviewVersion(documentId: string) {
    const versions = getDocumentVersions(documentId).filter((version) => version.status === 'IN_REVIEW');
    if (!versions.length) return undefined;
    return [...versions].sort((a, b) => b.versionNumber - a.versionNumber)[0];
  }

  function getVersionById(documentId: string, versionId: string) {
    return getDocumentVersions(documentId).find((version) => version.id === versionId);
  }

  function getDocumentTitle(documentId: string) {
    const latestVersion = getLatestVersion(documentId);
    return latestVersion?.document?.title || documentMap[documentId]?.active?.document?.title || 'Untitled Document';
  }

  function isDocumentCreatedByCurrentUser(documentId: string) {
    if (!user?.id) return false;

    const data = documentMap[documentId];
    if (!data) return false;

    return data.versions.some((version) => version.createdBy.id === user.id) || data.active?.createdBy.id === user.id;
  }

  function isPdfFileName(fileName?: string) {
    return !!fileName?.toLowerCase().endsWith('.pdf');
  }

  function isPlainTextFileName(fileName?: string) {
    const normalized = fileName?.toLowerCase() || '';
    return normalized.endsWith('.txt') || normalized.endsWith('.md') || normalized.endsWith('.csv') || normalized.endsWith('.log');
  }

  function renderVersionContent(fileName: string | undefined, content: string) {
    const looksLikeHtml = /<[^>]+>/.test(content);

    if (!looksLikeHtml || isPlainTextFileName(fileName)) {
      return (
        <pre className="whitespace-pre-wrap break-words font-mono text-[16px] leading-8 text-slate-800">
          {content}
        </pre>
      );
    }

    return (
      <div
        className="space-y-5 text-[16px] leading-8 text-slate-800"
        dangerouslySetInnerHTML={{ __html: content }}
      />
    );
  }

  const selectDocument = (documentId: string) => {
    setSelectedDocumentId(documentId);
    createVersionForm.setValue('documentId', documentId);
    markDocumentLoaded(documentId, getDocumentTitle(documentId));
    setIsInlineViewerOpen(false);
    setSelectedInlineVersion(null);
    setIsInlineContentLoading(false);
    setInlineContentError('');
    setIsViewerOpen(false);
    setCompareResult(null);
    setCompareResultDocumentId('');
    setPreviewUrl('');
    setPreviewVersionId('');
  };

  const handleEditDocument = (documentId: string) => {
    selectDocument(documentId);
    createSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  const makeVersionActionKey = (action: string, versionId: string) => `${action}:${versionId}`;

  const isVersionActionPending = (action: string, versionId: string) => {
    return !!pendingVersionActions[makeVersionActionKey(action, versionId)];
  };

  const renderPendingLabel = (pending: boolean, idleText: string, pendingText: string) => {
    if (!pending) {
      return <span>{idleText}</span>;
    }

    return (
      <span className="inline-flex items-center justify-center gap-2">
        <Loader2 size={14} className="animate-spin" />
        {pendingText}
      </span>
    );
  };

  const renderDocumentCardSkeleton = () => {
    return (
      <article className="rounded-lg border border-slate-300 bg-white p-3 min-h-[168px]">
        <div className="animate-pulse">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0 flex-1">
              <div className="h-4 w-3/5 rounded bg-gray-200 dark:bg-slate-700" />
              <div className="mt-2 h-4 w-20 rounded-full bg-gray-200 dark:bg-slate-700" />
              <div className="mt-2 space-y-1">
                <div className="h-3 w-4/5 rounded bg-gray-200 dark:bg-slate-700" />
                <div className="h-3 w-2/3 rounded bg-gray-200 dark:bg-slate-700" />
                <div className="h-3 w-1/3 rounded bg-gray-200 dark:bg-slate-700" />
              </div>
            </div>

            <div className="flex flex-col items-end gap-2">
              <div className="h-5 w-16 rounded bg-gray-200 dark:bg-slate-700" />
              <div className="flex items-center gap-1 rounded-md border border-slate-200 p-1">
                <div className="h-7 w-7 rounded bg-gray-200 dark:bg-slate-700" />
                <div className="h-7 w-7 rounded bg-gray-200 dark:bg-slate-700" />
                <div className="h-7 w-7 rounded bg-gray-200 dark:bg-slate-700" />
                <div className="h-7 w-7 rounded bg-gray-200 dark:bg-slate-700" />
              </div>
            </div>
          </div>
        </div>
      </article>
    );
  };

  const renderDocumentCard = (documentId: string, isSelected = false, asButton = false, isHighlighted = false) => {
    const title = getDocumentTitle(documentId);
    const status = getDocumentStatus(documentId);
    const versionCount = documentMap[documentId]?.versions?.length;
    const viewVersion = getLatestVersion(documentId);
    const inReviewVersion = getInReviewVersion(documentId);
    const createdByMe = isDocumentCreatedByCurrentUser(documentId);
    const canView =
      !!viewVersion &&
      canAccessVersionByRole(user?.role, user?.id, viewVersion);

    console.log('ROLE:', user?.role);
    console.log('VERSION:', viewVersion);
    console.log('VIEW CHECK:', {
      role: user?.role,
      isActive: viewVersion?.isActive,
      status: viewVersion?.status,
    });
    const canEdit = user?.role === 'AUTHOR' && status === 'DRAFT';
    const canModerate = (user?.role === 'REVIEWER' || user?.role === 'ADMIN') && status === 'IN_REVIEW' && !!inReviewVersion;
    const canExport = !!viewVersion?.isActive;
    const cardClass = `rounded-xl border p-4 min-h-[176px] text-left transform-gpu shadow-md transition-all duration-200 ease-out hover:-translate-y-1 hover:shadow-xl ${
      isSelected
        ? 'border-slate-900 bg-slate-900 text-white shadow-soft'
        : 'border-slate-300 bg-white text-slate-700 hover:border-slate-400'
    } ${isHighlighted ? 'ring-2 ring-cyan-400 ring-offset-2' : ''}`;

    const actionButtonClass = `h-9 min-w-[92px] gap-2 px-3 text-xs font-semibold ${
      isSelected
        ? 'border-white/15 bg-white/10 text-white hover:bg-white/20'
        : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50'
    }`;

    const actionLabelClass = 'inline-flex items-center justify-center gap-2';

    const cardContent = (
      <>
        <div className="flex h-full flex-col gap-4">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0 flex-1 space-y-2">
              <div className="flex items-start gap-2">
                <p className="min-w-0 flex-1 truncate text-sm font-bold leading-5">{title}</p>
              </div>
              <div className="flex items-center gap-2">
              {createdByMe ? (
                <span
                  className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                    isSelected ? 'bg-white/20 text-white' : 'bg-cyan-100 text-cyan-800'
                  }`}
                >
                  createdByMe
                </span>
              ) : null}
              </div>
              <div className={`space-y-1.5 text-xs leading-5 ${isSelected ? 'text-slate-200' : 'text-slate-500'}`}>
                <p className="truncate">ID: {documentId}</p>
                <p>Versions: {typeof versionCount === 'number' ? versionCount : '-'}</p>
              </div>
            </div>

            <div className="flex items-center gap-1.5">
              <span
                className={`inline-flex items-center rounded-full px-2.5 py-1 text-[10px] font-semibold tracking-wide ${
                  isSelected ? 'bg-white/20 text-white' : getStatusBadgeClass(status)
                }`}
              >
                {status}
              </span>
              {viewVersion?.isActive ? (
                <span
                  className={`inline-flex items-center rounded-full px-2.5 py-1 text-[10px] font-semibold tracking-wide ${
                    isSelected ? 'bg-emerald-500/30 text-emerald-100' : 'bg-emerald-100 text-emerald-800'
                  }`}
                >
                  ACTIVE
                </span>
              ) : null}
            </div>
          </div>

          <div className={`rounded-xl border p-2 ${isSelected ? 'border-white/10 bg-white/5' : 'border-slate-200 bg-slate-50/80'}`}>
            <div className="flex flex-wrap gap-2">
              <Tooltip content="View document">
                <Button
                  variant="ghost"
                  className={actionButtonClass}
                  aria-label="View document"
                  disabled={!canView || !viewVersion || isVersionActionPending('preview', viewVersion.id)}
                  onClick={(event) => {
                    event.stopPropagation();
                    if (!viewVersion) return;
                    handleViewInline(viewVersion);
                  }}
                >
                  <span className={actionLabelClass}>
                    <Eye size={14} />
                    View
                  </span>
                </Button>
              </Tooltip>

              {canEdit ? (
                <Tooltip content="Edit document">
                  <Button
                    variant="ghost"
                    className={actionButtonClass}
                    aria-label="Edit document"
                    onClick={(event) => {
                      event.stopPropagation();
                      handleEditDocument(documentId);
                    }}
                  >
                    <span className={actionLabelClass}>
                      <Pencil size={14} />
                      Edit
                    </span>
                  </Button>
                </Tooltip>
              ) : null}

              {canModerate && inReviewVersion ? (
                <>
                  <Tooltip content="Approve">
                    <Button
                      variant="secondary"
                      className={actionButtonClass}
                      aria-label="Approve"
                      disabled={isVersionActionPending('approve', inReviewVersion.id)}
                      onClick={(event) => {
                        event.stopPropagation();
                        void onVersionAction('approve', inReviewVersion.id);
                      }}
                    >
                      <span className={actionLabelClass}>
                        {isVersionActionPending('approve', inReviewVersion.id) ? (
                          <Loader2 size={14} className="animate-spin" />
                        ) : (
                          <Check size={14} />
                        )}
                        Approve
                      </span>
                    </Button>
                  </Tooltip>
                  <Tooltip content="Reject">
                    <Button
                      variant="danger"
                      className={actionButtonClass}
                      aria-label="Reject"
                      disabled={isVersionActionPending('reject', inReviewVersion.id)}
                      onClick={(event) => {
                        event.stopPropagation();
                        void onVersionAction('reject', inReviewVersion.id);
                      }}
                    >
                      <span className={actionLabelClass}>
                        {isVersionActionPending('reject', inReviewVersion.id) ? (
                          <Loader2 size={14} className="animate-spin" />
                        ) : (
                          <X size={14} />
                        )}
                        Reject
                      </span>
                    </Button>
                  </Tooltip>
                </>
              ) : null}

              <Tooltip content={canExport ? 'Export PDF' : 'Only active versions can be exported'}>
                <Button
                  variant="ghost"
                  className={actionButtonClass}
                  aria-label="Export PDF"
                  disabled={!canExport || !viewVersion || isVersionActionPending('export', viewVersion.id)}
                  onClick={(event) => {
                    event.stopPropagation();
                    if (!viewVersion) return;
                    void handleExportPdf(viewVersion);
                  }}
                >
                  <span className={actionLabelClass}>
                    {viewVersion && isVersionActionPending('export', viewVersion.id) ? (
                      <Loader2 size={14} className="animate-spin" />
                    ) : (
                      <FileDown size={14} />
                    )}
                    Export PDF
                  </span>
                </Button>
              </Tooltip>
            </div>
          </div>
        </div>
      </>
    );

    if (!asButton) {
      return <article className={cardClass}>{cardContent}</article>;
    }

    return (
      <article
        className={cardClass}
        onClick={() => {
          selectDocument(documentId);
        }}
      >
        {cardContent}
      </article>
    );
  };

  const handleCreateDocument = createDocumentForm.handleSubmit(async (values) => {
    const plainText = stripHtmlToPlainText(values.content);
    if (!plainText) {
      createDocumentForm.setError('content', {
        type: 'manual',
        message: 'Document content is required',
      });
      return;
    }

    try {
      const file = new File([plainText], `${values.title}.txt`, {
        type: 'text/plain',
      });

      const formData = new FormData();
      formData.append('title', values.title);
      formData.append('file', file);

      const result = await documentsApi.createDocument(formData);

      addCreatedDocument(result);
      addTrackedDocumentId(result.id);
      markDocumentLoaded(result.id, result.title);
      await refreshDocument(result.id);
      setSelectedDocumentId(result.id);
      createVersionForm.setValue('documentId', result.id);
      setShowCreateDocumentForm(false);
      addActivity({
        action: 'CREATE_DOCUMENT',
        description: `Created document ${result.title}`,
        role: user!.role,
        userId: user!.id,
        username: user!.username,
      });
      createDocumentForm.reset({
        title: '',
        content: '',
      });
      notifySuccess('documents', 'create-document-success', 'Document created', result.id);
    } catch (error) {
      notifyError('documents', 'create-document-error', error);
    }
  });

  const handleCreateVersion = createVersionForm.handleSubmit(async (values) => {
    markDocumentLoaded(values.documentId);

    const plainText = stripHtmlToPlainText(values.content);
    if (!plainText) {
      createVersionForm.setError('content', {
        type: 'manual',
        message: 'Version content is required',
      });
      return;
    }

    try {
      const file = new File([plainText], 'version.txt', {
        type: 'text/plain',
      });
      await versionsApi.createVersion(values.documentId, file);
      addActivity({
        action: 'CREATE_VERSION',
        description: `Created new version for document ${values.documentId}`,
        role: user!.role,
        userId: user!.id,
        username: user!.username,
      });
      await refreshDocument(values.documentId);
      setSelectedDocumentId(values.documentId);
      createVersionForm.reset({
        documentId: values.documentId,
        content: '',
      });
      notifySuccess('documents', 'create-version-success', 'Version created', values.documentId);
    } catch (error) {
      notifyError('documents', 'create-version-error', error, values.documentId);
    }
  });

  const onVersionAction = async (action: 'submit' | 'approve' | 'reject', versionId: string) => {
    const pendingKey = makeVersionActionKey(action, versionId);
    if (pendingVersionActions[pendingKey]) {
      return;
    }

    setPendingVersionActions((prev) => ({
      ...prev,
      [pendingKey]: true,
    }));

    if (selectedDocumentId) {
      markDocumentLoaded(selectedDocumentId);
    }

    try {
      if (action === 'submit') {
        await versionsApi.submit(versionId);
        addActivity({
          action: 'SUBMIT_FOR_REVIEW',
          description: `Submitted version ${versionId} for review`,
          role: user!.role,
          userId: user!.id,
          username: user!.username,
        });
        notifySuccess('documents', 'submit-version-success', 'Version submitted for review', versionId);
      }
      if (action === 'approve') {
        await versionsApi.approve(versionId);
        addActivity({
          action: 'APPROVE_VERSION',
          description: `Approved version ${versionId}`,
          role: user!.role,
          userId: user!.id,
          username: user!.username,
        });
        notifySuccess('documents', 'approve-version-success', 'Version approved', versionId);
      }
      if (action === 'reject') {
        await versionsApi.reject(versionId);
        addActivity({
          action: 'REJECT_VERSION',
          description: `Rejected version ${versionId}`,
          role: user!.role,
          userId: user!.id,
          username: user!.username,
        });
        notifySuccess('documents', 'reject-version-success', 'Version rejected', versionId);
      }
      await refreshDocument(selectedDocumentId);
      if (selectedDocumentId) {
        markDocumentLoaded(selectedDocumentId);
      }
    } catch (error) {
      notifyError('documents', 'version-action-error', error, versionId);
    } finally {
      setPendingVersionActions((prev) => ({
        ...prev,
        [pendingKey]: false,
      }));
    }
  };

  const handleCompare = async () => {
    if (!selectedDocumentId) return;

    if (isComparePending) {
      return;
    }

    setIsComparePending(true);

    markDocumentLoaded(selectedDocumentId);

    try {
      const result = await documentsApi.compareLatest(selectedDocumentId);
      console.log('COMPARE RESULT:', result);
      setCompareResult(result);
      setCompareResultDocumentId(selectedDocumentId);
      notifySuccess('documents', 'compare-success', 'Compare loaded', selectedDocumentId);
    } catch (error) {
      notifyError('documents', 'compare-error', error, selectedDocumentId);
    } finally {
      setIsComparePending(false);
    }
  };

  const handleExportPdf = async (version: VersionResponse) => {
    if (!version.isActive) {
      notifyErrorMessage(
        'documents',
        'export-not-active',
        'You cannot export versions that are not active',
      );
      return;
    }

    const versionId = version.id;
    const pendingKey = makeVersionActionKey('export', versionId);
    if (pendingVersionActions[pendingKey]) {
      return;
    }

    setPendingVersionActions((prev) => ({
      ...prev,
      [pendingKey]: true,
    }));

    if (selectedDocumentId) {
      markDocumentLoaded(selectedDocumentId);
    }

    try {
      const blob = await versionsApi.exportPdf(versionId);
      downloadBlob(blob, `document-${versionId}.pdf`);
      addActivity({
        action: 'EXPORT_PDF',
        description: `Exported PDF for version ${versionId}`,
        role: user!.role,
        userId: user!.id,
        username: user!.username,
      });
    } catch (error) {
      notifyError('documents', 'export-pdf-error', error, versionId);
    } finally {
      setPendingVersionActions((prev) => ({
        ...prev,
        [pendingKey]: false,
      }));
    }
  };

  const handlePdfPreview = async (versionId: string, openViewer = false) => {
    const pendingKey = makeVersionActionKey('preview', versionId);
    if (pendingVersionActions[pendingKey]) {
      return;
    }

    setPendingVersionActions((prev) => ({
      ...prev,
      [pendingKey]: true,
    }));

    if (selectedDocumentId) {
      markDocumentLoaded(selectedDocumentId);
    }

    console.log('OPENING VERSION:', versionId);

    try {
      const blob = await versionsApi.exportPdf(versionId);
      console.log('BLOB:', blob);

      if (!(blob instanceof Blob)) {
        throw new Error('PDF preview did not return a Blob');
      }

      if (blob.size === 0) {
        throw new Error('PDF preview returned an empty file');
      }

      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
      const url = URL.createObjectURL(blob);
      setPreviewUrl(url);
      setPreviewVersionId(versionId);
      if (openViewer) {
        setIsViewerOpen(true);
      }
    } catch (error) {
      notifyError('documents', 'preview-error', error, versionId);
    } finally {
      setPendingVersionActions((prev) => ({
        ...prev,
        [pendingKey]: false,
      }));
    }
  };

  const handleViewInline = async (version: VersionResponse) => {
    if (selectedDocumentId) {
      markDocumentLoaded(selectedDocumentId);
    }

    const requestId = inlineContentRequestRef.current + 1;
    inlineContentRequestRef.current = requestId;

    setSelectedInlineVersion(null);
    setInlineContentError(null);
    setInlineEditorError(null);
    setIsInlineEditMode(false);
    setInlineEditorContent('');
    setInlineEditorInitialContent('');
    setInlineEditVersionId('');
    setIsInlineContentLoading(true);
    setIsInlineViewerOpen(true);

    try {
      const versionContent = await versionsApi.getVersionContent(version.id);

      if (inlineContentRequestRef.current !== requestId) {
        return;
      }

      setSelectedInlineVersion(versionContent);
    } catch {
      if (inlineContentRequestRef.current !== requestId) {
        return;
      }

      setSelectedInlineVersion(null);
      setInlineContentError('Could not load content');
    } finally {
      if (inlineContentRequestRef.current === requestId) {
        setIsInlineContentLoading(false);
      }
    }
  };

  const handleEditDraft = async (version: VersionResponse) => {
    if (!canEditDraft(version)) {
      return;
    }

    if (selectedDocumentId) {
      markDocumentLoaded(selectedDocumentId);
    }

    const requestId = inlineContentRequestRef.current + 1;
    inlineContentRequestRef.current = requestId;

    setSelectedInlineVersion(null);
    setInlineContentError(null);
    setInlineEditorError(null);
    setIsInlineContentLoading(true);
    setIsInlineViewerOpen(true);
    setIsInlineEditMode(true);
    setInlineEditVersionId(version.id);

    try {
      const versionContent = await versionsApi.getVersionContent(version.id);

      if (inlineContentRequestRef.current !== requestId) {
        return;
      }

      setSelectedInlineVersion(versionContent);
      setInlineEditorContent(versionContent.content);
      setInlineEditorInitialContent(versionContent.content);
    } catch {
      if (inlineContentRequestRef.current !== requestId) {
        return;
      }

      setSelectedInlineVersion(null);
      setInlineContentError('Could not load content');
    } finally {
      if (inlineContentRequestRef.current === requestId) {
        setIsInlineContentLoading(false);
      }
    }
  };

  const updateDraftFile = async (versionId: string, content: string) => {
    const plainText = stripHtmlToPlainText(content);

    if (!plainText) {
      setInlineEditorError('Draft content cannot be empty');
      return false;
    }

    const file = new File([plainText], 'draft.txt', {
      type: 'text/plain',
    });

    await versionsApi.updateDraftFile(versionId, file);
    notifySuccess('documents', 'update-draft-success', 'Draft updated successfully', versionId);
    return true;
  };

  const handleSaveDraft = async () => {
    if (!inlineEditVersionId || isInlineSavePending || isInlineSubmitPending) {
      return;
    }

    setInlineEditorError(null);
    setIsInlineSavePending(true);

    try {
      const saved = await updateDraftFile(inlineEditVersionId, inlineEditorContent);
      if (!saved) {
        return;
      }

      if (selectedDocumentId) {
        await refreshDocument(selectedDocumentId);
      }
      closeInlineViewer();
    } catch (error) {
      notifyError('documents', 'update-draft-error', error, inlineEditVersionId);
    } finally {
      setIsInlineSavePending(false);
    }
  };

  const handleSubmitDraftForReview = async () => {
    if (!inlineEditVersionId || isInlineSavePending || isInlineSubmitPending) {
      return;
    }

    const nextPlain = stripHtmlToPlainText(inlineEditorContent);
    if (!nextPlain) {
      setInlineEditorError('Draft content cannot be empty');
      return;
    }

    setInlineEditorError(null);
    setIsInlineSubmitPending(true);

    try {
      const wasModified = stripHtmlToPlainText(inlineEditorContent) !== stripHtmlToPlainText(inlineEditorInitialContent);

      if (wasModified) {
        const saved = await updateDraftFile(inlineEditVersionId, inlineEditorContent);
        if (!saved) {
          return;
        }
      }

      await versionsApi.submit(inlineEditVersionId);
      notifySuccess('documents', 'submit-version-success', 'Version submitted for review', inlineEditVersionId);

      if (selectedDocumentId) {
        await refreshDocument(selectedDocumentId);
      }
      closeInlineViewer();
    } catch (error) {
      notifyError('documents', 'submit-version-error', error, inlineEditVersionId);
    } finally {
      setIsInlineSubmitPending(false);
    }
  };

  function closeInlineViewer() {
    inlineContentRequestRef.current += 1;
    setIsInlineViewerOpen(false);
    setIsInlineEditMode(false);
    setIsInlineContentLoading(false);
    setInlineContentError(null);
    setSelectedInlineVersion(null);
    setInlineEditorContent('');
    setInlineEditorInitialContent('');
    setInlineEditorError(null);
    setInlineEditVersionId('');
    setIsInlineSavePending(false);
    setIsInlineSubmitPending(false);
  }

  const loadComments = async (version: VersionResponse) => {
    if (!getCommentsAllowed(version)) {
      notifyErrorMessage('documents', 'comments-not-allowed', 'You do not have access to comments for this version.');
      return;
    }

    setCommentVersionId(version.id);
    const requestId = commentsRequestRef.current + 1;
    commentsRequestRef.current = requestId;

    const versionId = version.id;
    const pendingKey = makeVersionActionKey('comments', versionId);
    if (pendingVersionActions[pendingKey]) {
      return;
    }

    setPendingVersionActions((prev) => ({
      ...prev,
      [pendingKey]: true,
    }));

    if (selectedDocumentId) {
      markDocumentLoaded(selectedDocumentId);
    }

    try {
      setCommentLoading(true);
      const comments = await commentsApi.getComments(versionId);
      console.log('COMMENTS RESPONSE:', comments);
      if (commentsRequestRef.current !== requestId) {
        return;
      }

      setCommentsByVersionId((prev) => ({
        ...prev,
        [versionId]: comments,
      }));
    } catch (error) {
      notifyError('documents', 'load-comments-error', error, versionId);
    } finally {
      if (commentsRequestRef.current === requestId) {
        setCommentLoading(false);
      }
      setPendingVersionActions((prev) => ({
        ...prev,
        [pendingKey]: false,
      }));
    }
  };

  const handleAddComment = commentForm.handleSubmit(async (values) => {
    if (!commentVersionId) {
      notifyErrorMessage('documents', 'comment-version-missing', 'Select a version to comment on.');
      return;
    }

    if (selectedDocumentId) {
      markDocumentLoaded(selectedDocumentId);
    }

    try {
      await commentsApi.addComment(commentVersionId, values);
      const updatedComments = await commentsApi.getComments(commentVersionId);
      console.log('COMMENTS RESPONSE:', updatedComments);
      setCommentsByVersionId((prev) => ({
        ...prev,
        [commentVersionId]: updatedComments,
      }));
      commentForm.reset({ content: '' });
      addActivity({
        action: 'ADD_COMMENT',
        description: `Added comment to version ${commentVersionId}`,
        role: user!.role,
        userId: user!.id,
        username: user!.username,
      });
      notifySuccess('documents', 'add-comment-success', 'Comment added', commentVersionId);
    } catch (error) {
      notifyError('documents', 'add-comment-error', error, commentVersionId);
    }
  });

  return (
    <>
      <div className="space-y-6">
      <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold text-slate-900">Role-based document workspace</h2>
            <p className="text-sm text-slate-600">
              Current role: {user?.role ? <RoleBadge role={user.role} /> : null}
            </p>
          </div>
        </div>

        <div className="mt-4 grid gap-3 md:grid-cols-[minmax(0,1fr)_auto] md:items-end">
          <label className="space-y-1.5">
            <span className="text-sm font-medium text-slate-700">Document</span>
            <select
              value={selectedDocumentId}
              onChange={(event) => {
                const documentId = event.target.value;
                if (!documentId) return;
                addTrackedDocumentId(documentId);
                selectDocument(documentId);
              }}
              className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 shadow-sm transition focus:border-slate-400 focus:outline-none focus:ring-2 focus:ring-cyan-200"
              disabled={!availableDocuments.length}
            >
              <option value="">{availableDocuments.length ? 'Choose a document' : 'No documents available'}</option>
              {availableDocuments.map((document) => (
                <option
                  key={document.id}
                  value={document.id}
                  className={selectedDocumentId === document.id ? 'font-semibold text-slate-900' : 'text-slate-700'}
                >
                  {document.title} - ID: {document.id} - v{document.versionNumber ?? '-'} - {document.status}
                </option>
              ))}
            </select>
          </label>

          <div className="rounded-md border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-600">
            {selectedDocumentId ? (
              <>
                Selected document: <span className="font-medium text-slate-900">{selectedDocumentId}</span>
              </>
            ) : (
              'Select a document to load its versions and content.'
            )}
          </div>
        </div>

        {!realDocuments.length ? (
          <div className="empty-state-enter mt-4 rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center">
            <div className="mx-auto flex w-full max-w-md flex-col items-center gap-3">
              <div className="rounded-full bg-white p-3 shadow-sm">
                <FileText size={24} className="text-slate-600" />
              </div>
              <p className="text-base font-semibold text-slate-800">No documents yet</p>
              <p className="text-sm text-slate-600">Create a document to get started.</p>
            </div>
            <div className="mt-5 flex flex-wrap items-center justify-center gap-2">
              <Button
                variant="secondary"
                onClick={() => {
                  setShowCreateDocumentForm(true);
                  createSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
                }}
                disabled={!(user?.role === 'AUTHOR' || user?.role === 'ADMIN')}
              >
                Create Document
              </Button>
              <Button
                variant="secondary"
                onClick={() => {
                  const firstAvailableDocument = availableDocuments[0];
                  if (firstAvailableDocument) {
                    addTrackedDocumentId(firstAvailableDocument.id);
                    selectDocument(firstAvailableDocument.id);
                  }
                }}
                disabled={!availableDocuments.length}
              >
                Open first document
              </Button>
            </div>
          </div>
        ) : null}

        <div className="page-stagger-children mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {availableDocuments.map((document) => (
            <div
              key={document.id}
              className="relative min-h-[168px]"
              ref={(node) => {
                trackedCardRefs.current[document.id] = node;
              }}
            >
              {renderDocumentCard(
                document.id,
                selectedDocumentId === document.id,
                true,
                highlightedDocumentId === document.id,
              )}
              <div
                className={`absolute inset-0 transition-opacity duration-150 ${
                  documentMap[document.id]?.loading &&
                  !documentMap[document.id]?.active &&
                  !(documentMap[document.id]?.versions?.length > 0)
                    ? 'opacity-100'
                    : 'pointer-events-none opacity-0'
                }`}
              >
                {renderDocumentCardSkeleton()}
              </div>
            </div>
          ))}
        </div>
      </section>

      {(user?.role === 'AUTHOR' || user?.role === 'ADMIN') && (
        <section ref={createSectionRef} className="space-y-6">
          {(user?.role === 'AUTHOR' || user?.role === 'ADMIN') ? (
          <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 className="text-base font-semibold text-slate-900">Create Document</h3>
                <p className="text-sm text-slate-600">Write initial content and upload as the first document file.</p>
              </div>
              <Button
                variant="secondary"
                onClick={() => setShowCreateDocumentForm((prev) => !prev)}
              >
                {showCreateDocumentForm ? 'Close' : 'Create Document'}
              </Button>
            </div>

            {showCreateDocumentForm ? (
              <form onSubmit={handleCreateDocument} className="mt-4 space-y-4">
                <TextInput
                  label="Document Title"
                  error={createDocumentForm.formState.errors.title?.message}
                  {...createDocumentForm.register('title')}
                />
                {hasCreateDocumentTitle ? (
                  <div className="space-y-1.5">
                    <p className="text-sm font-medium text-slate-700">Document content</p>
                    <RichTextEditor
                      value={createDocumentContent}
                      onChange={(value) => createDocumentForm.setValue('content', value, { shouldValidate: true })}
                      placeholder="Write the initial document content"
                    />
                    {createDocumentForm.formState.errors.content?.message ? (
                      <p className="text-xs text-red-600">{createDocumentForm.formState.errors.content.message}</p>
                    ) : null}
                  </div>
                ) : null}
                <Button type="submit" disabled={!canSubmitCreateDocument}>
                  {createDocumentForm.formState.isSubmitting ? 'Creating...' : 'Create Document'}
                </Button>
              </form>
            ) : null}
          </div>
          ) : null}

          <form onSubmit={handleCreateVersion} className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Create Version</h3>
            <p className="mt-1 text-sm text-slate-600">Create a version for an existing document.</p>
            <div className="mt-4 space-y-4">
              <label className="space-y-1.5">
                <span className="text-sm font-medium text-slate-700">Document</span>
                <select
                  value={createVersionDocumentId}
                  onChange={(event) => {
                    const documentId = event.target.value;
                    createVersionForm.setValue('documentId', documentId, { shouldValidate: true });
                    if (documentId) {
                      addTrackedDocumentId(documentId);
                      setSelectedDocumentId(documentId);
                    }
                  }}
                  className="w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 shadow-sm transition focus:border-slate-400 focus:outline-none focus:ring-2 focus:ring-cyan-200"
                >
                  <option value="">{createVersionDocuments.length ? 'Choose a document' : 'No documents available'}</option>
                  {createVersionDocuments.map((document) => (
                    <option key={document.id} value={document.id}>
                      {document.title} - ID: {document.id} - {document.status}{document.isActive ? ' (ACTIVE)' : ''}
                    </option>
                  ))}
                </select>
                {createVersionForm.formState.errors.documentId?.message ? (
                  <p className="text-xs text-red-600">{createVersionForm.formState.errors.documentId.message}</p>
                ) : null}
              </label>

              {createVersionDocumentId ? (
                <div className="space-y-1.5">
                  <p className="text-sm font-medium text-slate-700">Version content</p>
                  <RichTextEditor
                    value={createVersionContent}
                    onChange={(value) => createVersionForm.setValue('content', value, { shouldValidate: true })}
                    placeholder="Create a new revision"
                  />
                  {createVersionForm.formState.errors.content?.message ? (
                    <p className="text-xs text-red-600">{createVersionForm.formState.errors.content.message}</p>
                  ) : null}
                </div>
              ) : (
                <p className="text-xs text-slate-500">Select a document to create a version.</p>
              )}

              <Button type="submit" disabled={!canSubmitCreateVersion}>
                {createVersionForm.formState.isSubmitting ? 'Creating...' : 'Create Version'}
              </Button>
            </div>
          </form>
        </section>
      )}

      <section className="grid gap-6 lg:grid-cols-[minmax(0,1.65fr)_minmax(380px,1fr)]">
        <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="text-base font-semibold text-slate-900">Versions</h3>
            <Button
              variant="secondary"
              onClick={handleCompare}
              disabled={!selectedDocumentId || isComparePending}
              className="min-w-[130px]"
            >
              {renderPendingLabel(isComparePending, 'Compare latest', 'Loading...')}
            </Button>
          </div>

          {!selectedDocumentId ? (
            <LoadingBlock label="Track a document ID to load versions" />
          ) : selectedData?.loading ? (
            <LoadingBlock label="Loading document versions..." />
          ) : (
            <div className="mt-4 overflow-x-auto">
              <table className="w-full border-collapse text-left text-sm">
                <thead>
                  <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                    <th className="px-2 py-2">Version</th>
                    <th className="px-2 py-2">Status</th>
                    <th className="px-2 py-2">Created By</th>
                    <th className="px-2 py-2">Created At</th>
                    <th className="px-2 py-2">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleVersions.map((version) => (
                    <tr key={version.id} className="border-b border-slate-100 align-top">
                      <td className="px-2 py-2">v{version.versionNumber}</td>
                      <td className="px-2 py-2">
                        <div className="flex flex-wrap items-center gap-1">
                          <span
                            className={`rounded px-2 py-1 text-xs ${
                              version.status === 'APPROVED'
                                ? 'bg-emerald-100 text-emerald-800'
                                : version.status === 'IN_REVIEW'
                                  ? 'bg-amber-100 text-amber-800'
                                  : version.status === 'REJECTED'
                                    ? 'bg-red-100 text-red-800'
                                    : 'bg-slate-100 text-slate-700'
                            }`}
                          >
                            {version.status}
                          </span>
                          {version.isActive ? (
                            <span className="rounded bg-emerald-100 px-2 py-1 text-xs text-emerald-800">ACTIVE</span>
                          ) : null}
                        </div>
                      </td>
                      <td className="px-2 py-2">{version.createdBy.username}</td>
                      <td className="px-2 py-2">{formatDate(version.createdAt)}</td>
                      <td className="px-2 py-2">
                        <div className="flex flex-wrap gap-1">
                          <Button
                            variant="ghost"
                            onClick={() => void handleViewInline(version)}
                            disabled={
                              isVersionActionPending('preview', version.id) ||
                              !canAccessVersionByRole(user?.role, user?.id, version)
                            }
                            className="min-w-[110px]"
                          >
                            {renderPendingLabel(
                              isVersionActionPending('preview', version.id),
                              'View inline',
                              'Loading...',
                            )}
                          </Button>
                          <Tooltip content={version.isActive ? 'Export PDF' : 'Only active versions can be exported'}>
                            <Button
                              variant="ghost"
                              onClick={() => void handleExportPdf(version)}
                              disabled={!version.isActive || isVersionActionPending('export', version.id)}
                              className="min-w-[110px]"
                            >
                              {renderPendingLabel(
                                isVersionActionPending('export', version.id),
                                'Export PDF',
                                'Loading...',
                              )}
                            </Button>
                          </Tooltip>
                          {(user?.role === 'AUTHOR' || user?.role === 'ADMIN') && version.status === 'DRAFT' ? (
                            <Button
                              variant="secondary"
                              onClick={() => void onVersionAction('submit', version.id)}
                              disabled={isVersionActionPending('submit', version.id)}
                              className="min-w-[110px]"
                            >
                              {renderPendingLabel(
                                isVersionActionPending('submit', version.id),
                                'Submit',
                                'Loading...',
                              )}
                            </Button>
                          ) : null}
                          {canEditDraft(version) ? (
                            <Button
                              variant="secondary"
                              onClick={() => void handleEditDraft(version)}
                              disabled={isInlineContentLoading && inlineEditVersionId === version.id}
                              className="min-w-[118px]"
                            >
                              Edit Draft
                            </Button>
                          ) : null}
                          {(user?.role === 'REVIEWER' || user?.role === 'ADMIN') && version.status === 'IN_REVIEW' ? (
                            <>
                              <Button
                                variant="secondary"
                                onClick={() => void onVersionAction('approve', version.id)}
                                disabled={isVersionActionPending('approve', version.id)}
                                className="min-w-[118px]"
                              >
                                {renderPendingLabel(
                                  isVersionActionPending('approve', version.id),
                                  'Approve',
                                  'Approving...',
                                )}
                              </Button>
                              <Button
                                variant="danger"
                                onClick={() => void onVersionAction('reject', version.id)}
                                disabled={isVersionActionPending('reject', version.id)}
                                className="min-w-[118px]"
                              >
                                {renderPendingLabel(
                                  isVersionActionPending('reject', version.id),
                                  'Reject',
                                  'Rejecting...',
                                )}
                              </Button>
                            </>
                          ) : null}
                          {getCommentsAllowed(version) && (
                            <Button
                              variant="secondary"
                              onClick={() => void loadComments(version)}
                              disabled={isVersionActionPending('comments', version.id)}
                              className="min-w-[110px]"
                            >
                              {renderPendingLabel(
                                isVersionActionPending('comments', version.id),
                                'Comments',
                                'Loading...',
                              )}
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {!visibleVersions.length ? (
                <p className="mt-4 text-sm text-slate-500">No visible versions for your role or selected document.</p>
              ) : null}
            </div>
          )}

          {compareResult ? (
            <div className="mt-4 rounded-lg border border-cyan-200 bg-cyan-50 p-4 text-sm shadow-sm">
              <p className="font-semibold text-cyan-900">
                Comparing v{compareResult.version1Number} and v{compareResult.version2Number}
              </p>
              <div className="mt-3 grid gap-3 xl:grid-cols-2">
                <article className="rounded-xl border border-cyan-200 bg-white p-4 shadow-sm">
                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{compareResult.fileName1}</p>
                  <div className="mt-3 max-h-72 overflow-auto pr-2">
                    {renderVersionContent(compareResult.fileName1, compareResult.version1Content)}
                  </div>
                </article>
                <article className="rounded-xl border border-cyan-200 bg-white p-4 shadow-sm">
                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{compareResult.fileName2}</p>
                  <div className="mt-3 max-h-72 overflow-auto pr-2">
                    {renderVersionContent(compareResult.fileName2, compareResult.version2Content)}
                  </div>
                </article>
              </div>
            </div>
          ) : null}
        </div>

        <div className="space-y-6">
          <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Selected document details</h3>
            {!selectedDocumentId ? (
              <p className="mt-2 text-sm text-slate-500">Select a document to load its details.</p>
            ) : !selectedData || selectedData.loading ? (
              <LoadingBlock label="Loading document details..." />
            ) : (
              <div className="mt-3 space-y-4">
                {(() => {
                  const activeVersion = getLatestVersion(selectedDocumentId);
                  const inReviewVersion = getInReviewVersion(selectedDocumentId);

                  return (
                    <>
                      <div className="rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
                        <div className="grid gap-2 sm:grid-cols-2">
                          <p><span className="font-semibold">Title:</span> {activeVersion?.document?.title || getDocumentTitle(selectedDocumentId)}</p>
                          <p><span className="font-semibold">Document ID:</span> {selectedDocumentId}</p>
                          <p><span className="font-semibold">Version:</span> {activeVersion ? `v${activeVersion.versionNumber}` : '-'}</p>
                          <p>
                            <span className="font-semibold">Status:</span>{' '}
                            {activeVersion ? getVersionStatusLabel(activeVersion) : getDocumentStatus(selectedDocumentId)}
                          </p>
                          <p><span className="font-semibold">File:</span> {activeVersion?.fileName || '-'}</p>
                          <p><span className="font-semibold">Created by:</span> {activeVersion?.createdBy.username || '-'}</p>
                          <p><span className="font-semibold">Created at:</span> {activeVersion ? formatDate(activeVersion.createdAt) : '-'}</p>
                          <p><span className="font-semibold">Approved at:</span> {activeVersion?.approvedAt ? formatDate(activeVersion.approvedAt) : '-'}</p>
                        </div>
                      </div>

                      {compareResult ? (
                        <div className="rounded-lg border border-cyan-200 bg-cyan-50 p-3 text-sm">
                          <p className="font-semibold text-cyan-900">
                            Reviewer content for v{compareResult.version1Number} and v{compareResult.version2Number}
                          </p>
                          <div className="mt-2 grid gap-3">
                            <article className="rounded border border-cyan-200 bg-white p-2">
                              <p className="text-xs font-semibold text-slate-500">{compareResult.fileName1}</p>
                              <pre className="mt-1 max-h-48 overflow-auto whitespace-pre-wrap break-words text-xs text-slate-700">
                                {compareResult.version1Content}
                              </pre>
                            </article>
                            <article className="rounded border border-cyan-200 bg-white p-2">
                              <p className="text-xs font-semibold text-slate-500">{compareResult.fileName2}</p>
                              <pre className="mt-1 max-h-48 overflow-auto whitespace-pre-wrap break-words text-xs text-slate-700">
                                {compareResult.version2Content}
                              </pre>
                            </article>
                            {inReviewVersion ? (
                              <p className="text-xs text-slate-500">
                                Latest review version: v{inReviewVersion.versionNumber} · {inReviewVersion.status}
                              </p>
                            ) : null}
                          </div>
                        </div>
                      ) : null}
                    </>
                  );
                })()}
              </div>
            )}
          </section>

          <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <h3 className="text-base font-semibold text-slate-900">Document viewer</h3>
              {selectedFileName ? (
                <span className="rounded-full bg-slate-100 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide text-slate-600">
                  {selectedIsPdf ? 'PDF' : selectedIsText ? 'Text' : 'HTML'}
                </span>
              ) : null}
            </div>

            <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50/80">
              <div className="h-[70vh] overflow-auto p-5 sm:p-6 lg:p-8">
                {previewUrl && !isViewerOpen ? (
                  <div className="space-y-4">
                    <div className="flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500">
                      <p>Version: {previewVersionId}</p>
                      <p className="truncate">{selectedFileName || 'PDF preview'}</p>
                    </div>
                    <iframe
                      src={previewUrl}
                      className="h-[70vh] w-full rounded-xl border border-slate-200 bg-white shadow-sm"
                      title="PDF preview"
                    />
                  </div>
                ) : previewUrl && isViewerOpen ? (
                  <div className="rounded-xl border border-dashed border-slate-300 bg-white p-5 text-sm leading-6 text-slate-600 shadow-sm">
                    Preview is opened in fullscreen mode.
                  </div>
                ) : selectedRenderableContent !== undefined ? (
                  <div className="space-y-5">
                    <p className="text-sm leading-6 text-slate-600">
                      The current document content is displayed below with a scrollable reading area.
                    </p>
                    <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
                      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                        {selectedRenderableFileName || 'Document preview'}
                      </p>
                      <div className="mt-4 max-h-[80vh] overflow-auto pr-3">
                        {renderVersionContent(selectedRenderableFileName, selectedRenderableContent)}
                      </div>
                    </article>
                  </div>
                ) : (
                  <div className="rounded-xl border border-dashed border-slate-300 bg-white p-5 text-sm leading-6 text-slate-600 shadow-sm">
                    Text preview unavailable for this version.
                  </div>
                )}
              </div>
            </div>
          </section>

          <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Version comments</h3>
            {commentLoading ? (
              <LoadingBlock label="Loading comments..." />
            ) : (
              <>
                <p className="mt-2 text-xs text-slate-500">Selected version: {commentVersionId || '-'}</p>
                <div className="mt-3 max-h-48 space-y-2 overflow-auto pr-1">
                  {commentList.map((comment) => (
                    <article key={comment.id} className="rounded border border-slate-200 p-2 text-sm">
                      <p className="text-slate-700">{comment.content}</p>
                      <p className="mt-1 text-xs text-slate-500">
                        {comment.user.username} - {formatDate(comment.createdAt)}
                      </p>
                    </article>
                  ))}
                  {!commentList.length && commentVersionId ? (
                    <p className="text-sm text-slate-500">No comments for this version.</p>
                  ) : null}
                </div>

                {(user?.role === 'REVIEWER' || user?.role === 'ADMIN') && (
                  <form onSubmit={handleAddComment} className="mt-3 space-y-2">
                    <textarea
                      className="min-h-20 w-full rounded-md border border-slate-300 p-2 text-sm"
                      placeholder="Add review comment"
                      {...commentForm.register('content')}
                    />
                    {commentForm.formState.errors.content?.message ? (
                      <p className="text-xs text-red-600">{commentForm.formState.errors.content.message}</p>
                    ) : null}
                    <Button type="submit" className="w-full" disabled={commentForm.formState.isSubmitting}>
                      Add comment
                    </Button>
                  </form>
                )}
              </>
            )}
          </section>

          <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">My Documents</h3>
            <div className="page-stagger-children mt-3 grid gap-2 text-sm">
              {myDocuments.map((doc) => (
                <div key={doc.id}>{renderDocumentCard(doc.id, selectedDocumentId === doc.id, true, highlightedDocumentId === doc.id)}</div>
              ))}
              {!myDocuments.length ? (
                <p className="text-slate-500">No created documents yet.</p>
              ) : null}
            </div>
          </section>

          <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
            <h3 className="text-base font-semibold text-slate-900">Recently Viewed</h3>
            <div className="page-stagger-children mt-3 grid gap-2 text-sm">
              {recentlyViewedDocuments.map((doc) => (
                <div key={doc.id}>{renderDocumentCard(doc.id, selectedDocumentId === doc.id, true, highlightedDocumentId === doc.id)}</div>
              ))}
              {!recentlyViewedDocuments.length ? (
                <p className="text-slate-500">No recently viewed documents yet.</p>
              ) : null}
            </div>
          </section>
        </div>
      </section>

      {selectedDocumentId ? (
        <div className="flex justify-end">
          <Button variant="ghost" onClick={() => removeTrackedDocumentId(selectedDocumentId)}>
            Remove selected document from workspace
          </Button>
        </div>
      ) : null}
      </div>

      {isInlineViewerOpen ? (
        <div className="fixed inset-0 z-50 flex flex-col bg-black/80">
          <div className="flex items-center justify-between bg-black p-4 text-white">
            <span className="text-sm">
              {selectedInlineVersion
                ? `Version v${selectedInlineVersion.versionNumber} | ${selectedInlineVersion.status}`
                : 'Viewing inline content'}
            </span>

            <button
              type="button"
              onClick={() => {
                closeInlineViewer();
              }}
              className="text-xl font-bold text-white hover:text-red-400"
              aria-label="Close inline preview"
            >
              X
            </button>
          </div>

          <div className="flex-1 overflow-auto bg-white p-6">
            {isInlineContentLoading ? (
              <p className="text-sm text-slate-600">Loading content...</p>
            ) : inlineContentError ? (
              <p className="text-sm text-red-600">Could not load content</p>
            ) : selectedInlineVersion && isInlineEditMode ? (
              <div className="space-y-4">
                <RichTextEditor
                  value={inlineEditorContent}
                  onChange={(value) => {
                    setInlineEditorError(null);
                    setInlineEditorContent(value);
                  }}
                  placeholder="Edit draft content"
                />
                {inlineEditorError ? (
                  <p className="text-sm text-red-600">{inlineEditorError}</p>
                ) : null}
                <div className="flex flex-wrap items-center justify-end gap-2">
                  <Button
                    variant="secondary"
                    onClick={() => {
                      void handleSaveDraft();
                    }}
                    disabled={isInlineSavePending || isInlineSubmitPending}
                    className="min-w-[118px]"
                  >
                    {isInlineSavePending ? 'Saving...' : 'Save Draft'}
                  </Button>
                  <Button
                    variant="secondary"
                    onClick={() => {
                      void handleSubmitDraftForReview();
                    }}
                    disabled={isInlineSavePending || isInlineSubmitPending}
                    className="min-w-[144px]"
                  >
                    {isInlineSubmitPending ? 'Submitting...' : 'Submit for Review'}
                  </Button>
                  <Button
                    variant="ghost"
                    onClick={() => {
                      closeInlineViewer();
                    }}
                    disabled={isInlineSavePending || isInlineSubmitPending}
                    className="min-w-[100px]"
                  >
                    Cancel
                  </Button>
                </div>
              </div>
            ) : selectedInlineVersion ? (
              <pre className="whitespace-pre-wrap font-mono text-sm">{selectedInlineVersion.content}</pre>
            ) : (
              <p className="text-sm text-slate-600">Loading content...</p>
            )}
          </div>
        </div>
      ) : null}

      {isViewerOpen && previewUrl ? (
        <div className="fixed inset-0 z-50 flex flex-col bg-black/80">
          <div className="flex items-center justify-between bg-black p-4 text-white">
            <span className="text-sm">Viewing document (version: {previewVersionId})</span>

            <button
              type="button"
              onClick={() => {
                setIsViewerOpen(false);
                setPreviewUrl('');
              }}
              className="text-xl font-bold text-white hover:text-red-400"
              aria-label="Close fullscreen preview"
            >
              X
            </button>
          </div>

          <div className="flex-1">
            <iframe
              src={previewUrl}
              className="h-full w-full bg-white"
              title="Fullscreen PDF preview"
            />
          </div>
        </div>
      ) : null}
    </>
  );
}
