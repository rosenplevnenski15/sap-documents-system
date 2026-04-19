import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { usersApi } from '../../api/users.api';
import { useAuthStore } from '../../store/auth.store';
import { useActivityStore } from '../../store/activity.store';
import { getErrorMessage } from '../../lib/http-error';
import { formatDate } from '../../lib/utils';
import { RoleBadge } from '../../components/common/role-badge';
import { Button } from '../../components/common/button';
import { LoadingBlock } from '../../components/common/loading-block';
import type { ActivityAction } from '../../store/activity.store';
import type { Role, UserDto } from '../../types/api';
import { useConfirm } from '../../hooks/use-confirm';

const roles: Role[] = ['READER', 'AUTHOR', 'REVIEWER', 'ADMIN'];

function getUserIsActive(user: UserDto) {
  return user.isActive === true;
}

export function AdminUsersPage() {
  const [users, setUsers] = useState<UserDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyUserId, setBusyUserId] = useState<string>('');
  const currentUser = useAuthStore((state) => state.user);
  const addActivity = useActivityStore((state) => state.addEntry);
  const confirm = useConfirm();

  const loadUsers = async () => {
    try {
      setLoading(true);
      const result = await usersApi.getAllUsers();
      setUsers(result);
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadUsers();
  }, []);

  const handleRoleChange = async (userId: string, role: Role) => {
    try {
      setBusyUserId(userId);
      await usersApi.changeRole(userId, role);

      addActivity({
        action: 'CHANGE_ROLE',
        description: `Changed role of user ${userId} to ${role}`,
        role: currentUser!.role,
        userId: currentUser!.id,
        username: currentUser!.username,
      });

      toast.success('Role updated');
      await loadUsers();
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setBusyUserId('');
    }
  };

  const handleDeactivate = async (userId: string) => {
    const accepted = await confirm({
      title: 'Deactivate user',
      description: 'This user will no longer be able to access the system.',
      confirmText: 'Deactivate',
    });

    if (!accepted) return;

    try {
      setBusyUserId(userId);

      await usersApi.deactivateUser(userId);

      setUsers((currentUsers) =>
        currentUsers.map((user) =>
          user.id === userId ? { ...user, isActive: false } : user,
        ),
      );

      addActivity({
        action: 'DEACTIVATE_USER',
        description: `Deactivated user ${userId}`,
        role: currentUser!.role,
        userId: currentUser!.id,
        username: currentUser!.username,
      });

      toast.success('User deactivated');
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setBusyUserId('');
    }
  };

  const handleActivate = async (userId: string) => {
    try {
      setBusyUserId(userId);

      await usersApi.activateUser(userId);

      setUsers((currentUsers) =>
        currentUsers.map((user) =>
          user.id === userId ? { ...user, isActive: true } : user,
        ),
      );

      addActivity({
        action: 'ACTIVATE_USER' as ActivityAction,
        description: `Activated user ${userId}`,
        role: currentUser!.role,
        userId: currentUser!.id,
        username: currentUser!.username,
      });

      toast.success('User activated');
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setBusyUserId('');
    }
  };

  if (loading) {
    return <LoadingBlock label="Loading users..." />;
  }

  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <h2 className="text-lg font-semibold text-slate-900">User administration</h2>
      <p className="mt-1 text-sm text-slate-600">
        Manage user roles and account activation.
      </p>

      <div className="mt-4 overflow-x-auto">
        <table className="w-full border-collapse text-left text-sm">
          <thead>
            <tr className="border-b border-slate-200 text-xs uppercase tracking-wide text-slate-500">
              <th className="px-2 py-2">Username</th>
              <th className="px-2 py-2">Status</th>
              <th className="px-2 py-2">Current Role</th>
              <th className="px-2 py-2">Change Role</th>
              <th className="px-2 py-2">Actions</th>
            </tr>
          </thead>

          <tbody>
            {users.map((user) => (
              <tr key={user.id} className="border-b border-slate-100">
                <td className="px-2 py-2 font-medium text-slate-800">
                  {user.username}
                </td>

                <td className="px-2 py-2">
                  <span
                    className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold tracking-wide ${
                      getUserIsActive(user)
                        ? 'bg-emerald-100 text-emerald-800'
                        : 'bg-slate-200 text-slate-700'
                    }`}
                  >
                    {getUserIsActive(user) ? 'ACTIVE' : 'INACTIVE'}
                  </span>
                </td>

                <td className="px-2 py-2">
                  <RoleBadge role={user.role} />
                </td>

                <td className="px-2 py-2">
                  <select
                    className="rounded-md border border-slate-300 px-2 py-1"
                    defaultValue={user.role}
                    onChange={(event) =>
                      void handleRoleChange(user.id, event.target.value as Role)
                    }
                    disabled={busyUserId === user.id}
                  >
                    {roles.map((role) => (
                      <option key={role} value={role}>
                        {role}
                      </option>
                    ))}
                  </select>
                </td>

                <td className="px-2 py-2">
                  {getUserIsActive(user) ? (
                    <Button
                      variant="danger"
                      onClick={() => void handleDeactivate(user.id)}
                      disabled={busyUserId === user.id}
                    >
                      Deactivate
                    </Button>
                  ) : (
                    <Button
                      variant="success"
                      onClick={() => void handleActivate(user.id)}
                      disabled={busyUserId === user.id}
                    >
                      Activate
                    </Button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="mt-3 text-xs text-slate-500">
        Last refreshed: {formatDate(new Date().toISOString())}
      </p>
    </div>
  );
}