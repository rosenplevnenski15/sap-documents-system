import { FileText, Shield, History, LogOut } from 'lucide-react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/auth.store';
import { authApi } from '../../api/auth.api';
import { toast } from 'sonner';
import { getErrorMessage } from '../../lib/http-error';
import { Button } from '../common/button';

const linkClass =
  'flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition hover:bg-slate-100 hover:text-slate-900';

const activeLinkClass = 'bg-slate-900 text-white hover:bg-slate-800 hover:text-white';

export function Sidebar() {
  const user = useAuthStore((state) => state.user);
  const clearSession = useAuthStore((state) => state.clearSession);
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      clearSession();
      navigate('/auth/login');
    }
  };

  return (
    <aside className="flex h-full w-64 flex-col border-r border-slate-200 bg-white">
      <div className="border-b border-slate-200 px-5 py-4">
        <h1 className="text-lg font-semibold tracking-tight text-slate-900">Document System</h1>
      </div>

      <nav className="flex-1 space-y-1 p-3">
        <NavLink
          to="/documents"
          className={({ isActive }) => `${linkClass} ${isActive ? activeLinkClass : 'text-slate-600'}`}
        >
          <FileText size={16} />
          Documents
        </NavLink>

        <NavLink
          to="/audit"
          className={({ isActive }) => `${linkClass} ${isActive ? activeLinkClass : 'text-slate-600'}`}
        >
          <History size={16} />
          Audit
        </NavLink>

        {user?.role === 'ADMIN' ? (
          <NavLink
            to="/admin/users"
            className={({ isActive }) => `${linkClass} ${isActive ? activeLinkClass : 'text-slate-600'}`}
          >
            <Shield size={16} />
            Admin
          </NavLink>
        ) : null}
      </nav>

      <div className="border-t border-slate-200 p-3">
        <Button className="w-full" variant="secondary" onClick={handleLogout}>
          <LogOut size={16} className="mr-2" />
          Logout
        </Button>
      </div>
    </aside>
  );
}
