import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../api/admin';
import { getApiErrorMessage } from '../api/client';
import type { ReservationStatus } from '../types';

const STATUS_ORDER: ReservationStatus[] = ['PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-lg border border-line bg-paper-raised p-4">
      <p className="text-sm text-ink-soft">{label}</p>
      <p className="mt-1 font-display text-2xl font-semibold">{value}</p>
    </div>
  );
}

export function AdminDashboardPage() {
  const query = useQuery({ queryKey: ['admin-dashboard-stats'], queryFn: adminApi.dashboardStats });

  if (query.isLoading) return <p className="text-sm text-ink-soft">Loading dashboard…</p>;
  if (query.isError) return <p className="text-sm text-rust">{getApiErrorMessage(query.error)}</p>;
  if (!query.data) return null;

  const stats = query.data;

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold">Dashboard</h1>

      <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCard label="Customers" value={stats.totalCustomers} />
        <StatCard label="Technicians" value={stats.totalTechnicians} />
        <StatCard label="Reservations" value={stats.totalReservations} />
        <StatCard label="Avg. rating" value={stats.totalReviews > 0 ? stats.averageRating.toFixed(1) : '—'} />
      </div>

      <h2 className="mt-8 font-display text-lg font-semibold">Reservations by status</h2>
      <div className="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-5">
        {STATUS_ORDER.map((status) => (
          <StatCard
            key={status}
            label={status.replace('_', ' ').toLowerCase()}
            value={stats.reservationsByStatus[status] ?? 0}
          />
        ))}
      </div>

      <p className="mt-6 text-sm text-ink-soft">
        {stats.totalReviews} review{stats.totalReviews === 1 ? '' : 's'} total.
      </p>
    </div>
  );
}
