import { useAuth } from '../context/AuthContext';

export function HomePage() {
  const { user } = useAuth();

  return (
    <div className="max-w-2xl">
      <h1 className="font-display text-3xl font-semibold tracking-tight">
        Home repairs, booked right.
      </h1>
      <p className="mt-3 text-ink-soft">
        {user
          ? `Welcome back, ${user.username}. The full booking experience lands in the next build pass.`
          : 'Electricians, plumbers, and carpenters — one booking flow, a real status you can track. Full booking experience lands in the next build pass.'}
      </p>
    </div>
  );
}
