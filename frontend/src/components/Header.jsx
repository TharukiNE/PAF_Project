import { useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import NotificationBell from './NotificationBell.jsx'
import { useAuth } from '../context/AuthContext.jsx'

const mobileLinks = [
  { to: '/', label: 'Overview', end: true },
  { to: '/resources', label: 'Facilities' },
  { to: '/bookings', label: 'Bookings' },
  { to: '/maintenance', label: 'Maintenance' },
]

const pageTitles = {
  '/': 'Overview',
  '/resources': 'Facilities',
  '/bookings': 'Bookings',
  '/maintenance': 'Maintenance',
  '/admin/users': 'User Management',
}

function formatDate() {
  return new Intl.DateTimeFormat('en-LK', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  }).format(new Date())
}

function Avatar({ name }) {
  const initials = name
    ? name.split(' ').map((w) => w[0]).join('').slice(0, 2).toUpperCase()
    : '?'
  return (
    <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-violet-500 to-indigo-600 text-xs font-bold text-white shadow-md shadow-violet-500/25">
      {initials}
    </div>
  )
}

export default function Header() {
  const { logout, user } = useAuth()
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)
  const location = useLocation()
  const pageTitle = pageTitles[location.pathname] ?? 'Campus Hub'

  return (
    <header className="sticky top-0 z-20 border-b border-slate-200/60 bg-white/75 backdrop-blur-2xl">
      <div className="mx-auto flex h-[3.25rem] w-full max-w-7xl items-center justify-between px-4 sm:px-6 lg:px-10">

        <div className="flex min-w-0 flex-1 items-center gap-3 lg:gap-4">
          <div className="flex items-center gap-3 lg:hidden">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-gradient-to-br from-violet-600 to-indigo-600 shadow-md shadow-violet-500/20">
              <svg className="h-4 w-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
            <span className="font-display text-sm font-semibold text-slate-900">Campus Hub</span>
          </div>

          <div className="hidden min-w-0 lg:flex lg:items-center lg:gap-4">
            <div className="hidden h-9 w-px bg-slate-200/80 sm:block" />
            <div className="min-w-0">
              <p className="text-[10px] font-semibold uppercase tracking-[0.14em] text-slate-400">Workspace</p>
              <h1 className="font-display truncate text-lg font-bold tracking-tight text-slate-900">{pageTitle}</h1>
            </div>
            <span className="ml-2 hidden rounded-full border border-slate-200/80 bg-white/60 px-3 py-1 text-xs font-medium text-slate-500 shadow-sm backdrop-blur-sm xl:inline">
              {formatDate()}
            </span>
          </div>
        </div>

        <div className="flex shrink-0 items-center gap-1">
          <NotificationBell />

          <div className="hidden items-center sm:flex">
            <div className="mx-2 h-6 w-px bg-slate-200/80 sm:mx-3" />
            <div className="flex items-center gap-2.5 pr-1">
              <Avatar name={user?.name} />
              <div className="hidden flex-col md:flex">
                <span className="max-w-[10rem] truncate text-[13px] font-semibold leading-tight text-slate-900">
                  {user?.name || user?.email}
                </span>
                <span className="text-[10px] font-medium uppercase tracking-wide text-violet-600/90">{user?.role}</span>
              </div>
            </div>
            <div className="mx-2 h-6 w-px bg-slate-200/80 md:mx-3" />
            <button
              type="button"
              onClick={() => logout()}
              className="flex items-center gap-1.5 rounded-xl px-3 py-2 text-[13px] font-medium text-slate-500 transition hover:bg-slate-100/90 hover:text-slate-900 active:scale-[0.98]"
            >
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
              <span className="hidden lg:inline">Sign out</span>
            </button>
          </div>

          <button
            type="button"
            className="flex h-9 w-9 items-center justify-center rounded-xl text-slate-500 transition hover:bg-slate-100/90 sm:hidden"
            onClick={() => setMobileMenuOpen((v) => !v)}
            aria-label="Toggle menu"
          >
            {mobileMenuOpen ? (
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            ) : (
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
              </svg>
            )}
          </button>
        </div>
      </div>

      {mobileMenuOpen && (
        <div className="border-t border-slate-100 bg-white/95 px-4 py-4 backdrop-blur-xl lg:hidden">
          <nav className="flex flex-col gap-1">
            {mobileLinks.map((l) => (
              <NavLink
                key={l.to}
                to={l.to}
                end={l.end}
                onClick={() => setMobileMenuOpen(false)}
                className={({ isActive }) =>
                  'rounded-xl px-3 py-2.5 text-sm font-medium transition ' +
                  (isActive
                    ? 'bg-violet-100 text-violet-800'
                    : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900')
                }
              >
                {l.label}
              </NavLink>
            ))}
            <button
              type="button"
              onClick={() => logout()}
              className="mt-2 rounded-xl border border-slate-200 px-3 py-2.5 text-left text-sm font-medium text-slate-600 hover:bg-slate-50"
            >
              Sign out
            </button>
          </nav>
        </div>
      )}
    </header>
  )
}
