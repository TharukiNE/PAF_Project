import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { apiGet } from '../api/client'

// ── Tiny inline SVG bar chart ─────────────────────────────────────────────────
function BarChart({ data, labelKey, valueKey, color = '#7c3aed' }) {
  const max = Math.max(...data.map((d) => d[valueKey]), 1)
  return (
    <div className="space-y-2">
      {data.map((item, i) => {
        const pct = Math.round((item[valueKey] / max) * 100)
        return (
          <div key={i} className="flex items-center gap-3">
            <span className="w-24 shrink-0 truncate text-right text-xs text-slate-500">
              {item[labelKey]}
            </span>
            <div className="flex-1 rounded-full bg-slate-100 h-2.5">
              <motion.div
                className="h-2.5 rounded-full"
                style={{ backgroundColor: color }}
                initial={{ width: 0 }}
                animate={{ width: `${pct}%` }}
                transition={{ duration: 0.5, delay: i * 0.04 }}
              />
            </div>
            <span className="w-8 shrink-0 text-xs font-semibold text-slate-600 tabular-nums">
              {item[valueKey]}
            </span>
          </div>
        )
      })}
    </div>
  )
}

// ── Peak hours heatmap ─────────────────────────────────────────────────────────
function PeakHoursChart({ data }) {
  const max = Math.max(...data.map((d) => d.count), 1)
  return (
    <div className="flex flex-wrap gap-1">
      {data.map((d) => {
        const intensity = d.count / max
        const bg = intensity === 0 ? 'bg-slate-100'
          : intensity < 0.25 ? 'bg-violet-100'
          : intensity < 0.5  ? 'bg-violet-300'
          : intensity < 0.75 ? 'bg-violet-500'
          : 'bg-violet-700'
        return (
          <div
            key={d.hour}
            title={`${d.hour}:00 – ${d.count} bookings`}
            className={`flex h-8 w-8 items-center justify-center rounded text-[10px] font-semibold text-white ${bg} ${intensity < 0.25 ? '!text-slate-400' : ''}`}
          >
            {d.hour}
          </div>
        )
      })}
    </div>
  )
}

// ── Donut (simple SVG) ─────────────────────────────────────────────────────────
const COLORS = ['#7c3aed', '#2563eb', '#16a34a', '#d97706', '#dc2626', '#64748b']

function DonutChart({ segments }) {
  const total = segments.reduce((s, x) => s + x.value, 0)
  if (total === 0) return <p className="text-sm text-slate-400">No data</p>
  let cumulative = 0
  const r = 40, cx = 50, cy = 50
  const paths = segments.map((seg, i) => {
    const frac = seg.value / total
    const start = cumulative
    cumulative += frac
    const a1 = 2 * Math.PI * start - Math.PI / 2
    const a2 = 2 * Math.PI * cumulative - Math.PI / 2
    const x1 = cx + r * Math.cos(a1)
    const y1 = cy + r * Math.sin(a1)
    const x2 = cx + r * Math.cos(a2)
    const y2 = cy + r * Math.sin(a2)
    const large = frac > 0.5 ? 1 : 0
    return (
      <path
        key={seg.label}
        d={`M${cx},${cy} L${x1},${y1} A${r},${r} 0 ${large},1 ${x2},${y2} Z`}
        fill={COLORS[i % COLORS.length]}
        opacity={0.85}
      >
        <title>{seg.label}: {seg.value}</title>
      </path>
    )
  })
  return (
    <div className="flex flex-wrap items-center gap-6">
      <svg viewBox="0 0 100 100" className="h-28 w-28 shrink-0">
        <circle cx={cx} cy={cy} r={r} fill="white" />
        {paths}
        <circle cx={cx} cy={cy} r={r * 0.55} fill="white" />
        <text x={cx} y={cy + 4} textAnchor="middle" className="text-[10px]" fontSize={9} fill="#64748b">{total}</text>
      </svg>
      <ul className="space-y-1.5">
        {segments.map((seg, i) => (
          <li key={seg.label} className="flex items-center gap-2 text-xs">
            <span className="h-2.5 w-2.5 shrink-0 rounded-sm" style={{ backgroundColor: COLORS[i % COLORS.length] }} />
            <span className="text-slate-600">{seg.label}</span>
            <span className="ml-auto font-semibold text-slate-800 tabular-nums">{seg.value}</span>
          </li>
        ))}
      </ul>
    </div>
  )
}

