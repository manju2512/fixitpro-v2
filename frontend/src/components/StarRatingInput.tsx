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
