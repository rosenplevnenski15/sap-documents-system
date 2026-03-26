import { FormEvent, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { api } from "../lib/api";
import type { DocumentResponse, VersionResponse } from "../types";

export default function DocumentRegistryPage() {
  const { session } = useAuth();
  const { showToast } = useToast();
  const token = session!.accessToken;
  const [query, setQuery] = useState("");
  const [mineOnly, setMineOnly] = useState(false);
  const [rows, setRows] = useState<DocumentResponse[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sortBy, setSortBy] = useState("createdAt");
  const [direction, setDirection] = useState<"asc" | "desc">("desc");
  const [totalPages, setTotalPages] = useState(0);
  const [selectedVersions, setSelectedVersions] = useState<VersionResponse[]>([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState("");
  const [compareResult, setCompareResult] = useState("");
  const [loading, setLoading] = useState(false);

  async function searchAt(targetPage: number) {
    setLoading(true);
    try {
      const data = await api.listDocuments(token, query, mineOnly, targetPage, size, sortBy, direction);
      setRows(data.content);
      setTotalPages(data.totalPages);
      setPage(data.number);
      showToast(`Loaded ${data.content.length} documents.`, "success");
    } catch (e) {
      showToast(e instanceof Error ? e.message : "Failed to load documents.", "error");
    } finally {
      setLoading(false);
    }
  }

  async function handleSearch(event?: FormEvent) {
    event?.preventDefault();
    await searchAt(0);
  }

  async function loadDocumentVersions(documentId: string) {
    try {
      const versions = await api.getVersions(documentId, token);
      setSelectedDocumentId(documentId);
      setSelectedVersions(versions);
      showToast(`Loaded ${versions.length} versions for selected document.`, "success");
    } catch (e) {
      showToast(e instanceof Error ? e.message : "Failed to load versions.", "error");
    }
  }

  async function compareLatest(documentId: string) {
    try {
      const value = await api.compareLatest(documentId, token);
      setCompareResult(value);
      showToast("Loaded latest compare result.", "success");
    } catch (e) {
      showToast(e instanceof Error ? e.message : "Failed to compare versions.", "error");
    }
  }

  async function exportActivePdf(documentId: string) {
    try {
      const active = await api.getActiveVersion(documentId, token);
      const pdf = await api.exportPdf(active.id, token);
      const url = URL.createObjectURL(pdf);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `document-${documentId}.pdf`;
      anchor.click();
      URL.revokeObjectURL(url);
      showToast("PDF export downloaded.", "success");
    } catch (e) {
      showToast(e instanceof Error ? e.message : "Failed to export PDF.", "error");
    }
  }

  return (
    <section>
      <h1>Document Registry</h1>
      <p className="muted">Search across document titles and filter by documents created by you.</p>

      <div className="panel">
        <form onSubmit={handleSearch}>
          <input
            placeholder="Search title..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            maxLength={255}
          />
          <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
            <option value="createdAt">Created At</option>
            <option value="title">Title</option>
          </select>
          <select value={direction} onChange={(e) => setDirection(e.target.value as "asc" | "desc")}>
            <option value="desc">Desc</option>
            <option value="asc">Asc</option>
          </select>
          <input
            type="number"
            min={1}
            max={100}
            value={size}
            onChange={(e) => setSize(Number(e.target.value))}
            title="Page size"
          />
          <label className="checkbox-label">
            <input type="checkbox" checked={mineOnly} onChange={(e) => setMineOnly(e.target.checked)} />
            Mine only
          </label>
          <button type="submit" disabled={loading}>
            {loading ? "Loading..." : "Search"}
          </button>
        </form>
      </div>

      <div className="panel">
        <table>
          <thead>
            <tr>
              <th>Title</th>
              <th>Document ID</th>
              <th>Created By</th>
              <th>Created At</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((doc) => (
              <tr key={doc.id}>
                <td>{doc.title}</td>
                <td>{doc.id}</td>
                <td>{doc.createdBy?.username}</td>
                <td>{new Date(doc.createdAt).toLocaleString()}</td>
                <td className="row-actions">
                  <button onClick={() => loadDocumentVersions(doc.id)}>Versions</button>
                  <button onClick={() => compareLatest(doc.id)}>Compare</button>
                  <button onClick={() => exportActivePdf(doc.id)}>Export PDF</button>
                </td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan={5} className="muted">
                  No documents loaded yet.
                </td>
              </tr>
            )}
          </tbody>
        </table>
        <div className="row-actions">
          <button
            onClick={() => void searchAt(Math.max(page - 1, 0))}
            disabled={page <= 0 || loading}
          >
            Previous
          </button>
          <span className="muted">
            Page {page + 1} / {Math.max(totalPages, 1)}
          </span>
          <button
            onClick={() => void searchAt(page + 1)}
            disabled={loading || (totalPages > 0 && page + 1 >= totalPages)}
          >
            Next
          </button>
        </div>
      </div>

      {selectedDocumentId && (
        <div className="panel">
          <h3>Versions for {selectedDocumentId}</h3>
          <table>
            <thead>
              <tr>
                <th>Version</th>
                <th>Status</th>
                <th>File</th>
              </tr>
            </thead>
            <tbody>
              {selectedVersions.map((version) => (
                <tr key={version.id}>
                  <td>{version.versionNumber}</td>
                  <td>{version.status}</td>
                  <td>{version.fileName}</td>
                </tr>
              ))}
              {selectedVersions.length === 0 && (
                <tr>
                  <td colSpan={3} className="muted">
                    No versions found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {compareResult && (
        <div className="panel">
          <h3>Compare Result</h3>
          <pre>{compareResult}</pre>
        </div>
      )}
    </section>
  );
}
