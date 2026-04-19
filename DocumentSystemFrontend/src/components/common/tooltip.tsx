import type { ReactNode } from 'react';

interface TooltipProps {
  content: string;
  children: ReactNode;
}

export function Tooltip({ content, children }: TooltipProps) {
  return (
    <span className="group relative inline-flex">
      {children}
      <span
        role="tooltip"
        className="pointer-events-none absolute bottom-full left-1/2 z-20 mb-2 -translate-x-1/2 translate-y-1 whitespace-nowrap rounded-md bg-slate-900 px-2 py-1 text-[11px] font-medium text-white opacity-0 shadow-md transition-all duration-200 delay-100 will-change-transform group-hover:translate-y-0 group-hover:opacity-100 group-hover:delay-150 group-focus-within:translate-y-0 group-focus-within:opacity-100 group-focus-within:delay-150"
      >
        {content}
      </span>
    </span>
  );
}
