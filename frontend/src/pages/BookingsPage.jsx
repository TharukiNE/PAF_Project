import { useEffect, useRef, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import QRCode from 'qrcode'
import { apiDelete, apiGet, apiSend } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'

const statusConfig = {
  PENDING:   { label: 'Pending',   cls: 'bg-amber-100 text-amber-700 ring-amber-200' },
  APPROVED:  { label: 'Approved',  cls: 'bg-emerald-100 text-emerald-700 ring-emerald-200' },
  REJECTED:  { label: 'Rejected',  cls: 'bg-red-100 text-red-700 ring-red-200' },
  CANCELLED: { label: 'Cancelled', cls: 'bg-slate-100 text-slate-600 ring-slate-200' },
}

// ── ICS generator ────────────────────────────────────────────────────────────
function toIcsDate(iso) {
  return iso.replace(/[-:]/g, '').replace(/\.\d{3}/, '').replace('Z', 'Z')
}

function downloadIcs(booking) {
  const uid = booking.id + '@smartcampus'
  const now = toIcsDate(new Date().toISOString())
  const start = toIcsDate(new Date(booking.startTime).toISOString())
  const end = toIcsDate(new Date(booking.endTime).toISOString())
  const summary = `Booking: ${booking.resourceName}`
  const desc = booking.purpose ? `Purpose: ${booking.purpose}` : 'Smart Campus Booking'
  const ics = [
    'BEGIN:VCALENDAR',
    'VERSION:2.0',
    'PRODID:-//SmartCampus//Booking//EN',
    'BEGIN:VEVENT',
    `UID:${uid}`,
    `DTSTAMP:${now}`,
    `DTSTART:${start}`,
    `DTEND:${end}`,
    `SUMMARY:${summary}`,
    `DESCRIPTION:${desc}`,
    'END:VEVENT',
    'END:VCALENDAR',
  ].join('\r\n')
  const blob = new Blob([ics], { type: 'text/calendar' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `booking-${booking.id}.ics`
  a.click()
  URL.revokeObjectURL(a.href)
}

// ── QR Modal ─────────────────────────────────────────────────────────────────
function QrModal({ booking, onClose }) {
  const canvasRef = useRef(null)

  useEffect(() => {
    if (!canvasRef.current || !booking) return
    const text = [
      `Booking ID: ${booking.id}`,
      `Resource: ${booking.resourceName}`,
      `Start: ${new Date(booking.startTime).toLocaleString()}`,
      `End: ${new Date(booking.endTime).toLocaleString()}`,
      booking.purpose ? `Purpose: ${booking.purpose}` : '',
    ].filter(Boolean).join('\n')
    QRCode.toCanvas(canvasRef.current, text, { width: 240, margin: 2 })
  }, [booking])

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm"
      onClick={onClose}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.95 }}
        transition={{ duration: 0.15 }}
        className="relative w-full max-w-xs rounded-2xl bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <button
          onClick={onClose}
          className="absolute right-3 top-3 flex h-7 w-7 items-center justify-center rounded-full text-slate-400 hover:bg-slate-100 hover:text-slate-600"
        >
          ✕
        </button>
        <h3 className="mb-1 text-base font-semibold text-slate-900">Check-in QR Code</h3>
        <p className="mb-4 text-xs text-slate-400">{booking?.resourceName}</p>
        <div className="flex justify-center">
          <canvas ref={canvasRef} className="rounded-lg" />
        </div>
        <p className="mt-3 text-center text-xs text-slate-400">Scan at the venue entrance</p>
      </motion.div>
    </div>
  )
}

