import { Outlet } from 'react-router-dom';
import { AppHeader } from './app-header';

export function AppLayout() {
  return (
    <div className="min-h-screen bg-slate-50">
      <AppHeader />
      <main className="min-h-[calc(100vh-4rem)]">
        <Outlet />
      </main>
    </div>
  );
}
