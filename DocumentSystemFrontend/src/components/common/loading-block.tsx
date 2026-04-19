export function LoadingBlock({ label = 'Loading...' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center rounded-lg border border-dashed border-slate-300 bg-white p-10 text-sm text-slate-600">
      {label}
    </div>
  );
}
