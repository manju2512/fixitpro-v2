import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="mx-auto flex max-w-sm flex-col items-center py-12 text-center">
      <span className="font-utility text-6xl font-medium tracking-tight text-line">404</span>
      <h1 className="mt-3 font-display text-2xl font-semibold tracking-tight">Page not found</h1>
      <p className="mt-2 text-sm text-ink-soft">
        Nothing's here — the page may have moved, or the link's off.
      </p>
      <Link
        to="/"
        className="mt-6 rounded-md bg-signal px-4 py-2 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90"
      >
        Back to home
      </Link>
    </div>
  );
}
