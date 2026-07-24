import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { reviewsApi } from '../api/reviews';
import { getApiErrorMessage } from '../api/client';
import { StarRatingDisplay } from '../components/StarRatingInput';
import type { ReviewReply } from '../types';

const REPLY_ACTIONS: { label: string; status: ReviewReply['status']; className: string }[] = [
  { label: 'Show', status: 'VISIBLE', className: 'bg-moss/15 text-moss hover:bg-moss/25' },
  { label: 'Hide', status: 'HIDDEN', className: 'bg-signal/15 text-signal-ink hover:bg-signal/25' },
  { label: 'Delete', status: 'DELETED', className: 'bg-rust/15 text-rust hover:bg-rust/25' },
];

export function AdminReviewsPage() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['admin-reviews'], queryFn: reviewsApi.adminAll });

  const moderateMutation = useMutation({
    mutationFn: ({ replyId, status }: { replyId: number; status: ReviewReply['status'] }) =>
      reviewsApi.moderateReply(replyId, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-reviews'] }),
  });

  return (
    <div className="max-w-2xl">
      <h1 className="font-display text-2xl font-semibold">Reviews</h1>
      <p className="mt-1 text-sm text-ink-soft">Moderate technician replies to customer reviews.</p>

      {query.isLoading && <p className="mt-4 text-sm text-ink-soft">Loading reviews…</p>}
      {query.isError && <p className="mt-4 text-sm text-rust">{getApiErrorMessage(query.error)}</p>}
      {moderateMutation.isError && (
        <p className="mt-4 text-sm text-rust">{getApiErrorMessage(moderateMutation.error)}</p>
      )}

      <div className="mt-4 flex flex-col gap-3">
        {query.data?.map((review) => (
          <div key={review.reviewId} className="rounded-lg border border-line bg-paper-raised p-4">
            <div className="flex items-baseline justify-between">
              <p className="font-medium">{review.customerName}</p>
              <StarRatingDisplay value={review.rating} />
            </div>
            <p className="mt-1 text-sm text-ink-soft">{review.comment}</p>
            <p className="mt-1 font-utility text-xs text-ink-soft">Technician: {review.technicianName}</p>

            {review.reply && (
              <div className="mt-3 border-t border-line pt-3">
                <div className="flex items-baseline justify-between">
                  <p className="text-sm font-medium">{review.reply.technicianName} replied</p>
                  <span className="font-utility text-xs text-ink-soft">{review.reply.status}</span>
                </div>
                <p className="mt-1 text-sm text-ink-soft">{review.reply.replyText}</p>
                <div className="mt-2 flex gap-2">
                  {REPLY_ACTIONS.filter((a) => a.status !== review.reply!.status).map((action) => (
                    <button
                      key={action.status}
                      onClick={() =>
                        moderateMutation.mutate({ replyId: review.reply!.replyId, status: action.status })
                      }
                      disabled={moderateMutation.isPending}
                      className={`rounded-md px-2.5 py-1 text-xs font-medium transition-colors disabled:opacity-50 ${action.className}`}
                    >
                      {action.label}
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        ))}
        {query.data?.length === 0 && <p className="text-sm text-ink-soft">No reviews yet.</p>}
      </div>
    </div>
  );
}
