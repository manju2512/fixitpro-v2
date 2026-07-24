import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { reviewsApi } from '../api/reviews';
import { getApiErrorMessage } from '../api/client';
import { StarRatingInput } from './StarRatingInput';

export function ReviewForm({ reservationId, onDone }: { reservationId: number; onDone: () => void }) {
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');

  const mutation = useMutation({
    mutationFn: () => reviewsApi.create({ reservationId, rating, comment }),
    onSuccess: onDone,
  });

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        mutation.mutate();
      }}
      className="mt-3 flex flex-col gap-2 border-t border-line pt-3"
    >
      <StarRatingInput value={rating} onChange={setRating} />
      <textarea
        required
        placeholder="How did it go?"
        value={comment}
        onChange={(e) => setComment(e.target.value)}
        rows={2}
        className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
      />
      {mutation.isError && <p className="text-xs text-rust">{getApiErrorMessage(mutation.error)}</p>}
      <button
        type="submit"
        disabled={mutation.isPending}
        className="self-start rounded-md bg-signal px-3 py-1.5 text-xs font-semibold text-signal-ink transition-opacity hover:opacity-90 disabled:opacity-50"
      >
        {mutation.isPending ? 'Submitting…' : 'Submit review'}
      </button>
    </form>
  );
}
