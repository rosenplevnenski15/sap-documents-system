import type { Role } from '../../types/api';

const roleClass: Record<Role, string> = {
  ADMIN: 'bg-red-100 text-red-800',
  AUTHOR: 'bg-cyan-100 text-cyan-800',
  REVIEWER: 'bg-amber-100 text-amber-800',
  READER: 'bg-emerald-100 text-emerald-800',
};

export function RoleBadge({ role }: { role: Role }) {
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${roleClass[role]}`}>
      {role}
    </span>
  );
}
