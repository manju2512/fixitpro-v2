export function PlaceholderPage({ title, note }: { title: string; note: string }) {
  return (
    <div>
      <h1 className="font-display text-2xl font-semibold">{title}</h1>
      <p className="mt-2 max-w-md rounded-md border border-dashed border-line bg-paper-raised px-4 py-3 font-utility text-sm text-ink-soft">
        {note}
      </p>
    </div>
  );
}
