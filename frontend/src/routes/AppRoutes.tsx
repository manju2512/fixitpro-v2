import { Route, Routes } from 'react-router-dom';
import { Layout } from '../layout/Layout';
import { HomePage } from '../pages/HomePage';
import { LoginPage } from '../pages/LoginPage';
import { SignupPage } from '../pages/SignupPage';
import { BookServicePage } from '../pages/BookServicePage';
import { MyBookingsPage } from '../pages/MyBookingsPage';
import { TechnicianJobsPage } from '../pages/TechnicianJobsPage';
import { AdminDashboardPage } from '../pages/AdminDashboardPage';
import { AdminUsersPage } from '../pages/AdminUsersPage';
import { AdminTechniciansPage } from '../pages/AdminTechniciansPage';
import { AdminReviewsPage } from '../pages/AdminReviewsPage';
import { AdminServiceTypesPage } from '../pages/AdminServiceTypesPage';
import { NotFoundPage } from '../pages/NotFoundPage';
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

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
