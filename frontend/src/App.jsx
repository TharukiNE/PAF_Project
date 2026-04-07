import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext.jsx'
import DashboardLayout from './layouts/DashboardLayout.jsx'
import DashboardPage from './pages/DashboardPage.jsx'
import ResourcesPage from './pages/ResourcesPage.jsx'
import BookingsPage from './pages/BookingsPage.jsx'
import MaintenancePage from './pages/MaintenancePage.jsx'
import AdminUsersPage from './pages/AdminUsersPage.jsx'
import AdminAnalyticsPage from './pages/AdminAnalyticsPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'

function ProtectedRoute({ children }) {
  const { user, loading } = useAuth()
  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-page">
        <div className="h-10 w-10 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    )
  }
  if (!user) return <Navigate to="/login" replace />
  return children
}

function RoleRoute({ roles, children }) {
  const { user } = useAuth()
  if (!roles.includes(user.role)) return <Navigate to="/" replace />
  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <DashboardLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="resources" element={<ResourcesPage />} />
        <Route path="bookings" element={<BookingsPage />} />
        <Route path="maintenance" element={<MaintenancePage />} />
        <Route
          path="admin/users"
          element={
            <RoleRoute roles={['ADMIN']}>
              <AdminUsersPage />
            </RoleRoute>
          }
        />
        <Route
          path="admin/analytics"
          element={
            <RoleRoute roles={['ADMIN']}>
              <AdminAnalyticsPage />
            </RoleRoute>
          }
        />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