// ── Main page ──────────────────────────────────────────────────────────────────
export default function AdminAnalyticsPage() {
  const [stats, setStats]     = useState(null)
  const [error, setError]     = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    apiGet('/admin/analytics')
      .then((d) => { setStats(d); setLoading(false) })
      .catch((e) => { setError(e.message); setLoading(false) })
  }, [])

  function exportCsv() {
    window.open('/api/admin/analytics/export/bookings', '_blank')
  }

  if (loading) {
    return (
      <div className="flex min-h-60 items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    )
  }

  if (error) {
    return (
      <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700 ring-1 ring-red-100">{error}</div>
    )
  }

  const bookingStatus = stats?.bookingsByStatus ?? {}
  const bookingSegments = Object.entries(bookingStatus).map(([label, value]) => ({ label, value }))

  const ticketStatus = stats?.ticketsByStatus ?? {}
  const ticketSegments = Object.entries(ticketStatus).map(([label, value]) => ({ label, value: Number(value) }))

  const ticketPriority = stats?.ticketsByPriority ?? {}
  const prioritySegments = Object.entries(ticketPriority).map(([label, value]) => ({ label, value: Number(value) }))

  const topResources = (stats?.topResources ?? []).map((r) => ({ name: r.name, count: Number(r.count) }))
  const peakHours    = stats?.peakHours ?? []

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-900">Analytics</h2>
          <p className="mt-1 text-sm text-slate-500">Usage statistics, booking trends, and maintenance insights.</p>
        </div>
        <button
          type="button"
          onClick={exportCsv}
          className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:border-violet-300 hover:text-violet-700"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          Export bookings CSV
        </button>
      </div>

      {/* KPI tiles */}
      <motion.div
        initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}
        className="grid grid-cols-2 gap-4 sm:grid-cols-4"
      >
        {[
          { label: 'Total Bookings',  value: stats?.totalBookings,  icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z', bg: 'bg-violet-100 text-violet-600' },
          { label: 'Active Tickets',  value: stats?.totalTickets,   icon: 'M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z', bg: 'bg-blue-100 text-blue-600' },
          { label: 'Facilities',      value: stats?.totalResources, icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5', bg: 'bg-emerald-100 text-emerald-600' },
          { label: 'Users',           value: stats?.totalUsers,     icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z', bg: 'bg-amber-100 text-amber-600' },
        ].map((kpi, i) => (
          <motion.div
            key={kpi.label}
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: i * 0.07 }}
            className="card p-5"
          >
            <div className={`mb-3 flex h-9 w-9 items-center justify-center rounded-xl ${kpi.bg}`}>
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
                <path strokeLinecap="round" strokeLinejoin="round" d={kpi.icon} />
              </svg>
            </div>
            <p className="font-display text-3xl font-bold text-slate-900 tabular-nums">{kpi.value ?? 0}</p>
            <p className="mt-0.5 text-xs text-slate-500">{kpi.label}</p>
          </motion.div>
        ))}
      </motion.div>

      {/* Charts grid */}
      <div className="grid gap-6 lg:grid-cols-2">
        {/* Top resources */}
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }} className="card p-6">
          <h3 className="mb-4 text-sm font-semibold text-slate-900">Top Booked Resources</h3>
          {topResources.length === 0
            ? <p className="text-sm text-slate-400">No booking data yet.</p>
            : <BarChart data={topResources} labelKey="name" valueKey="count" />
          }
        </motion.div>

        {/* Booking status donut */}
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.15 }} className="card p-6">
          <h3 className="mb-4 text-sm font-semibold text-slate-900">Booking Status Breakdown</h3>
          <DonutChart segments={bookingSegments} />
        </motion.div>

        {/* Ticket status donut */}
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }} className="card p-6">
          <h3 className="mb-4 text-sm font-semibold text-slate-900">Ticket Status Breakdown</h3>
          <DonutChart segments={ticketSegments} />
        </motion.div>

        {/* Ticket priority */}
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.25 }} className="card p-6">
          <h3 className="mb-4 text-sm font-semibold text-slate-900">Ticket Priority Distribution</h3>
          {prioritySegments.length === 0
            ? <p className="text-sm text-slate-400">No ticket data yet.</p>
            : <BarChart data={prioritySegments} labelKey="label" valueKey="value" color="#f59e0b" />
          }
        </motion.div>
      </div>

      {/* Peak hours heatmap */}
      <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }} className="card p-6">
        <h3 className="mb-2 text-sm font-semibold text-slate-900">Peak Booking Hours (UTC)</h3>
        <p className="mb-4 text-xs text-slate-400">Each cell represents an hour of the day; darker = more bookings.</p>
        <PeakHoursChart data={peakHours} />
        <div className="mt-3 flex items-center gap-2 text-[10px] text-slate-400">
          <span className="h-3 w-3 rounded-sm bg-slate-100" /> Low
          <span className="h-3 w-3 rounded-sm bg-violet-200 ml-2" /> Medium
          <span className="h-3 w-3 rounded-sm bg-violet-500 ml-2" /> High
          <span className="h-3 w-3 rounded-sm bg-violet-700 ml-2" /> Peak
        </div>
      </motion.div>
    </div>
  )
}
