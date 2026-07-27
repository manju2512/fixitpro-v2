import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { reservationsApi } from '../api/reservations';
import { getApiErrorMessage } from '../api/client';
import { StatusBadge, TicketCard } from '../components/StatusBadge';
import { ReviewForm } from '../components/ReviewForm';
import type { Reservation } from '../types';

const CANCELLABLE_STATUSES: Reservation['status'][] = ['PENDING', 'CONFIRMED', 'IN_PROGRESS'];

function BookingCardSkeleton() {
  return (
    <div className="animate-pulse rounded-lg border border-line border-l-4 border-l-line bg-paper-raised p-4">
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1">
          <div className="h-4 w-32 rounded bg-line" />
          <div className="mt-2 h-3 w-40 rounded bg-line" />
          <div className="mt-2 h-3 w-48 rounded bg-line" />
        </div>
        <div className="h-5 w-20 rounded-full bg-line" />
      </div>
    </div>
  );
}

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
        <h1 className="font-display text-2xl font-semibold tracking-tight">My bookings</h1>
        <Link
          to="/book"
          className="rounded-md bg-signal px-3 py-1.5 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90"
        >
          Book a service
        </Link>
      </div>

      {query.isError && (
        <p className="mt-4 rounded-md border border-rust/30 bg-rust/10 px-4 py-3 text-sm text-rust">
          {getApiErrorMessage(query.error)}
        </p>
      )}

      {query.isLoading && (
        <div className="mt-4 flex flex-col gap-3">
          <BookingCardSkeleton />
          <BookingCardSkeleton />
        </div>
      )}

      {query.data?.length === 0 && (
        <div className="mt-4 rounded-lg border border-dashed border-line bg-paper-raised px-4 py-8 text-center">
          <p className="text-sm text-ink-soft">No bookings yet.</p>
          <Link
            to="/book"
            className="mt-3 inline-block rounded-md bg-signal px-4 py-2 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90"
          >
            Book your first service
          </Link>
        </div>
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
