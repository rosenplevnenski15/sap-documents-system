import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="flex h-full items-center justify-center">
      <div className="rounded-xl border border-slate-200 bg-white p-8 text-center shadow-sm">
        <h1 className="text-2xl font-semibold text-slate-900">Page not found</h1>
        <p className="mt-2 text-sm text-slate-600">The page you requested does not exist.</p>
        <Link className="mt-4 inline-block text-sm font-medium text-cyan-700" to="/documents">
          Go to documents
        </Link>
      </div>
    </div>
  );
}
