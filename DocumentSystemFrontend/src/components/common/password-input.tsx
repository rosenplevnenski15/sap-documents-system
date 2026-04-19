import { forwardRef, useState, type InputHTMLAttributes } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { cn } from '../../lib/utils';

interface PasswordInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label: string;
  error?: string;
}

export const PasswordInput = forwardRef<HTMLInputElement, PasswordInputProps>(function PasswordInput(
  { label, error, className, ...props },
  ref,
) {
  const [visible, setVisible] = useState(false);

  return (
    <label className="flex w-full flex-col gap-1.5 text-sm">
      <span className="font-medium text-slate-700">{label}</span>
      <div className="relative">
        <input
          ref={ref}
          className={cn(
            'w-full rounded-md border border-slate-300 bg-white px-3 py-2 pr-10 text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-cyan-700 focus:ring-2 focus:ring-cyan-200',
            error && 'border-red-500 focus:border-red-500 focus:ring-red-200',
            className,
          )}
          type={visible ? 'text' : 'password'}
          {...props}
        />
        <button
          type="button"
          className="absolute right-2 top-2 rounded p-1 text-slate-500 hover:bg-slate-100 hover:text-slate-800"
          onClick={() => setVisible((state) => !state)}
          aria-label={visible ? 'Hide password' : 'Show password'}
        >
          {visible ? <EyeOff size={16} /> : <Eye size={16} />}
        </button>
      </div>
      {error ? <span className="text-xs text-red-600">{error}</span> : null}
    </label>
  );
});

PasswordInput.displayName = 'PasswordInput';
