import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { unwrapHalCollection } from '../api/hateoas.js'
import { apiDelete, apiGet, apiSend } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'

const emptyForm = {
  name: '', type: 'LECTURE_HALL', capacity: '', location: '',
  floor: '', amenities: '', status: 'ACTIVE',
}

const typeLabel = {
  LECTURE_HALL: 'Lecture Hall',
  LAB: 'Laboratory',
  MEETING_ROOM: 'Meeting Room',
  EQUIPMENT: 'Equipment',
}

const typeIcon = {
// Facility resource metadata helpers used by the resources page.
  LECTURE_HALL: (
    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
    </svg>
  ),
  LAB: (
    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 3v2m6-2v2M9 19h6m-6.5-6a.5.5 0 010-1h7a.5.5 0 010 1H8.5zm-2 4A2 2 0 008 19h8a2 2 0 001.732-3L15 9H9l-2.268 7z" />
    </svg>
  ),
  MEETING_ROOM: (
    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  ),
  EQUIPMENT: (
    <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.8}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
    </svg>
  ),
}

// Inline capacity bar chart
function CapacityBar({ capacity }) {
  if (!capacity) return null
  const max = 200
  const pct = Math.min(100, Math.round((capacity / max) * 100))
  const color = pct < 40 ? 'bg-emerald-400' : pct < 75 ? 'bg-amber-400' : 'bg-red-400'
  return (
    <div title={`${capacity} seats`}>
      <div className="flex items-center justify-between text-[10px] text-slate-400 mb-0.5">
        <span>Capacity</span>
        <span className="font-semibold text-slate-600">{capacity}</span>
      </div>
      <div className="h-1.5 w-full rounded-full bg-slate-100">
        <div className={`h-1.5 rounded-full ${color} transition-all`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}

// ResourcesPage allows admins to add, edit, and remove campus facilities.
// Regular users may view configured resources for booking and planning.
export default function ResourcesPage() {
  const { user } = useAuth()
  const [list, setList]           = useState([])
  const [form, setForm]           = useState(emptyForm)
  const [editingId, setEditingId] = useState(null)
  const [error, setError]         = useState(null)
  const [typeFilter, setTypeFilter] = useState('ALL')
  const [search, setSearch]       = useState('')

  const isAdmin = user?.role === 'ADMIN'

  // Load all configured campus resources from the backend.
  async function load() {
    setError(null)
    try {
      const data = await apiGet('/resources')
      setList(unwrapHalCollection(data))
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => { load() }, [])

  // Create or update a campus resource depending on whether an edit is in progress.
  async function submit(e) {
    e.preventDefault()
    setError(null)
    const amenitiesList = form.amenities
      ? form.amenities.split(',').map((s) => s.trim()).filter(Boolean)
      : []
    const body = {
      name: form.name,
      type: form.type,
      capacity: form.capacity ? Number(form.capacity) : null,
      location: form.location || null,
      floor: form.floor || null,
      amenities: amenitiesList,
      status: form.status,
    }
    try {
      if (editingId) {
        await apiSend('/resources/' + editingId, 'PUT', body)
      } else {
        await apiSend('/resources', 'POST', body)
      }
      setForm(emptyForm)
      setEditingId(null)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  // Populate the form for editing an existing resource.
  function startEdit(r) {
    setEditingId(r.id)
    setForm({
      name: r.name,
      type: r.type,
      capacity: r.capacity ?? '',
      location: r.location ?? '',
      floor: r.floor ?? '',
      amenities: (r.amenities ?? []).join(', '),
      status: r.status,
    })
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // Delete a resource from the facility inventory.
  async function remove(id) {
    if (!confirm('Delete this resource?')) return
    try {
      await apiDelete('/resources/' + id)
      load()
    } catch (e) {
      setError(e.message)
    }
  }

  // Apply UI filters for type and search term to the loaded resources.
  const filtered = list.filter((r) => {
    if (typeFilter !== 'ALL' && r.type !== typeFilter) return false
    if (search && !r.name.toLowerCase().includes(search.toLowerCase())) return false
    return true
  })

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-slate-900">Facilities</h2>
        <p className="mt-1 text-sm text-slate-500">Lecture halls, labs, meeting rooms and equipment.</p>
      </div>

      {isAdmin && (
        <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="card p-6">
          <h3 className="text-base font-semibold text-slate-900">
            {editingId ? 'Edit resource' : 'Add resource'}
          </h3>
          <form onSubmit={submit} className="mt-5 grid gap-4 sm:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Name</label>
              <input
                required className="input" placeholder="e.g. Lab A204"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Type</label>
              <select className="input" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
                <option value="LECTURE_HALL">Lecture Hall</option>
                <option value="LAB">Laboratory</option>
                <option value="MEETING_ROOM">Meeting Room</option>
                <option value="EQUIPMENT">Equipment</option>
              </select>
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Capacity</label>
              <input
                type="number" min="0" className="input" placeholder="e.g. 60"
                value={form.capacity}
                onChange={(e) => setForm({ ...form, capacity: e.target.value })}
              />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Location</label>
              <input
                className="input" placeholder="e.g. Block A, Floor 2"
                value={form.location}
                onChange={(e) => setForm({ ...form, location: e.target.value })}
              />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Floor / Map ref</label>
              <input
                className="input" placeholder="e.g. Level 3, Wing C"
                value={form.floor}
                onChange={(e) => setForm({ ...form, floor: e.target.value })}
              />
            </div>
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">
                Amenities <span className="font-normal text-slate-400">(comma-separated)</span>
              </label>
              <input
                className="input" placeholder="e.g. Projector, AC, Whiteboard"
                value={form.amenities}
                onChange={(e) => setForm({ ...form, amenities: e.target.value })}
              />
            </div>
            <div className="sm:col-span-2">
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Status</label>
              <select className="input" value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                <option value="ACTIVE">Active</option>
                <option value="OUT_OF_SERVICE">Out of service</option>
              </select>
            </div>
            <div className="flex gap-2 sm:col-span-2">
              <button type="submit" className="btn-primary">
                {editingId ? 'Save changes' : 'Create resource'}
              </button>
              {editingId && (
                <button type="button" className="btn-ghost" onClick={() => { setEditingId(null); setForm(emptyForm) }}>
                  Cancel
                </button>
              )}
            </div>
          </form>
        </motion.div>
      )}

      {error && (
        <div className="flex items-center gap-2 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700 ring-1 ring-red-100">
          <svg className="h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          {error}
        </div>
      )}

      {/* Search + type filter */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <svg className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <input
            className="input pl-9"
            placeholder="Search facilities…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="flex gap-1 rounded-xl bg-slate-100 p-1 flex-shrink-0">
          {['ALL', 'LECTURE_HALL', 'LAB', 'MEETING_ROOM', 'EQUIPMENT'].map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => setTypeFilter(t)}
              className={
                'rounded-lg px-3 py-1 text-xs font-semibold transition ' +
                (typeFilter === t
                  ? 'bg-white text-slate-900 shadow-sm'
                  : 'text-slate-500 hover:text-slate-700')
              }
            >
              {t === 'ALL' ? 'All' : typeLabel[t] ?? t}
            </button>
          ))}
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {filtered.length === 0 && (
          <div className="card col-span-full flex flex-col items-center gap-2 py-14 text-center">
            <svg className="h-10 w-10 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5" />
            </svg>
            <p className="text-sm font-medium text-slate-500">No facilities match your search.</p>
          </div>
        )}
        {filtered.map((r) => (
          <motion.div key={r.id} layout className="card flex flex-col gap-4 p-5">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-violet-100 text-primary">
                  {typeIcon[r.type] ?? typeIcon.EQUIPMENT}
                </div>
                <div>
                  <h4 className="font-semibold text-slate-900">{r.name}</h4>
                  <p className="text-xs text-slate-400">{typeLabel[r.type] ?? r.type}</p>
                </div>
              </div>
              <span
                className={
                  'badge ring-1 ' +
                  (r.status === 'ACTIVE'
                    ? 'bg-emerald-100 text-emerald-700 ring-emerald-200'
                    : 'bg-amber-100 text-amber-700 ring-amber-200')
                }
              >
                {r.status === 'ACTIVE' ? 'Active' : 'Out of service'}
              </span>
            </div>

            {/* Capacity bar */}
            {r.capacity && <CapacityBar capacity={r.capacity} />}

            <div className="grid grid-cols-2 gap-2">
              {r.location && (
                <div className="rounded-lg bg-slate-50 px-3 py-2">
                  <p className="text-[10px] font-medium uppercase tracking-wide text-slate-400">Location</p>
                  <p className="mt-0.5 truncate text-sm font-semibold text-slate-700">{r.location}</p>
                </div>
              )}
              {r.floor && (
                <div className="rounded-lg bg-slate-50 px-3 py-2">
                  <p className="text-[10px] font-medium uppercase tracking-wide text-slate-400">Floor</p>
                  <p className="mt-0.5 truncate text-sm font-semibold text-slate-700">{r.floor}</p>
                </div>
              )}
            </div>

            {/* Amenities */}
            {r.amenities && r.amenities.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {r.amenities.map((a) => (
                  <span key={a} className="rounded-full bg-violet-50 px-2 py-0.5 text-[11px] font-medium text-violet-700">
                    {a}
                  </span>
                ))}
              </div>
            )}

            {isAdmin && (
              <div className="flex gap-3 border-t border-slate-100 pt-3">
                <button
                  type="button"
                  className="text-sm font-medium text-primary hover:text-primary-dark hover:underline"
                  onClick={() => startEdit(r)}
                >
                  Edit
                </button>
                <button
                  type="button"
                  className="text-sm font-medium text-red-500 hover:text-red-700 hover:underline"
                  onClick={() => remove(r.id)}
                >
                  Delete
                </button>
              </div>
            )}
          </motion.div>
        ))}
      </div>
    </div>
  )
}
