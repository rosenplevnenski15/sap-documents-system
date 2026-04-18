import { RouterProvider } from 'react-router-dom';
import { Toaster } from 'sonner';
import { router } from './app/router';
import { ConfirmDialog } from './components/common/confirm-dialog';

function App() {
  return (
    <>
      <RouterProvider router={router} />
      <Toaster position="top-right" richColors />
      <ConfirmDialog />
    </>
  );
}

export default App;
