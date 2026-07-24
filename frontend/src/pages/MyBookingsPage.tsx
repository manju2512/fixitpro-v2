import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { reservationsApi } from '../api/reservations';
import { getApiErrorMessage } from '../api/client';
import { StatusBadge, TicketCard } from '../components/StatusBadge';
import { ReviewForm } from '../components/ReviewForm';
import type { Reservation } from '../types';

const CANCELLABLE_STATUSES: Reservation['status'][] = ['PENDING', 'CONFIRMED', 'IN_PROGRESS'];

function BookingCard({ reservation }: { reservation: Reservation }) {
  const queryClient = useQueryClient();
  const [reviewing, setReviewing] = useState(false);
  const [reviewed, setReviewed] = useState(false);

  const cancelMutation = useMutation({
    mutationFn: () => reservationsApi.updateStatus(reservation.reservationId, 'CANCELLED'),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['my-reservations'] }),
  });

  return (
    <TicketCard status={reservation.status}>
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="font-medium">{reservation.serviceTypeName}</p>
          <p className="mt-0.5 font-utility text-sm text-ink-soft">
            {reservation.reservationDate} · {reservation.timeSlot}
          </p>
          <p className="mt-0.5 text-sm text-ink-soft">
            {reservation.technicianName ? `Technician: ${reservation.technicianName}` : 'Awaiting technician assignment'}
          </p>
        </div>
        <StatusBadge status={reservation.status} />
      </div>

      {cancelMutation.isError && (
        <p className="mt-2 text-xs text-rust">{getApiErrorMessage(cancelMutation.error)}</p>
      )}

      <div className="mt-3 flex gap-3">
        {CANCELLABLE_STATUSES.includes(reservation.status) && (
          <button
            onClick={() => cancelMutation.mutate()}
            disabled={cancelMutation.isPending}
            className="text-xs font-medium text-rust underline underline-offset-2 disabled:opacity-50"
          >
            {cancelMutation.isPending ? 'Cancelling…' : 'Cancel booking'}
          </button>
        )}
        {reservation.status === 'COMPLETED' && !reviewed && (
          <button
            onClick={() => setReviewing((v) => !v)}
            className="text-xs font-medium text-steel underline underline-offset-2"
          >
            {reviewing ? 'Hide review form' : 'Leave a review'}
          </button>
        )}
      </div>

      {reviewing && !reviewed && (
        <ReviewForm
          reservationId={reservation.reservationId}
          onDone={() => {
            setReviewed(true);
            setReviewing(false);
          }}
        />
      )}
      {reviewed && <p className="mt-3 border-t border-line pt-3 text-xs text-moss">Review submitted — thanks!</p>}
    </TicketCard>
  );
}

export function MyBookingsPage() {
  const query = useQuery({ queryKey: ['my-reservations'], queryFn: reservationsApi.myReservations });

  return (
    <div className="max-w-2xl">
      <div className="flex items-center justify-between">
        <h1 className="font-display text-2xl font-semibold">My bookings</h1>
        <Link
          to="/book"
          className="rounded-md bg-signal px-3 py-1.5 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90"
        >
          Book a service
        </Link>
      </div>

      {query.isLoading && <p className="mt-4 text-sm text-ink-soft">Loading your bookings…</p>}
      {query.isError && <p className="mt-4 text-sm text-rust">{getApiErrorMessage(query.error)}</p>}

      {query.data?.length === 0 && (
        <p className="mt-4 rounded-md border border-dashed border-line bg-paper-raised px-4 py-3 text-sm text-ink-soft">
          No bookings yet. Book your first service to see it here.
        </p>
      )}

      <div className="mt-4 flex flex-col gap-3">
        {query.data
          ?.slice()
          .sort((a, b) => b.reservationId - a.reservationId)
          .map((r) => (
            <BookingCard key={r.reservationId} reservation={r} />
          ))}
      </div>
    </div>
  );
}
