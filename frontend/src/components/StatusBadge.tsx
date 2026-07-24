import type { ReservationStatus } from '../types';

// Mirrors ReservationStatus.canTransitionTo in core-service. One color per
// state, used consistently as a left-border "ticket stripe" on cards and as
// a small pill badge elsewhere - so status is legible before reading a word.
const STATUS_STYLES: Record<ReservationStatus, { border: string; bg: string; text: string; label: string }> = {
  PENDING: { border: 'border-l-signal', bg: 'bg-signal/15', text: 'text-signal-ink', label: 'Pending' },
  CONFIRMED: { border: 'border-l-steel', bg: 'bg-steel/15', text: 'text-steel', label: 'Confirmed' },
  IN_PROGRESS: { border: 'border-l-steel', bg: 'bg-steel/25', text: 'text-steel', label: 'In progress' },
  COMPLETED: { border: 'border-l-moss', bg: 'bg-moss/15', text: 'text-moss', label: 'Completed' },
  CANCELLED: { border: 'border-l-rust', bg: 'bg-rust/15', text: 'text-rust', label: 'Cancelled' },
};

export function statusStripeClass(status: ReservationStatus): string {
  return STATUS_STYLES[status].border;
}

export function StatusBadge({ status }: { status: ReservationStatus }) {
  const style = STATUS_STYLES[status];
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 font-utility text-xs font-medium ${style.bg} ${style.text}`}
    >
      <span className="h-1.5 w-1.5 rounded-full bg-current" aria-hidden="true" />
      {style.label}
    </span>
  );
}

/** A "ticket" card: white surface, color-coded left stripe matching status. */
export function TicketCard({
  status,
  children,
  className = '',
}: {
  status: ReservationStatus;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div
      className={`rounded-lg border border-line border-l-4 bg-paper-raised p-4 shadow-sm ${statusStripeClass(status)} ${className}`}
    >
      {children}
    </div>
  );
}
