import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { authApi } from '../../api/auth.api';
import { useAuthStore } from '../../store/auth.store';
import { useActivityStore } from '../../store/activity.store';
import { getErrorMessage, mapValidationErrors } from '../../lib/http-error';
import { loginSchema } from '../../lib/validation';
import { TextInput } from '../../components/common/text-input';
import { PasswordInput } from '../../components/common/password-input';
import { Button } from '../../components/common/button';
import { toast } from 'sonner';

type LoginFormValues = z.infer<typeof loginSchema>;

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const setSession = useAuthStore((state) => state.setSession);
  const addEntry = useActivityStore((state) => state.addEntry);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: '',
      password: '',
    },
  });

  const onSubmit = async (values: LoginFormValues) => {
    try {
      const response = await authApi.login(values);
      setSession(response);
      addEntry({
        action: 'LOGIN',
        description: 'Authenticated successfully',
        role: response.user.role,
        userId: response.user.id,
        username: response.user.username,
      });
      toast.success('Logged in successfully');
      navigate((location.state as { from?: string } | null)?.from || '/documents', {
        replace: true,
      });
    } catch (error) {
      const validationErrors = mapValidationErrors(error);
      Object.entries(validationErrors).forEach(([field, message]) => {
        if (field === 'username' || field === 'password') {
          setError(field, { message });
        }
      });
      toast.error(getErrorMessage(error));
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center bg-[radial-gradient(circle_at_top,_#cffafe_0%,_#f8fafc_40%,_#f8fafc_100%)] p-4">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 shadow-xl shadow-cyan-900/10">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-slate-900">Sign in</h1>
          <p className="mt-2 max-w-xs text-sm text-slate-500 text-center mx-auto">
            Secure access to your document management system.
          </p>
        </div>

        <form className="mt-6 space-y-4" onSubmit={handleSubmit(onSubmit)}>
          <TextInput
            label="Username"
            placeholder="Enter username"
            error={errors.username?.message}
            {...register('username')}
          />
          <PasswordInput
            label="Password"
            placeholder="Enter password"
            error={errors.password?.message}
            {...register('password')}
          />

          <Button className="w-full" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Signing in...' : 'Sign in'}
          </Button>
        </form>

        <p className="mt-4 text-sm text-slate-600">
          New user?{' '}
          <Link className="font-medium text-cyan-700" to="/auth/register">
            Create an account
          </Link>
        </p>
      </div>
    </div>
  );
}
