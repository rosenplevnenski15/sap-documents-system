import type { ButtonHTMLAttributes } from 'react';
import { cn } from '../../lib/utils';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'success' | 'ghost';
}

const baseClass =
  'inline-flex items-center justify-center rounded-md border px-3 py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-50';

const variants: Record<NonNullable<ButtonProps['variant']>, string> = {
  primary: 'border-slate-900 bg-slate-900 text-white hover:bg-slate-800',
  secondary: 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50',
  danger: 'border-red-700 bg-red-700 text-white hover:bg-red-600',
  success: 'border-emerald-700 bg-emerald-700 text-white hover:bg-emerald-600',
  ghost: 'border-transparent bg-transparent text-slate-700 hover:bg-slate-100',
};

export function Button({ className, variant = 'primary', ...props }: ButtonProps) {
  return <button className={cn(baseClass, variants[variant], className)} {...props} />;
}
