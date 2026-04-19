import { useConfirmStore } from '../../store/confirm.store';
import { Button } from './button';

export function ConfirmDialog() {
  const { isOpen, title, description, confirmText, cancelText, close } = useConfirmStore();

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      <div className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
        <h3 className="text-lg font-semibold text-slate-900">{title}</h3>
        {description ? <p className="mt-2 text-sm text-slate-600">{description}</p> : null}
        <div className="mt-6 flex items-center justify-end gap-2">
          <Button variant="secondary" onClick={() => close(false)}>
            {cancelText}
          </Button>
          <Button variant="danger" onClick={() => close(true)}>
            {confirmText}
          </Button>
        </div>
      </div>
    </div>
  );
}
