import { useConfirmStore } from '../store/confirm.store';

export function useConfirm() {
  return useConfirmStore((state) => state.ask);
}
