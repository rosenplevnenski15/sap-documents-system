import { Outlet, useLocation } from 'react-router-dom';
import { Sidebar } from './sidebar';
import { Topbar } from './topbar';

export function AppShell() {
  const location = useLocation();

  return (
    <div className="flex min-h-[calc(100vh-4rem)] w-full bg-slate-100">
      <Sidebar />
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar />
        <main className="flex-1 overflow-auto p-6">
          <div key={location.pathname} className="page-route-enter">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
