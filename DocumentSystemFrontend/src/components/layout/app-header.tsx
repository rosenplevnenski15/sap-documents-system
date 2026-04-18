import { Link } from 'react-router-dom';
import logo from '../../assets/logo.svg';

export function AppHeader() {
  return (
    <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="mx-auto flex h-16 w-full max-w-[1400px] items-center px-4 sm:px-6">
        <Link to="/" className="flex items-center gap-3">
          <img src={logo} alt="Document system logo" className="h-9 w-9" />
          <h1 className="text-base font-semibold tracking-tight text-slate-900 sm:text-lg">
            DocumentVersionControlSystem
          </h1>
        </Link>
      </div>
    </header>
  );
}
