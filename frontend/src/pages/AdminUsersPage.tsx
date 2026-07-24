import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../api/admin';
import { getApiErrorMessage } from '../api/client';
import { useAuth } from '../context/AuthContext';
import type { Role } from '../types';

const ROLE_FILTERS: { label: string; value: Role | undefined }[] = [
  { label: 'All', value: undefined },
  { label: 'Customers', value: 'CUSTOMER' },
  { label: 'Technicians', value: 'TECHNICIAN' },
  { label: 'Admins', value: 'ADMIN' },
];

export function AdminUsersPage() {
  const { user: currentUser } = useAuth();
  const queryClient = useQueryClient();
  const [roleFilter, setRoleFilter] = useState<Role | undefined>(undefined);

  const usersQuery = useQuery({
    queryKey: ['admin-users', roleFilter],
    queryFn: () => adminApi.listUsers(roleFilter),
  });

  const toggleMutation = useMutation({
    mutationFn: ({ userId, active }: { userId: number; active: boolean }) => adminApi.setUserActive(userId, active),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-users'] }),
  });

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold">Users</h1>

      <div className="mt-4 flex gap-2">
        {ROLE_FILTERS.map((f) => (
          <button
            key={f.label}
            onClick={() => setRoleFilter(f.value)}
            className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
              roleFilter === f.value ? 'bg-ink text-paper' : 'bg-paper-raised text-ink-soft hover:text-ink'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {usersQuery.isLoading && <p className="mt-4 text-sm text-ink-soft">Loading users…</p>}
      {usersQuery.isError && <p className="mt-4 text-sm text-rust">{getApiErrorMessage(usersQuery.error)}</p>}
      {toggleMutation.isError && (
        <p className="mt-4 text-sm text-rust">{getApiErrorMessage(toggleMutation.error)}</p>
      )}

      <div className="mt-4 flex flex-col gap-2">
        {usersQuery.data?.map((u) => {
          const isSelf = u.userId === currentUser?.userId;
          return (
            <div
              key={u.userId}
              className="flex items-center justify-between rounded-lg border border-line bg-paper-raised px-4 py-3"
            >
              <div>
                <p className="font-medium">
                  {u.username} <span className="font-utility text-xs text-ink-soft">· {u.role}</span>
                </p>
                <p className="text-sm text-ink-soft">
                  {u.email} · {u.phone}
                </p>
              </div>
              <button
                onClick={() => toggleMutation.mutate({ userId: u.userId, active: !u.active })}
                disabled={toggleMutation.isPending || isSelf}
                title={isSelf ? "You can't deactivate your own account" : undefined}
                className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-40 ${
                  u.active ? 'bg-moss/15 text-moss hover:bg-rust/15 hover:text-rust' : 'bg-line text-ink-soft'
                }`}
              >
                {u.active ? 'Active' : 'Inactive'}
              </button>
            </div>
          );
        })}
        {usersQuery.data?.length === 0 && <p className="text-sm text-ink-soft">No users found.</p>}
      </div>
    </div>
  );
}
