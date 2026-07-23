import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div>
      <h1 className="font-display text-2xl font-semibold">Page not found</h1>
      <p className="mt-2 text-ink-soft">There's nothing here.</p>
      <Link to="/" className="mt-4 inline-block text-sm font-medium text-steel underline underline-offset-2">
        Back to home
      </Link>
    </div>
  );
}
