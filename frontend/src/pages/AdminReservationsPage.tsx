import { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { reservationsApi } from '../api/reservations';
import { adminApi } from '../api/admin';
import { getApiErrorMessage } from '../api/client';
import { StatusBadge, TicketCard } from '../components/StatusBadge';
import type { Reservation, ReservationStatus } from '../types';

// Mirrors ReservationStatus.canTransitionTo in core-service - only offer
// transitions the backend will actually accept, so the dropdown can't
// suggest a move that just 400s.
const VALID_NEXT_STATUSES: Record<ReservationStatus, ReservationStatus[]> = {
  PENDING: ['CONFIRMED', 'CANCELLED'],
  CONFIRMED: ['IN_PROGRESS', 'CANCELLED'],
  IN_PROGRESS: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
};

const STATUS_FILTERS: Array<ReservationStatus | 'ALL'> = [
  'ALL',
  'PENDING',
  'CONFIRMED',
  'IN_PROGRESS',
  'COMPLETED',
  'CANCELLED',
];

function ReservationRow({ reservation }: { reservation: Reservation }) {
  const queryClient = useQueryClient();
  const [assigning, setAssigning] = useState(false);

  const { data: technicians } = useQuery({
    queryKey: ['admin-technicians'],
    queryFn: adminApi.listAllTechnicians,
    enabled: assigning, // only fetch once the admin actually opens the assign control
  });

  const statusMutation = useMutation({
    mutationFn: (status: ReservationStatus) => reservationsApi.updateStatus(reservation.reservationId, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-reservations'] }),
  });

  const assignMutation = useMutation({
    mutationFn: (technicianId: number) => reservationsApi.assignTechnician(reservation.reservationId, technicianId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-reservations'] });
      setAssigning(false);
    },
  });

  const nextStatuses = VALID_NEXT_STATUSES[reservation.status];
  const isTerminal = nextStatuses.length === 0;
  const matchingTechnicians = technicians?.filter((t) => t.serviceTypeId === reservation.serviceTypeId) ?? [];

  return (
    <TicketCard status={reservation.status}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-medium">
            #{reservation.reservationId} · {reservation.serviceTypeName}
          </p>
          <p className="mt-0.5 font-utility text-sm text-ink-soft">
            {reservation.reservationDate} · {reservation.timeSlot}
          </p>
          <p className="mt-0.5 text-sm text-ink-soft">
            Customer: {reservation.customerName} · {reservation.telephone}
          </p>
          <p className="text-sm text-ink-soft">{reservation.address}</p>
          {reservation.comments && (
            <p className="mt-1 text-sm italic text-ink-soft">"{reservation.comments}"</p>
          )}
        </div>
        <StatusBadge status={reservation.status} />
      </div>

      <div className="mt-3 flex flex-wrap items-center gap-3 border-t border-line pt-3">
        <span className="text-sm text-ink-soft">
          Technician: {reservation.technicianName ?? <span className="text-signal-ink">Unassigned</span>}
        </span>

        {!isTerminal && (
          <button
            onClick={() => setAssigning((v) => !v)}
            className="text-xs font-medium text-steel underline underline-offset-2"
          >
            {reservation.technicianName ? 'Reassign' : 'Assign technician'}
          </button>
        )}

        {!isTerminal && nextStatuses.length > 0 && (
          <select
            value=""
            onChange={(e) => e.target.value && statusMutation.mutate(e.target.value as ReservationStatus)}
            disabled={statusMutation.isPending}
            className="rounded-md border border-line bg-paper px-2 py-1 text-xs outline-none focus-visible:border-signal"
          >
            <option value="" disabled>
              Change status…
            </option>
            {nextStatuses.map((s) => (
              <option key={s} value={s}>
                Move to {s.replace('_', ' ').toLowerCase()}
              </option>
            ))}
          </select>
        )}
      </div>

      {assigning && (
        <div className="mt-3 border-t border-line pt-3">
          {matchingTechnicians.length === 0 ? (
            <p className="text-xs text-ink-soft">
              No technicians for {reservation.serviceTypeName} yet - add one under Technicians.
            </p>
          ) : (
            <select
              value=""
              onChange={(e) => e.target.value && assignMutation.mutate(Number(e.target.value))}
              disabled={assignMutation.isPending}
              className="w-full rounded-md border border-line bg-paper px-2 py-1.5 text-sm outline-none focus-visible:border-signal"
            >
              <option value="" disabled>
                Select a technician…
              </option>
              {matchingTechnicians.map((t) => (
                <option key={t.technicianId} value={t.technicianId} disabled={!t.available}>
                  {t.name} {!t.available ? '(unavailable)' : `· ★ ${t.ratingAvg.toFixed(1)}`}
                </option>
              ))}
            </select>
          )}
        </div>
      )}

      {(statusMutation.isError || assignMutation.isError) && (
        <p className="mt-2 text-xs text-rust">
          {getApiErrorMessage(statusMutation.error ?? assignMutation.error)}
        </p>
      )}
    </TicketCard>
  );
}

export function AdminReservationsPage() {
  const [statusFilter, setStatusFilter] = useState<ReservationStatus | 'ALL'>('ALL');
  const [search, setSearch] = useState('');

  const query = useQuery({ queryKey: ['admin-reservations'], queryFn: reservationsApi.adminAll });

  const filtered = useMemo(() => {
    if (!query.data) return [];
    return query.data
      .filter((r) => statusFilter === 'ALL' || r.status === statusFilter)
      .filter((r) => {
        if (!search.trim()) return true;
        const needle = search.trim().toLowerCase();
        return (
          r.customerName.toLowerCase().includes(needle) ||
          (r.technicianName ?? '').toLowerCase().includes(needle) ||
          r.serviceTypeName.toLowerCase().includes(needle)
        );
      })
      .sort((a, b) => b.reservationId - a.reservationId);
  }, [query.data, statusFilter, search]);

  const unassignedPendingCount = query.data?.filter((r) => r.status === 'PENDING' && !r.technicianName).length ?? 0;

  return (
    <div className="max-w-3xl">
      <div className="flex items-center justify-between">
        <h1 className="font-display text-2xl font-semibold tracking-tight">Reservations</h1>
        {unassignedPendingCount > 0 && (
          <span className="rounded-full bg-signal/15 px-3 py-1 font-utility text-xs font-medium text-signal-ink">
            {unassignedPendingCount} pending unassigned
          </span>
        )}
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-2">
        {STATUS_FILTERS.map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
              statusFilter === s ? 'bg-ink text-paper' : 'bg-paper-raised text-ink-soft hover:text-ink'
            }`}
          >
            {s === 'ALL' ? 'All' : s.replace('_', ' ')}
          </button>
        ))}
      </div>

      <input
        className="mt-3 w-full rounded-md border border-line bg-paper-raised px-3 py-2 text-sm outline-none focus-visible:border-signal"
        placeholder="Search by customer, technician, or service…"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />

      {query.isLoading && <p className="mt-6 text-sm text-ink-soft">Loading reservations…</p>}
      {query.isError && <p className="mt-6 text-sm text-rust">{getApiErrorMessage(query.error)}</p>}
      {query.data && filtered.length === 0 && (
        <p className="mt-6 text-sm text-ink-soft">No reservations match this view.</p>
      )}

      <div className="mt-4 flex flex-col gap-3">
        {filtered.map((r) => (
          <ReservationRow key={r.reservationId} reservation={r} />
        ))}
      </div>
    </div>
  );
}
