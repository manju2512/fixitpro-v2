import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../api/auth';
import { getApiErrorMessage } from '../api/client';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await authApi.forgotPassword(email);
      // Always show success, regardless of what the backend actually did -
      // it always returns 200 either way, this just mirrors that on purpose.
      setSubmitted(true);
    } catch (err) {
      // A genuine network/server error is still worth surfacing - this is
      // different from "email not found", which never reaches this branch.
      setError(getApiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="font-display text-2xl font-semibold tracking-tight">Reset your password</h1>
      <p className="mt-1 text-sm text-ink-soft">
        Enter your account email and we'll send you a link to reset your password.
      </p>

      <div className="mt-6 rounded-lg border border-line bg-paper-raised p-6 shadow-sm">
        {submitted ? (
          <p className="text-sm text-ink">
            If an account exists for <span className="font-medium">{email}</span>, we've sent a
            password reset link. It expires in 30 minutes - check your inbox (and spam folder).
          </p>
        ) : (
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <label className="flex flex-col gap-1">
              <span className="text-sm font-medium">Email</span>
              <input
                type="email"
                className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
                autoFocus
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
              {submitting ? 'Sending…' : 'Send reset link'}
            </button>
          </form>
        )}
      </div>

      <p className="mt-4 text-center text-sm text-ink-soft">
        <Link to="/login" className="font-medium text-steel underline underline-offset-2">
          Back to log in
        </Link>
      </p>
    </div>
  );
}
