import { RoleBadge } from '../common/role-badge';
import { useAuthStore } from '../../store/auth.store';

export function Topbar() {
  const user = useAuthStore((state) => state.user);

  return (
    <header className="flex h-16 items-center justify-between border-b border-slate-200 bg-white px-6">
      <div>
        <p className="text-xs uppercase tracking-[0.16em] text-slate-500">Workspace</p>
        <h2 className="text-base font-semibold text-slate-900">Document Version Control System</h2>
      </div>
      <div className="flex items-center gap-3">
        <span className="text-sm text-slate-600">{user?.username}</span>
        {user?.role ? <RoleBadge role={user.role} /> : null}
      </div>
    </header>
  );
}