// ── Page ─────────────────────────────────────────────────────────────────────
export default function BookingsPage() {
  const { user } = useAuth()
  const [bookings, setBookings]       = useState([])
  const [resources, setResources]     = useState([])
  const [error, setError]             = useState(null)
  const [form, setForm]               = useState({ resourceId: '', start: '', end: '', purpose: '' })
  const [decisionReasons, setDecisionReasons] = useState({})
  const [mineOnly, setMineOnly]       = useState(false)
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [qrBooking, setQrBooking]     = useState(null)

  const isAdmin = user?.role === 'ADMIN'

  async function load() {
    setError(null)
    try {
      const q = isAdmin ? '?all=true' : ''
      const [b, r] = await Promise.all([apiGet('/bookings' + q), apiGet('/resources')])
      setBookings(Array.isArray(b) ? b : [])
      setResources(Array.isArray(r) ? r.filter((x) => x.status === 'ACTIVE') : [])
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => { load() }, [isAdmin])

  async function createBooking(e) {
    e.preventDefault()
    setError(null)
    try {
      await apiSend('/bookings', 'POST', {
        resourceId: String(form.resourceId),
        startTime: new Date(form.start).toISOString(),
        endTime: new Date(form.end).toISOString(),
        purpose: form.purpose || null,
      })
      setForm({ resourceId: '', start: '', end: '', purpose: '' })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function setStatus(id, status, reason) {
    try {
      await apiSend('/bookings/' + id + '/status', 'PUT', { status, reason: reason || null })
      load()
    } catch (e) { setError(e.message) }
  }

  async function cancelBooking(id) {
    try {
      await apiSend('/bookings/' + id + '/cancel', 'POST')
      load()
    } catch (e) { setError(e.message) }
  }

  async function removeBooking(id) {
    if (!confirm('Remove this booking from your list? This cannot be undone.')) return
    try {
      await apiDelete('/bookings/' + id)
      load()
    } catch (e) { setError(e.message) }
  }

  // Filter logic
  const filtered = bookings.filter((b) => {
    if (mineOnly && b.userEmail !== user?.email) return false
    if (statusFilter !== 'ALL' && b.status !== statusFilter) return false
    return true
  })

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-slate-900">Bookings</h2>
        <p className="mt-1 text-sm text-slate-500">Request and manage resource reservations.</p>
      </div>

      {/* New booking form */}
      <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="card p-6">
        <h3 className="text-base font-semibold text-slate-900">New booking request</h3>
        <form onSubmit={createBooking} className="mt-5 grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label className="mb-1.5 block text-sm font-medium text-slate-700">Resource</label>
            <select
              required
              className="input"
              value={form.resourceId}
              onChange={(e) => setForm({ ...form, resourceId: e.target.value })}
            >
              <option value="">Select a resource…</option>
              {resources.map((r) => (
                <option key={r.id} value={r.id}>{r.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700">Start</label>
            <input
              required type="datetime-local" className="input"
              value={form.start}
              onChange={(e) => setForm({ ...form, start: e.target.value })}
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700">End</label>
            <input
              required type="datetime-local" className="input"
              value={form.end}
              onChange={(e) => setForm({ ...form, end: e.target.value })}
            />
          </div>
          <div className="sm:col-span-2">
            <label className="mb-1.5 block text-sm font-medium text-slate-700">
              Purpose <span className="font-normal text-slate-400">(optional)</span>
            </label>
            <input
              className="input" placeholder="e.g. Lecture, Workshop, Meeting…"
              value={form.purpose}
              onChange={(e) => setForm({ ...form, purpose: e.target.value })}
            />
          </div>
          <div className="sm:col-span-2">
            <button type="submit" className="btn-primary">Submit request</button>
          </div>
        </form>
      </motion.div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700 ring-1 ring-red-100">
          <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          {error}
        </div>
      )}

      {/* Filters bar */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex gap-1 rounded-xl bg-slate-100 p-1">
          {['ALL', 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'].map((s) => (
            <button
              key={s}
              type="button"
              onClick={() => setStatusFilter(s)}
              className={
                'rounded-lg px-3 py-1 text-xs font-semibold transition ' +
                (statusFilter === s
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-500 hover:text-slate-700')
              }
            >
              {s === 'ALL' ? 'All' : statusConfig[s]?.label ?? s}
            </button>
          ))}
        </div>
        {isAdmin && (
          <button
            type="button"
            onClick={() => setMineOnly((v) => !v)}
            className={
              'flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-xs font-medium transition ' +
              (mineOnly
                ? 'border-violet-200 bg-violet-50 text-violet-700'
                : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300')
            }
          >
            <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            My bookings only
          </button>
        )}
      </div>

      {/* Bookings list */}
      <div className="space-y-3">
        {filtered.length === 0 && (
          <div className="card flex flex-col items-center gap-2 py-14 text-center">
            <svg className="h-10 w-10 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            <p className="text-sm font-medium text-slate-500">No bookings match the current filter.</p>
          </div>
        )}
        {filtered.map((b) => {
          const s = statusConfig[b.status] ?? statusConfig.PENDING
          const isOwn = b.userEmail === user?.email
          const canAct = isAdmin || isOwn
          return (
            <motion.div
              key={b.id}
              layout
              className="card flex flex-wrap items-start justify-between gap-4 p-5"
            >
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold text-slate-900">{b.resourceName}</p>
                  <span className={`badge ring-1 ${s.cls}`}>{s.label}</span>
                </div>
                <p className="mt-1 text-xs text-slate-400">
                  {new Date(b.startTime).toLocaleString()} — {new Date(b.endTime).toLocaleString()}
                </p>
                <p className="text-xs text-slate-400">By {b.userEmail}</p>
                {b.purpose && <p className="mt-1.5 text-sm text-slate-600">{b.purpose}</p>}
                {b.decisionReason && (
                  <p className="mt-1.5 text-sm text-slate-700">
                    <span className="font-semibold text-slate-900">Decision:</span> {b.decisionReason}
                  </p>
                )}
              </div>
              <div className="flex flex-wrap items-start gap-2">
                {/* ICS download */}
                <button
                  type="button"
                  title="Download .ics calendar event"
                  onClick={() => downloadIcs(b)}
                  className="flex items-center gap-1 rounded-lg border border-slate-200 px-2.5 py-1.5 text-xs font-medium text-slate-600 transition hover:border-violet-300 hover:text-violet-700"
                >
                  <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
                  </svg>
                  .ics
                </button>

                {/* QR for approved */}
                {b.status === 'APPROVED' && (
                  <button
                    type="button"
                    title="Show check-in QR code"
                    onClick={() => setQrBooking(b)}
                    className="flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-2.5 py-1.5 text-xs font-medium text-emerald-700 transition hover:bg-emerald-100"
                  >
                    <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v1m6 11h2m-6 0h-2v4m0-11v3m0 0h.01M12 12h4.01M16 20h4M4 12h4m12 0h.01M5 8h2a1 1 0 001-1V5a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1zm12 0h2a1 1 0 001-1V5a1 1 0 00-1-1h-2a1 1 0 00-1 1v2a1 1 0 001 1zM5 20h2a1 1 0 001-1v-2a1 1 0 00-1-1H5a1 1 0 00-1 1v2a1 1 0 001 1z" />
                    </svg>
                    QR
                  </button>
                )}

                {/* Admin actions */}
                {isAdmin && b.status === 'PENDING' && (
                  <>
                    <div className="w-full mt-1">
                      <label className="mb-1.5 block text-xs font-medium text-slate-700">
                        Decision reason <span className="font-normal text-slate-400">(required for Reject)</span>
                      </label>
                      <input
                        className="input"
                        placeholder="e.g. Schedule conflict / policy violation"
                        value={decisionReasons[b.id] || ''}
                        onChange={(e) => setDecisionReasons((d) => ({ ...d, [b.id]: e.target.value }))}
                      />
                    </div>
                    <button
                      type="button"
                      className="rounded-lg bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition hover:bg-emerald-600"
                      onClick={() => setStatus(b.id, 'APPROVED', (decisionReasons[b.id] || '').trim() || null)}
                    >
                      Approve
                    </button>
                    <button
                      type="button"
                      className="rounded-lg bg-red-500 px-3 py-1.5 text-xs font-semibold text-white shadow-sm transition hover:bg-red-600"
                      onClick={() => {
                        const reason = (decisionReasons[b.id] || '').trim()
                        if (!reason) { setError('Decision reason is required when rejecting a booking'); return }
                        setStatus(b.id, 'REJECTED', reason)
                      }}
                    >
                      Reject
                    </button>
                  </>
                )}
                {canAct && b.status === 'PENDING' && (
                  <button
                    type="button"
                    title="Withdraw this request (no approval needed)"
                    className="rounded-lg border border-slate-200 px-3 py-1.5 text-xs font-medium text-slate-600 transition hover:border-amber-300 hover:bg-amber-50 hover:text-amber-900"
                    onClick={() => removeBooking(b.id)}
                  >
                    Withdraw
                  </button>
                )}
                {canAct && b.status === 'APPROVED' && (
                  <button
                    type="button"
                    title="Cancel this approved booking"
                    className="btn-ghost py-1.5 px-3 text-xs text-amber-800 ring-1 ring-amber-200 hover:bg-amber-50"
                    onClick={() => cancelBooking(b.id)}
                  >
                    Cancel booking
                  </button>
                )}
                {canAct && (b.status === 'CANCELLED' || b.status === 'REJECTED') && (
                  <button
                    type="button"
                    title="Delete this record from your list"
                    className="rounded-lg border border-red-200 px-3 py-1.5 text-xs font-medium text-red-700 transition hover:bg-red-50"
                    onClick={() => removeBooking(b.id)}
                  >
                    Remove
                  </button>
                )}
              </div>
            </motion.div>
          )
        })}
      </div>

      {/* QR Modal */}
      <AnimatePresence>
        {qrBooking && <QrModal booking={qrBooking} onClose={() => setQrBooking(null)} />}
      </AnimatePresence>
    </div>
  )
}
