export function StarRatingInput({ value, onChange }: { value: number; onChange: (v: number) => void }) {
  return (
    <div className="flex gap-1" role="radiogroup" aria-label="Rating">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={`${n} star${n === 1 ? '' : 's'}`}
          onClick={() => onChange(n)}
          className={`text-xl leading-none transition-colors ${n <= value ? 'text-signal' : 'text-line'}`}
        >
          ★
        </button>
      ))}
    </div>
  );
}

export function StarRatingDisplay({ value }: { value: number }) {
  return (
    <div className="flex gap-0.5" aria-label={`${value} out of 5 stars`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <span key={n} aria-hidden="true" className={`text-sm leading-none ${n <= value ? 'text-signal' : 'text-line'}`}>
          ★
        </span>
      ))}
    </div>
  );
}
