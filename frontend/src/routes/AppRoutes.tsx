import { Route, Routes } from 'react-router-dom';
import { Layout } from '../layout/Layout';
import { HomePage } from '../pages/HomePage';
import { LoginPage } from '../pages/LoginPage';
import { SignupPage } from '../pages/SignupPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { PlaceholderPage } from '../pages/PlaceholderPage';
import { ProtectedRoute } from './ProtectedRoute';

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="signup" element={<SignupPage />} />

        <Route
          path="book"
          element={
            <ProtectedRoute roles={['CUSTOMER']}>
              <PlaceholderPage
                title="Book a service"
                note="Service picker + technician selection + scheduling — next phase."
              />
            </ProtectedRoute>
          }
        />
        <Route
          path="my-bookings"
          element={
            <ProtectedRoute roles={['CUSTOMER']}>
              <PlaceholderPage
                title="My bookings"
                note="List of reservations with status tickets and review actions — next phase."
              />
            </ProtectedRoute>
          }
        />

        <Route
          path="my-jobs"
          element={
            <ProtectedRoute roles={['TECHNICIAN']}>
              <PlaceholderPage
                title="My jobs"
                note="Assigned jobs, availability toggle, and status updates — next phase."
              />
            </ProtectedRoute>
          }
        />

        <Route
          path="admin"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <PlaceholderPage title="Admin dashboard" note="Aggregate stats — next phase." />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/users"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <PlaceholderPage title="Users" note="List/activate/deactivate users — next phase." />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/technicians"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <PlaceholderPage
                title="Technicians"
                note="Provision technicians, edit profiles, moderate reviews — next phase."
              />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
