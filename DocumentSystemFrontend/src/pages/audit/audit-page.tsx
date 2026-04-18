import { useMemo, useState } from 'react';
import { useAuthStore } from '../../store/auth.store';
import { useActivityStore } from '../../store/activity.store';
import { formatDate } from '../../lib/utils';

export function AuditPage() {
  const user = useAuthStore((state) => state.user);
  const entries = useActivityStore((state) => state.entries);
  const [actionFilter, setActionFilter] = useState('ALL');
  const [query, setQuery] = useState('');

  const myEntries = useMemo(() => {
    return entries
      .filter((entry) => entry.userId === user?.id)
      .filter((entry) => (actionFilter === 'ALL' ? true : entry.action === actionFilter))
      .filter((entry) => entry.description.toLowerCase().includes(query.toLowerCase()));
  }, [actionFilter, entries, query, user?.id]);

  const actions = ['ALL', ...new Set(entries.map((entry) => entry.action))];

  return (
    <div className="space-y-4">
      <section className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">My activity history</h2>

        <div className="mt-4 flex flex-wrap gap-2">
          <select
            value={actionFilter}
            onChange={(event) => setActionFilter(event.target.value)}
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          >
            {actions.map((action) => (
              <option key={action} value={action}>
                {action}
              </option>
            ))}
          </select>
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Filter by description"
            className="w-full max-w-sm rounded-md border border-slate-300 px-3 py-2 text-sm"
          />
        </div>

        <div className="mt-4 overflow-x-auto">
          <table className="w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
                <th className="px-2 py-2">Timestamp</th>
                <th className="px-2 py-2">Action</th>
                <th className="px-2 py-2">Description</th>
                <th className="px-2 py-2">Role</th>
              </tr>
            </thead>
            <tbody>
              {myEntries.map((entry) => (
                <tr key={entry.id} className="border-b border-slate-100">
                  <td className="px-2 py-2">{formatDate(entry.createdAt)}</td>
                  <td className="px-2 py-2">{entry.action}</td>
                  <td className="px-2 py-2">{entry.description}</td>
                  <td className="px-2 py-2">{entry.role}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {!myEntries.length ? <p className="mt-3 text-sm text-slate-500">No matching activity records.</p> : null}
        </div>
      </section>
    </div>
  );
}
