import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { getApiErrorMessage } from '../api/client';

export function SignupPage() {
  const { signup, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', email: '', password: '', phone: '' });
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated) return <Navigate to="/" replace />;

  function update<K extends keyof typeof form>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await signup(form);
      navigate('/', { replace: true });
    } catch (err) {
      setError(getApiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="font-display text-2xl font-semibold">Create your account</h1>
      <p className="mt-1 text-sm text-ink-soft">
        Sign up as a customer to start booking repairs. Technician and admin accounts are provisioned by an admin.
      </p>

      <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Username</span>
          <input
            className="rounded-md border border-line bg-paper-raised px-3 py-2 text-sm outline-none focus-visible:border-signal"
            value={form.username}
            onChange={(e) => update('username', e.target.value)}
            autoComplete="username"
            required
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Email</span>
          <input
            type="email"
            className="rounded-md border border-line bg-paper-raised px-3 py-2 text-sm outline-none focus-visible:border-signal"
            value={form.email}
            onChange={(e) => update('email', e.target.value)}
            autoComplete="email"
            required
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Phone</span>
          <input
            className="rounded-md border border-line bg-paper-raised px-3 py-2 text-sm outline-none focus-visible:border-signal"
            value={form.phone}
            onChange={(e) => update('phone', e.target.value)}
            autoComplete="tel"
            required
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm font-medium">Password</span>
          <input
            type="password"
            className="rounded-md border border-line bg-paper-raised px-3 py-2 text-sm outline-none focus-visible:border-signal"
            value={form.password}
            onChange={(e) => update('password', e.target.value)}
            autoComplete="new-password"
            required
          />
        </label>

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
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
    </div>
  );
}
