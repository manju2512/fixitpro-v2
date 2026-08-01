import { useState, type FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../api/auth';
import { getApiErrorMessage } from '../api/client';

const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;

function validatePassword(value: string): string | null {
  if (!value) return null;
  if (value.length < 8) return 'Must be at least 8 characters';
  if (!PASSWORD_PATTERN.test(value)) return 'Needs an uppercase letter, a lowercase letter, and a digit';
  return null;
}

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const navigate = useNavigate();

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [touched, setTouched] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  const passwordError = validatePassword(password);
  const confirmError = touched && confirm && confirm !== password ? "Passwords don't match" : null;
  const isValid = !passwordError && password.length >= 8 && confirm === password;

  if (!token) {
    return (
      <div className="mx-auto max-w-sm">
        <h1 className="font-display text-2xl font-semibold tracking-tight">Invalid reset link</h1>
        <p className="mt-2 text-sm text-ink-soft">
          This link is missing its reset token. Request a new one from the forgot password page.
        </p>
        <Link
          to="/forgot-password"
          className="mt-4 inline-block rounded-md bg-signal px-4 py-2 text-sm font-semibold text-signal-ink"
        >
          Request a new link
        </Link>
      </div>
    );
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setTouched(true);
    if (!isValid || !token) return;

    setError(null);
    setSubmitting(true);
    try {
      await authApi.resetPassword(token, password);
      setDone(true);
      setTimeout(() => navigate('/login', { replace: true }), 2500);
    } catch (err) {
      // Covers both an actually-expired/invalid token and any other
      // failure - the backend gives the same "invalid or expired" message
      // for a bad token specifically, which getApiErrorMessage surfaces as-is.
      setError(getApiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="font-display text-2xl font-semibold tracking-tight">Set a new password</h1>

      <div className="mt-6 rounded-lg border border-line bg-paper-raised p-6 shadow-sm">
        {done ? (
          <p className="text-sm text-moss">Password updated. Redirecting you to log in…</p>
        ) : (
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <label className="flex flex-col gap-1">
              <span className="text-sm font-medium">New password</span>
              <input
                type="password"
                className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                onBlur={() => setTouched(true)}
                autoComplete="new-password"
                autoFocus
                required
              />
              <span className={`text-xs ${touched && passwordError ? 'text-rust' : 'text-ink-soft'}`}>
                {touched && passwordError
                  ? passwordError
                  : 'At least 8 characters, with an uppercase letter, a lowercase letter, and a digit.'}
              </span>
            </label>

            <label className="flex flex-col gap-1">
              <span className="text-sm font-medium">Confirm password</span>
              <input
                type="password"
                className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                onBlur={() => setTouched(true)}
                autoComplete="new-password"
                required
              />
              {confirmError && <span className="text-xs text-rust">{confirmError}</span>}
            </label>

            {error && (
              <p role="alert" className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={submitting || !isValid}
              className="mt-2 rounded-md bg-signal px-4 py-2 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90 disabled:opacity-50"
            >
              {submitting ? 'Updating…' : 'Update password'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
