import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import StatCard from '../components/StatCard.jsx'
import CircularProgress from '../components/CircularProgress.jsx'
import { unwrapHalCollection } from '../api/hateoas.js'
import { apiGet } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'

const quickActions = [
  {
    to: '/bookings',
    color: 'from-violet-500 to-violet-600',
    text: 'Book a resource',
    desc: 'Create a new reservation',
  },
  {
    to: '/bookings',
    color: 'from-emerald-500 to-teal-600',
    text: 'Review requests',
    desc: 'Approve or reject as admin',
  },
  {
    to: '/maintenance',
    color: 'from-amber-500 to-orange-500',
    text: 'Log maintenance',
    desc: 'Open a ticket with photos',
  },
  {
    to: '/maintenance',
    color: 'from-sky-500 to-cyan-600',
    text: 'Resolve tickets',
    desc: 'Update status & notes',
  },
]

function greeting() {
  const h = new Date().getHours()
  if (h < 12) return 'Good morning'
  if (h < 17) return 'Good afternoon'
  return 'Good evening'
}

export default function DashboardPage() {
  const { user } = useAuth()
  const [stats, setStats] = useState({ resources: 0, bookings: 0, tickets: 0, activeBookings: 0 })

  const displayName = useMemo(() => {
    const n = user?.name?.trim()
    if (n) return n.split(' ')[0]
    return user?.email?.split('@')[0] || 'there'
  }, [user])

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const [resources, bookings, tickets] = await Promise.all([
          apiGet('/resources'),
          apiGet('/bookings?all=true'),
          apiGet('/maintenance/tickets'),
        ])
        const rList = unwrapHalCollection(resources)
        const bList = unwrapHalCollection(bookings)
        const active = bList.filter((b) => b.status === 'PENDING' || b.status === 'APPROVED').length
        if (!cancelled) {
          setStats({
            resources: rList.length,
            bookings: bList.length,
            tickets: unwrapHalCollection(tickets).length,
            activeBookings: active,
          })
        }
      } catch {
        if (!cancelled) setStats({ resources: 0, bookings: 0, tickets: 0, activeBookings: 0 })
      }
    }
    load()
    return () => { cancelled = true }
  }, [])

  const utilization =
    stats.bookings === 0 ? 0 : Math.min(100, Math.round((stats.activeBookings / Math.max(stats.bookings, 1)) * 100))

  return (
    <div className="space-y-10 pb-4">
      <motion.section
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-3xl border border-violet-200/50 bg-gradient-to-br from-white via-white to-violet-50/60 p-8 shadow-float ring-1 ring-white/90"
      >
        <div className="pointer-events-none absolute inset-y-0 right-0 w-1/2 bg-gradient-to-l from-violet-100/40 to-transparent" />
        <div className="pointer-events-none absolute -right-16 top-0 h-64 w-64 rounded-full bg-violet-300/20 blur-3xl" />

        <div className="relative flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.16em] text-violet-600/80">Dashboard</p>
            <h2 className="mt-2 font-display text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">
              {greeting()}, {displayName}
            </h2>
            <p className="mt-2 max-w-xl text-sm leading-relaxed text-slate-600">
              Facilities, bookings, and maintenance in one place. Use the overview below, then jump into any module from
              the sidebar.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Link
              to="/resources"
              className="inline-flex items-center gap-2 rounded-2xl bg-slate-900 px-4 py-2.5 text-sm font-semibold text-white shadow-lg shadow-slate-900/20 transition hover:bg-slate-800"
            >
              Browse facilities
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M13 7l5 5m0 0l-5 5m5-5H6" />
              </svg>
            </Link>
            <Link
              to="/bookings"
              className="inline-flex items-center gap-2 rounded-2xl border border-slate-200/90 bg-white/90 px-4 py-2.5 text-sm font-semibold text-slate-800 shadow-sm backdrop-blur-sm transition hover:border-violet-200 hover:bg-white"
            >
              New booking
            </Link>
          </div>
        </div>
      </motion.section>

      <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          title="Facilities"
          value={stats.resources}
          subtitle="Halls, labs, equipment"
          tone="violet"
          icon={
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
            </svg>
          }
        />
        <StatCard
          title="Total bookings"
          value={stats.bookings}
          subtitle="All time"
          tone="emerald"
          icon={
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          }
        />
        <StatCard
          title="Active bookings"
          value={stats.activeBookings}
          subtitle="Pending or approved"
          tone="amber"
          icon={
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          }
        />
        <StatCard
          title="Tickets"
          value={stats.tickets}
          subtitle="Maintenance incidents"
          tone="sky"
          icon={
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          }
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-5">
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.15 }}
          className="card p-8 lg:col-span-2"
        >
          <div className="flex items-start justify-between gap-4">
            <div>
              <h3 className="font-display text-lg font-bold text-slate-900">Booking pipeline</h3>
              <p className="mt-1 text-sm text-slate-500">Share of bookings still active vs completed.</p>
            </div>
          </div>
          <div className="mt-10 flex justify-center rounded-2xl bg-gradient-to-b from-slate-50/80 to-white py-6">
            <CircularProgress value={utilization} label="In-flight bookings" />
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="card p-8 lg:col-span-3"
        >
          <h3 className="font-display text-lg font-bold text-slate-900">Quick actions</h3>
          <p className="mt-1 text-sm text-slate-500">Jump straight into common tasks.</p>
          <ul className="mt-6 grid gap-3 sm:grid-cols-2">
            {quickActions.map((a) => (
              <li key={a.text}>
                <Link
                  to={a.to}
                  className="group flex gap-3 rounded-2xl border border-slate-100 bg-slate-50/50 p-4 transition hover:border-violet-200/80 hover:bg-white hover:shadow-md"
                >
                  <span
                    className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-br ${a.color} text-white shadow-md transition group-hover:scale-105`}
                  >
                    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M13 10V3L4 14h7v7l9-11h-7z" />
                    </svg>
                  </span>
                  <span className="min-w-0">
                    <span className="block text-sm font-semibold text-slate-900">{a.text}</span>
                    <span className="mt-0.5 block text-xs text-slate-500">{a.desc}</span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </motion.div>
      </div>
    </div>
  )
}
