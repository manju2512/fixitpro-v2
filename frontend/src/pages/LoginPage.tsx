import { useState, type FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../api/client';

export function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) return <Navigate to="/" replace />;

  const from = (location.state as { from?: Location })?.from?.pathname ?? '/';

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login(username, password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="font-display text-2xl font-semibold tracking-tight">Log in</h1>
      <p className="mt-1 text-sm text-ink-soft">Book a repair or manage your jobs.</p>

      <div className="mt-6 rounded-lg border border-line bg-paper-raised p-6 shadow-sm">
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <label className="flex flex-col gap-1">
            <span className="text-sm font-medium">Username, email, or phone</span>
            <input
              className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              autoFocus
              required
            />
          </label>

          <label className="flex flex-col gap-1">
            <span className="text-sm font-medium">Password</span>
            <input
              type="password"
              className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </label>

          <Link to="/forgot-password" className="-mt-2 self-end text-xs font-medium text-steel underline underline-offset-2">
            Forgot password?
          </Link>

          {error && (
            <p role="alert" className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="mt-2 rounded-md bg-signal px-4 py-2 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90 disabled:opacity-50"
          >
            {submitting ? 'Logging in…' : 'Log in'}
          </button>
        </form>
      </div>

      <p className="mt-4 text-center text-sm text-ink-soft">
        New here?{' '}
        <Link to="/signup" className="font-medium text-steel underline underline-offset-2">
          Create an account
        </Link>
      </p>
    </div>
  );
}
