import { Link, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { authApi } from '../../api/auth.api';
import { registerSchema } from '../../lib/validation';
import { getErrorMessage, mapValidationErrors } from '../../lib/http-error';
import { TextInput } from '../../components/common/text-input';
import { PasswordInput } from '../../components/common/password-input';
import { Button } from '../../components/common/button';
import { toast } from 'sonner';

type RegisterFormValues = z.infer<typeof registerSchema>;

export function RegisterPage() {
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      username: '',
      password: '',
    },
  });

  const onSubmit = async (values: RegisterFormValues) => {
    try {
      await authApi.register(values);
      toast.success('Registration successful. Please sign in.');
      navigate('/auth/login');
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
    <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center bg-[radial-gradient(circle_at_top,_#fef3c7_0%,_#f8fafc_40%,_#f8fafc_100%)] p-4">
      <div className="w-full max-w-md rounded-2xl border border-slate-200 bg-white p-8 shadow-xl shadow-amber-900/10">
        <div className="mb-6 text-center">
          <h1 className="text-2xl font-bold text-slate-900">Register</h1>
          <p className="mt-2 max-w-xs text-sm text-slate-500 text-center mx-auto">
            Create your account to manage and control document versions.
          </p>
        </div>

        <form className="mt-6 space-y-4" onSubmit={handleSubmit(onSubmit)}>
          <TextInput
            label="Username"
            placeholder="Choose username"
            error={errors.username?.message}
            {...register('username')}
          />
          <PasswordInput
            label="Password"
            placeholder="Create password"
            error={errors.password?.message}
            {...register('password')}
          />

          <Button className="w-full" type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Registering...' : 'Register'}
          </Button>
        </form>

        <p className="mt-4 text-sm text-slate-600">
          Already registered?{' '}
          <Link className="font-medium text-cyan-700" to="/auth/login">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
