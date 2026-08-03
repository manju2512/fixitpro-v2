import { Route, Routes } from 'react-router-dom';
import { Layout } from '../layout/Layout';
import { HomePage } from '../pages/HomePage';
import { LoginPage } from '../pages/LoginPage';
import { SignupPage } from '../pages/SignupPage';
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage';
import { ResetPasswordPage } from '../pages/ResetPasswordPage';
import { BookServicePage } from '../pages/BookServicePage';
import { MyBookingsPage } from '../pages/MyBookingsPage';
import { TechnicianJobsPage } from '../pages/TechnicianJobsPage';
import { TechnicianProfilePage } from '../pages/TechnicianProfilePage';
import { AdminDashboardPage } from '../pages/AdminDashboardPage';
import { AdminUsersPage } from '../pages/AdminUsersPage';
import { AdminTechniciansPage } from '../pages/AdminTechniciansPage';
import { AdminReviewsPage } from '../pages/AdminReviewsPage';
import { AdminServiceTypesPage } from '../pages/AdminServiceTypesPage';
import { AdminReservationsPage } from '../pages/AdminReservationsPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { ProtectedRoute } from './ProtectedRoute';

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<HomePage />} />
        <Route path="login" element={<LoginPage />} />
        <Route path="signup" element={<SignupPage />} />
        <Route path="forgot-password" element={<ForgotPasswordPage />} />
        <Route path="reset-password" element={<ResetPasswordPage />} />

        <Route
          path="book"
          element={
            <ProtectedRoute roles={['CUSTOMER']}>
              <BookServicePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="my-bookings"
          element={
            <ProtectedRoute roles={['CUSTOMER']}>
              <MyBookingsPage />
            </ProtectedRoute>
          }
        />

        <Route
          path="my-jobs"
          element={
            <ProtectedRoute roles={['TECHNICIAN']}>
              <TechnicianJobsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="my-profile"
          element={
            <ProtectedRoute roles={['TECHNICIAN']}>
              <TechnicianProfilePage />
            </ProtectedRoute>
          }
        />

        <Route
          path="admin"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminDashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/users"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminUsersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/technicians"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminTechniciansPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/reviews"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminReviewsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/service-types"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminServiceTypesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="admin/reservations"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminReservationsPage />
            </ProtectedRoute>
          }
        />

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
