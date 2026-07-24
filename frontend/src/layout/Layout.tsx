import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

function NavItem({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `rounded-md px-3 py-1.5 text-sm font-medium transition-colors ${
          isActive ? 'bg-ink text-paper' : 'text-ink-soft hover:bg-ink/5 hover:text-ink'
        }`
      }
    >
      {children}
    </NavLink>
  );
}

/** The wrench-in-a-tag mark: a literal service tag with a tool cutout. */
function LogoMark() {
  return (
    <svg width="28" height="28" viewBox="0 0 28 28" fill="none" aria-hidden="true">
      <path
        d="M4 6a2 2 0 0 1 2-2h11l7 7v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6Z"
        fill="var(--color-signal)"
      />
      <circle cx="9" cy="9" r="1.6" fill="var(--color-signal-ink)" />
      <path
        d="M14 20.5 18.5 16a2.2 2.2 0 1 0-2-2l-4.5 4.5a1.4 1.4 0 1 0 2 2Z"
        fill="var(--color-paper-raised)"
      />
    </svg>
  );
}

function roleNavItems(role: 'CUSTOMER' | 'TECHNICIAN' | 'ADMIN') {
  switch (role) {
    case 'CUSTOMER':
      return (
        <>
          <NavItem to="/book">Book a service</NavItem>
          <NavItem to="/my-bookings">My bookings</NavItem>
        </>
      );
    case 'TECHNICIAN':
      return <NavItem to="/my-jobs">My jobs</NavItem>;
    case 'ADMIN':
      return (
        <>
          <NavItem to="/admin">Dashboard</NavItem>
          <NavItem to="/admin/users">Users</NavItem>
          <NavItem to="/admin/technicians">Technicians</NavItem>
          <NavItem to="/admin/reviews">Reviews</NavItem>
        </>
      );
  }
}

export function Layout() {
  const { user, isAuthenticated, logout } = useAuth();

  return (
    <div className="min-h-screen bg-paper text-ink">
      <header className="border-b border-line bg-paper-raised">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-3">
          <NavLink to="/" className="flex items-center gap-2">
            <LogoMark />
            <span className="font-display text-lg font-semibold tracking-tight">FixitPro</span>
          </NavLink>

          <nav className="flex items-center gap-1">
            {isAuthenticated && user ? roleNavItems(user.role) : null}
          </nav>

          <div className="flex items-center gap-3">
            {isAuthenticated && user ? (
              <>
                <span className="font-utility text-xs text-ink-soft">
                  {user.username} · {user.role.toLowerCase()}
                </span>
                <button
                  onClick={logout}
                  className="rounded-md border border-line px-3 py-1.5 text-sm font-medium text-ink-soft transition-colors hover:border-rust hover:text-rust"
                >
                  Log out
                </button>
              </>
            ) : (
              <>
                <NavItem to="/login">Log in</NavItem>
                <NavLink
                  to="/signup"
                  className="rounded-md bg-signal px-3 py-1.5 text-sm font-medium text-signal-ink transition-opacity hover:opacity-90"
                >
                  Sign up
                </NavLink>
              </>
            )}
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-6 py-8">
        <Outlet />
      </main>
    </div>
  );
}
