import { FormEvent, useState } from "react";
import { api } from "../lib/api";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { isUuid } from "../lib/validation";
import type { VersionResponse } from "../types";

export default function OperationsPage() {
  const { session } = useAuth();
  const { showToast } = useToast();
  const token = session!.accessToken;
  const role = session!.user.role;

  const [title, setTitle] = useState("");
  const [docFile, setDocFile] = useState<File | null>(null);
  const [documentId, setDocumentId] = useState("");
  const [versionFile, setVersionFile] = useState<File | null>(null);
  const [versionId, setVersionId] = useState("");
  const [comment, setComment] = useState("");
  const [rows, setRows] = useState<VersionResponse[]>([]);
  const [compareResult, setCompareResult] = useState("");
  const [error, setError] = useState("");

  async function run(task: () => Promise<void>) {
    try {
      setError("");
      await task();
    } catch (e) {
      const message = e instanceof Error ? e.message : "Operation failed";
      setError(message);
      showToast(message, "error");
    }
  }

  const createDocument = (event: FormEvent) => {
    event.preventDefault();
    if (!docFile) return;
    if (title.trim().length < 3) {
      setError("Title must be at least 3 characters.");
      showToast("Title must be at least 3 characters.", "error");
      return;
    }
    run(async () => {
      const created = await api.createDocument(title, docFile, token);
      showToast(`Created document ${created.title}.`, "success");
      setDocumentId(created.id);
    });
  };

  const loadVersions = () =>
    run(async () => {
      if (!isUuid(documentId)) throw new Error("Document ID must be a valid UUID.");
      const versions = await api.getVersions(documentId, token);
      setRows(versions);
      showToast(`Loaded ${versions.length} versions.`, "success");
    });

  const createVersion = (event: FormEvent) => {
    event.preventDefault();
    if (!versionFile) return;
    run(async () => {
      if (!isUuid(documentId)) throw new Error("Document ID must be a valid UUID.");
      const created = await api.createVersion(documentId, versionFile, token);
      setVersionId(created.id);
      showToast(`Created version ${created.versionNumber}.`, "success");
    });
  };

  return (
    <section>
      <h1>Operations Console</h1>
      <p className="muted">Role-based actions for documents, versions, and review workflow.</p>

      {(role === "AUTHOR" || role === "ADMIN") && (
        <div className="panel">
          <h3>Create document</h3>
          <form onSubmit={createDocument}>
            <input placeholder="Title" value={title} onChange={(e) => setTitle(e.target.value)} />
            <input type="file" onChange={(e) => setDocFile(e.target.files?.[0] ?? null)} />
            <button type="submit">Create</button>
          </form>
        </div>
      )}

      <div className="panel">
        <h3>Document lookup</h3>
        <input placeholder="Document UUID" value={documentId} onChange={(e) => setDocumentId(e.target.value)} />
        <button onClick={loadVersions}>Load versions</button>
        <button
          onClick={() =>
            run(async () => {
              const value = await api.compareLatest(documentId, token);
              setCompareResult(value);
              showToast("Loaded compare result.", "success");
            })
          }
        >
          Compare latest
        </button>
      </div>

      {(role === "AUTHOR" || role === "ADMIN") && (
        <div className="panel">
          <h3>Create version</h3>
          <form onSubmit={createVersion}>
            <input type="file" onChange={(e) => setVersionFile(e.target.files?.[0] ?? null)} />
            <button type="submit">Create version</button>
          </form>
        </div>
      )}

      <div className="panel">
        <h3>Version actions</h3>
        <input placeholder="Version UUID" value={versionId} onChange={(e) => setVersionId(e.target.value)} />
        {(role === "AUTHOR" || role === "ADMIN") && (
          <button onClick={() => run(async () => void (await api.submitVersion(versionId, token)))}>Submit</button>
        )}
        {(role === "AUTHOR" || role === "REVIEWER") && (
          <>
            <button onClick={() => run(async () => void (await api.approveVersion(versionId, token)))}>Approve</button>
            <button onClick={() => run(async () => void (await api.rejectVersion(versionId, token)))}>Reject</button>
          </>
        )}
      </div>

      {(role === "REVIEWER" || role === "ADMIN") && (
        <div className="panel">
          <h3>Add comment</h3>
          <input placeholder="Comment..." value={comment} onChange={(e) => setComment(e.target.value)} />
          <button
            onClick={() =>
              run(async () => {
                if (comment.trim().length < 5) throw new Error("Comment must be at least 5 characters.");
                await api.addComment(versionId, comment, token);
                showToast("Comment saved.", "success");
              })
            }
          >
            Save comment
          </button>
        </div>
      )}

      {error && <p className="error">{error}</p>}
      {compareResult && <pre>{compareResult}</pre>}

      <div className="panel">
        <h3>Versions</h3>
        <table>
          <thead>
            <tr>
              <th>Version</th>
              <th>Status</th>
              <th>File</th>
              <th>Created By</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((v) => (
              <tr key={v.id}>
                <td>{v.versionNumber}</td>
                <td>{v.status}</td>
                <td>{v.fileName}</td>
                <td>{v.createdBy?.username}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
