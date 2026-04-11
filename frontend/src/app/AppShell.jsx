import { Outlet, Link, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'

function AppShell() {
  const { user, isAuthenticated, logout } = useAuth()
  const location = useLocation()

  const isActive = (path) => location.pathname === path || location.pathname.startsWith(path + '/')

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto max-w-7xl px-4">
          <div className="py-4 mb-4">
            <h1 className="text-xl font-semibold mb-2">Smart Campus Operations Hub</h1>
            {isAuthenticated && (
              <p className="text-xs text-slate-600">
                Logged in as {user?.displayName || user?.email} • Roles: {user?.roles?.join(', ') || 'No roles'}
              </p>
            )}
          </div>

          {/* Navigation */}
          {isAuthenticated && (
            <nav className="flex items-center justify-between border-t border-slate-200 pt-4">
              <div className="flex gap-6">
                <Link
                  to="/"
                  className={`pb-2 text-sm font-medium transition ${
                    isActive('/') && location.pathname === '/'
                      ? 'border-b-2 border-indigo-600 text-indigo-600'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Dashboard
                </Link>
                <Link
                  to="/tickets"
                  className={`pb-2 text-sm font-medium transition ${
                    isActive('/tickets')
                      ? 'border-b-2 border-indigo-600 text-indigo-600'
                      : 'text-slate-600 hover:text-slate-900'
                  }`}
                >
                  Tickets
                </Link>
              </div>
              <button
                onClick={logout}
                className="text-xs px-3 py-1 text-slate-600 hover:text-slate-900 transition"
              >
                Sign Out
              </button>
            </nav>
          )}
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}

export default AppShell
