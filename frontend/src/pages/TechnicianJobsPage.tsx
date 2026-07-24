import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { catalogApi } from '../api/catalog';
import { reservationsApi } from '../api/reservations';
import { getApiErrorMessage } from '../api/client';
import { StatusBadge, TicketCard } from '../components/StatusBadge';
import type { Reservation, ReservationStatus } from '../types';

// Mirrors ReservationStatus.canTransitionTo - technicians can advance through
// the lifecycle but (per assertCanChangeStatus in ReservationService) cannot
// cancel. PENDING and CONFIRMED both advance toward completion; terminal
// states get no button.
const NEXT_STATUS: Partial<Record<ReservationStatus, ReservationStatus>> = {
  PENDING: 'CONFIRMED',
  CONFIRMED: 'IN_PROGRESS',
  IN_PROGRESS: 'COMPLETED',
};

function AvailabilityToggle() {
  const queryClient = useQueryClient();
  const profileQuery = useQuery({ queryKey: ['my-technician-profile'], queryFn: catalogApi.getMyProfile });

  const toggleMutation = useMutation({
    mutationFn: (available: boolean) => catalogApi.setMyAvailability(available),
    onSuccess: (updated) => queryClient.setQueryData(['my-technician-profile'], updated),
  });

  if (profileQuery.isLoading) return <p className="text-sm text-ink-soft">Loading your profile…</p>;
  if (profileQuery.isError) return <p className="text-sm text-rust">{getApiErrorMessage(profileQuery.error)}</p>;
  if (!profileQuery.data) return null;

  const profile = profileQuery.data;

  return (
    <div className="flex items-center justify-between rounded-lg border border-line bg-paper-raised p-4">
      <div>
        <p className="font-medium">{profile.name}</p>
        <p className="mt-0.5 text-sm text-ink-soft">
          {profile.serviceType} · {profile.yearsExperience} yrs ·{' '}
          {profile.ratingCount > 0 ? `★ ${profile.ratingAvg.toFixed(1)} (${profile.ratingCount})` : 'No reviews yet'}
        </p>
      </div>
      <button
        onClick={() => toggleMutation.mutate(!profile.available)}
        disabled={toggleMutation.isPending}
        className={`rounded-md px-3 py-1.5 text-sm font-medium transition-colors disabled:opacity-50 ${
          profile.available
            ? 'bg-moss/15 text-moss hover:bg-moss/25'
            : 'bg-line text-ink-soft hover:bg-ink/10'
        }`}
      >
        {profile.available ? 'Available' : 'Unavailable'} — tap to toggle
      </button>
    </div>
  );
}

function JobCard({ reservation }: { reservation: Reservation }) {
  const queryClient = useQueryClient();
  const nextStatus = NEXT_STATUS[reservation.status];

  const advanceMutation = useMutation({
    mutationFn: (status: ReservationStatus) => reservationsApi.updateStatus(reservation.reservationId, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['my-jobs'] }),
  });

  return (
    <TicketCard status={reservation.status}>
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="font-medium">{reservation.serviceTypeName}</p>
          <p className="mt-0.5 font-utility text-sm text-ink-soft">
            {reservation.reservationDate} · {reservation.timeSlot}
          </p>
          <p className="mt-0.5 text-sm text-ink-soft">Customer: {reservation.customerName}</p>
          <p className="text-sm text-ink-soft">
            {reservation.address} · {reservation.telephone}
          </p>
          {reservation.comments && <p className="mt-1 text-sm text-ink-soft">"{reservation.comments}"</p>}
        </div>
        <StatusBadge status={reservation.status} />
      </div>

      {advanceMutation.isError && (
        <p className="mt-2 text-xs text-rust">{getApiErrorMessage(advanceMutation.error)}</p>
      )}

      {nextStatus && (
        <button
          onClick={() => advanceMutation.mutate(nextStatus)}
          disabled={advanceMutation.isPending}
          className="mt-3 rounded-md bg-signal px-3 py-1.5 text-xs font-semibold text-signal-ink transition-opacity hover:opacity-90 disabled:opacity-50"
        >
          {advanceMutation.isPending ? 'Updating…' : `Mark as ${nextStatus.replace('_', ' ').toLowerCase()}`}
        </button>
      )}
    </TicketCard>
  );
}

export function TechnicianJobsPage() {
  const jobsQuery = useQuery({ queryKey: ['my-jobs'], queryFn: reservationsApi.myAssignedJobs });

  return (
    <div className="max-w-2xl">
      <h1 className="font-display text-2xl font-semibold">My jobs</h1>

      <div className="mt-4">
        <AvailabilityToggle />
      </div>

      {jobsQuery.isLoading && <p className="mt-4 text-sm text-ink-soft">Loading your jobs…</p>}
      {jobsQuery.isError && <p className="mt-4 text-sm text-rust">{getApiErrorMessage(jobsQuery.error)}</p>}

      {jobsQuery.data?.length === 0 && (
        <p className="mt-4 rounded-md border border-dashed border-line bg-paper-raised px-4 py-3 text-sm text-ink-soft">
          No jobs assigned yet.
        </p>
      )}

      <div className="mt-4 flex flex-col gap-3">
        {jobsQuery.data
          ?.slice()
          .sort((a, b) => b.reservationId - a.reservationId)
          .map((r) => (
            <JobCard key={r.reservationId} reservation={r} />
          ))}
      </div>
    </div>
  );
}
