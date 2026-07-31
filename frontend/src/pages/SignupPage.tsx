import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../api/auth';
import { getApiErrorMessage } from '../api/client';

const USERNAME_PATTERN = /^[a-zA-Z0-9_.]+$/;
const PHONE_PATTERN = /^[6-9]\d{9}$/;
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;
const USERNAME_CHECK_DEBOUNCE_MS = 400;

type UsernameStatus = 'idle' | 'checking' | 'available' | 'taken' | 'error';

function validateUsernameFormat(value: string): string | null {
  if (!value) return null;
  if (value.length < 3 || value.length > 50) return 'Must be 3-50 characters';
  if (!USERNAME_PATTERN.test(value)) return 'Only letters, numbers, underscores and dots - no spaces';
  return null;
}

function validatePhone(value: string): string | null {
  if (!value) return null;
  if (!PHONE_PATTERN.test(value)) return 'Must be exactly 10 digits, starting with 6-9';
  return null;
}

function validatePassword(value: string): string | null {
  if (!value) return null;
  if (value.length < 8) return 'Must be at least 8 characters';
  if (!PASSWORD_PATTERN.test(value)) return 'Needs an uppercase letter, a lowercase letter, and a digit';
  return null;
}

function Field({
  label,
  hint,
  error,
  children,
}: {
  label: string;
  hint: string;
  error: string | null;
  children: React.ReactNode;
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-sm font-medium">{label}</span>
      {children}
      <span className={`text-xs ${error ? 'text-rust' : 'text-ink-soft'}`}>{error ?? hint}</span>
    </label>
  );
}

export function SignupPage() {
  const { signup, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', email: '', password: '', phone: '' });
  const [touched, setTouched] = useState({ username: false, phone: false, password: false });
  const [usernameStatus, setUsernameStatus] = useState<UsernameStatus>('idle');
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const latestCheckedUsername = useRef<string>('');

  const usernameFormatError = validateUsernameFormat(form.username);
  const phoneError = validatePhone(form.phone);
  const passwordError = validatePassword(form.password);

  // Debounced live availability check - only fires once the format is
  // already valid, so we're not hammering the API on every keystroke of an
  // obviously-invalid username.
  useEffect(() => {
    if (!form.username || usernameFormatError) {
      setUsernameStatus('idle');
      return;
    }

    setUsernameStatus('checking');
    const usernameAtRequestTime = form.username;

    const timer = setTimeout(() => {
      authApi
        .checkUsername(usernameAtRequestTime)
        .then((result) => {
          // Guard against an older, slower request resolving after a newer
          // one - only trust the response if it's still what's in the box.
          if (usernameAtRequestTime !== latestCheckedUsername.current) return;
          setUsernameStatus(result.available ? 'available' : 'taken');
        })
        .catch(() => {
          if (usernameAtRequestTime !== latestCheckedUsername.current) return;
          setUsernameStatus('error');
        });
    }, USERNAME_CHECK_DEBOUNCE_MS);

    latestCheckedUsername.current = usernameAtRequestTime;
    return () => clearTimeout(timer);
  }, [form.username, usernameFormatError]);

  if (isAuthenticated) return <Navigate to="/" replace />;

  const usernameError =
    touched.username && usernameFormatError
      ? usernameFormatError
      : usernameStatus === 'taken'
        ? 'This username is already taken'
        : usernameStatus === 'error'
          ? "Couldn't check availability - try again"
          : null;

  const usernameHelperText =
    usernameStatus === 'checking'
      ? 'Checking availability…'
      : usernameStatus === 'available'
        ? '✓ Username is available'
        : '3-50 characters. Letters, numbers, underscores and dots only - no spaces.';

  const isFormValid = useMemo(
    () =>
      form.username.length >= 3 &&
      !usernameFormatError &&
      usernameStatus === 'available' &&
      form.email.includes('@') &&
      !phoneError &&
      form.phone.length === 10 &&
      !passwordError &&
      form.password.length >= 8,
    [form, usernameFormatError, usernameStatus, phoneError, passwordError],
  );

  function update<K extends keyof typeof form>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function handlePhoneChange(raw: string) {
    const digitsOnly = raw.replace(/\D/g, '').slice(0, 10);
    update('phone', digitsOnly);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setTouched({ username: true, phone: true, password: true });
    if (!isFormValid) return;

    setSubmitError(null);
    setSubmitting(true);
    try {
      await signup(form);
      navigate('/', { replace: true });
    } catch (err) {
      setSubmitError(getApiErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mx-auto max-w-sm">
      <h1 className="font-display text-2xl font-semibold tracking-tight">Create your account</h1>
      <p className="mt-1 text-sm text-ink-soft">
        Sign up as a customer to start booking repairs. Technician and admin accounts are
        provisioned by an admin.
      </p>

      <div className="mt-6 rounded-lg border border-line bg-paper-raised p-6 shadow-sm">
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Field label="Username" hint={usernameHelperText} error={usernameError}>
            <div className="relative">
              <input
                className="w-full rounded-md border border-line bg-paper px-3 py-2 pr-8 text-sm outline-none focus-visible:border-signal"
                value={form.username}
                onChange={(e) => update('username', e.target.value)}
                onBlur={() => setTouched((t) => ({ ...t, username: true }))}
                autoComplete="username"
                autoFocus
                required
              />
              {usernameStatus === 'available' && (
                <span className="absolute right-2 top-1/2 -translate-y-1/2 text-moss">✓</span>
              )}
              {usernameStatus === 'taken' && (
                <span className="absolute right-2 top-1/2 -translate-y-1/2 text-rust">✕</span>
              )}
            </div>
          </Field>

          <Field label="Email" hint="We'll send booking confirmations here." error={null}>
            <input
              type="email"
              className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
              value={form.email}
              onChange={(e) => update('email', e.target.value)}
              autoComplete="email"
              required
            />
          </Field>

          <Field
            label="Phone"
            hint="Exactly 10 digits, starting with 6-9 (Indian mobile number)."
            error={touched.phone ? phoneError : null}
          >
            <input
              type="tel"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={10}
              className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
              value={form.phone}
              onChange={(e) => handlePhoneChange(e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, phone: true }))}
              autoComplete="tel"
              required
            />
          </Field>

          <Field
            label="Password"
            hint="At least 8 characters, with an uppercase letter, a lowercase letter, and a digit."
            error={touched.password ? passwordError : null}
          >
            <input
              type="password"
              className="rounded-md border border-line bg-paper px-3 py-2 text-sm outline-none focus-visible:border-signal"
              value={form.password}
              onChange={(e) => update('password', e.target.value)}
              onBlur={() => setTouched((t) => ({ ...t, password: true }))}
              autoComplete="new-password"
              required
            />
          </Field>

          {submitError && (
            <p role="alert" className="rounded-md bg-rust/10 px-3 py-2 text-sm text-rust">
              {submitError}
            </p>
          )}

          <button
            type="submit"
            disabled={submitting || !isFormValid}
            className="mt-2 rounded-md bg-signal px-4 py-2 text-sm font-semibold text-signal-ink transition-opacity hover:opacity-90 disabled:opacity-50"
          >
            {submitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>
      </div>

      <p className="mt-4 text-center text-sm text-ink-soft">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-steel underline underline-offset-2">
          Log in
        </Link>
      </p>
    </div>
  );
}