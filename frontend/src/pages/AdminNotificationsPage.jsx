import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { apiGet, apiSend } from '../api/client'

export default function AdminNotificationsPage() {
  const [students, setStudents] = useState([])
  const [message, setMessage] = useState('')
  const [audience, setAudience] = useState('ALL_STUDENTS')
  const [selected, setSelected] = useState(() => new Set())
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [loading, setLoading] = useState(false)

  async function loadRecipients() {
    setError(null)
    try {
      const data = await apiGet('/admin/notifications/recipients')
      setStudents(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => {
    loadRecipients()
  }, [])

  function toggleId(id) {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  function selectAll() {
    setSelected(new Set(students.map((s) => s.id)))
  }

  function clearSelection() {
    setSelected(new Set())
  }

  async function submit(e) {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    const trimmed = message.trim()
    if (!trimmed) {
      setError('Enter a notification message.')
      return
    }
    if (audience === 'SELECTED' && selected.size === 0) {
      setError('Select at least one student, or choose “All students”.')
      return
    }
    setLoading(true)
    try {
      await apiSend('/admin/notifications/broadcast', 'POST', {
        message: trimmed,
        audience,
        userIds: audience === 'SELECTED' ? Array.from(selected) : null,
      })
      setSuccess(
        audience === 'ALL_STUDENTS'
          ? `Announcement sent to all ${students.length} student account(s).`
          : `Announcement sent to ${selected.size} selected student(s).`
      )
      setMessage('')
      clearSelection()
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-slate-900">Broadcast notifications</h2>
        <p className="mt-1 text-sm text-slate-500">
          Send campus announcements (e.g. new lab hours) to students. They appear in the notification bell.
        </p>
      </div>

      <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="card p-6">
        <form onSubmit={submit} className="space-y-5">
          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700">Message</label>
            <textarea
              className="input min-h-[120px] resize-y"
              placeholder="e.g. New electronics lab is open in Block B — bookings available from Monday."
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              maxLength={2000}
            />
            <p className="mt-1 text-xs text-slate-400">{message.length}/2000</p>
          </div>

          <div>
            <span className="mb-2 block text-sm font-medium text-slate-700">Audience</span>
            <div className="flex flex-wrap gap-3">
              <label className="flex cursor-pointer items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 has-[:checked]:border-violet-400 has-[:checked]:bg-violet-50">
                <input
                  type="radio"
                  name="aud"
                  checked={audience === 'ALL_STUDENTS'}
                  onChange={() => setAudience('ALL_STUDENTS')}
                />
                <span className="text-sm font-medium text-slate-800">All students</span>
                <span className="text-xs text-slate-500">({students.length} accounts)</span>
              </label>
              <label className="flex cursor-pointer items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 has-[:checked]:border-violet-400 has-[:checked]:bg-violet-50">
                <input
                  type="radio"
                  name="aud"
                  checked={audience === 'SELECTED'}
                  onChange={() => setAudience('SELECTED')}
                />
                <span className="text-sm font-medium text-slate-800">Selected students</span>
              </label>
            </div>
          </div>

          {audience === 'SELECTED' && (
            <div className="rounded-xl border border-slate-200 bg-slate-50/80 p-4">
              <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                <span className="text-sm font-medium text-slate-700">Students</span>
                <div className="flex gap-2">
                  <button type="button" className="text-xs font-medium text-primary hover:underline" onClick={selectAll}>
                    Select all
                  </button>
                  <button type="button" className="text-xs text-slate-500 hover:underline" onClick={clearSelection}>
                    Clear
                  </button>
                </div>
              </div>
              <div className="max-h-56 space-y-1 overflow-y-auto rounded-lg border border-slate-100 bg-white p-2">
                {students.length === 0 && (
                  <p className="py-6 text-center text-sm text-slate-500">No student accounts yet.</p>
                )}
                {students.map((s) => (
                  <label
                    key={s.id}
                    className="flex cursor-pointer items-center gap-3 rounded-lg px-2 py-2 hover:bg-slate-50"
                  >
                    <input
                      type="checkbox"
                      checked={selected.has(s.id)}
                      onChange={() => toggleId(s.id)}
                    />
                    <span className="min-w-0 flex-1 text-sm text-slate-800">
                      <span className="font-medium">{s.name || s.email}</span>
                      <span className="block text-xs text-slate-500">{s.email}</span>
                    </span>
                  </label>
                ))}
              </div>
            </div>
          )}

          {error && (
            <div className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700 ring-1 ring-red-100">{error}</div>
          )}
          {success && (
            <div className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-800 ring-1 ring-emerald-100">
              {success}
            </div>
          )}

          <button type="submit" disabled={loading} className="btn-primary">
            {loading ? 'Sending…' : 'Send notification'}
          </button>
        </form>
      </motion.div>
    </div>
  )
}
