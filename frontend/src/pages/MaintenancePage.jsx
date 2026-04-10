import { useEffect, useRef, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { apiDelete, apiGet, apiSend, apiUpload, getAccessToken } from '../api/client'
import { useAuth } from '../context/AuthContext.jsx'

const priorityConfig = {
  LOW:      { cls: 'bg-sky-100 text-sky-700 ring-sky-200',           label: 'Low' },
  MEDIUM:   { cls: 'bg-amber-100 text-amber-700 ring-amber-200',     label: 'Medium' },
  HIGH:     { cls: 'bg-orange-100 text-orange-700 ring-orange-200',  label: 'High' },
  CRITICAL: { cls: 'bg-red-100 text-red-700 ring-red-200',           label: 'Critical' },
}

const statusConfig = {
  OPEN:        { cls: 'bg-amber-100 text-amber-700 ring-amber-200',       label: 'Open' },
  IN_PROGRESS: { cls: 'bg-blue-100 text-blue-700 ring-blue-200',          label: 'In Progress' },
  RESOLVED:    { cls: 'bg-emerald-100 text-emerald-700 ring-emerald-200', label: 'Resolved' },
  CLOSED:      { cls: 'bg-slate-100 text-slate-600 ring-slate-200',       label: 'Closed' },
  REJECTED:    { cls: 'bg-red-100 text-red-700 ring-red-200',             label: 'Rejected' },
}

// ── SLA elapsed time ─────────────────────────────────────────────────────────
function useElapsedTime(since) {
  const [label, setLabel] = useState('')
  useEffect(() => {
    function compute() {
      if (!since) return setLabel('')
      const ms = Date.now() - new Date(since).getTime()
      const hours = Math.floor(ms / 3_600_000)
      const days  = Math.floor(hours / 24)
      if (days > 0)       setLabel(`${days}d ${hours % 24}h open`)
      else if (hours > 0) setLabel(`${hours}h open`)
      else                setLabel(`${Math.floor(ms / 60_000)}m open`)
    }
    compute()
    const t = setInterval(compute, 60_000)
    return () => clearInterval(t)
  }, [since])
  return label
}

function SlaChip({ createdAt, status }) {
  const elapsed = useElapsedTime(
    status === 'OPEN' || status === 'IN_PROGRESS' ? createdAt : null,
  )
  if (!elapsed) return null
  const ms = Date.now() - new Date(createdAt).getTime()
  const hours = ms / 3_600_000
  const color = hours > 48 ? 'text-red-600 bg-red-50 ring-red-200'
    : hours > 24 ? 'text-amber-600 bg-amber-50 ring-amber-200'
    : 'text-slate-600 bg-slate-100 ring-slate-200'
  return (
    <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-semibold ring-1 ${color}`}>
      <svg className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <circle cx="12" cy="12" r="10" />
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 6v6l4 2" />
      </svg>
      {elapsed}
    </span>
  )
}

// ── AuthImage — fetches a protected image with JWT and renders it ─────────────
// The /api/maintenance/tickets/images/{id}/file endpoint requires auth.
// A plain <img src="..."> tag cannot send the Authorization header.
// Solution: fetch as blob → create an object URL → pass to <img src>.
function AuthImage({ src, alt, className, onClick }) {
  const [blobUrl, setBlobUrl] = useState(null)
  const [loading, setLoading] = useState(true)
  const [failed,  setFailed]  = useState(false)

  useEffect(() => {
    let objectUrl = null
    setLoading(true)
    setFailed(false)

    const token = getAccessToken()
    const headers = token ? { Authorization: `Bearer ${token}` } : {}

    fetch(src, { credentials: 'include', headers })
      .then((res) => {
        if (!res.ok) throw new Error('Failed to load image')
        return res.blob()
      })
      .then((blob) => {
        objectUrl = URL.createObjectURL(blob)
        setBlobUrl(objectUrl)
        setLoading(false)
      })
      .catch(() => {
        setFailed(true)
        setLoading(false)
      })

    // Revoke the object URL when component unmounts to free memory
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl)
    }
  }, [src])

  if (loading) {
    return (
      <div className={`${className} flex items-center justify-center bg-slate-100`}>
        <div className="h-5 w-5 animate-spin rounded-full border-2 border-primary border-t-transparent" />
      </div>
    )
  }

  if (failed) {
    return (
      <div className={`${className} flex flex-col items-center justify-center gap-1 bg-slate-100 text-slate-400`}>
        <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
          <path strokeLinecap="round" strokeLinejoin="round" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01" />
        </svg>
        <span className="text-[10px]">Failed</span>
      </div>
    )
  }

  return (
    <img
      src={blobUrl}
      alt={alt}
      className={className}
      onClick={onClick}
    />
  )
}

// ── Lightbox modal — shows a full-size image ──────────────────────────────────
function Lightbox({ image, onClose }) {
  // Close on Escape key
  useEffect(() => {
    function onKey(e) { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
      onClick={onClose}
    >
      <motion.div
        initial={{ opacity: 0, scale: 0.92 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.92 }}
        transition={{ duration: 0.18 }}
        className="relative max-h-[90vh] max-w-4xl"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute -right-3 -top-3 z-10 flex h-8 w-8 items-center justify-center rounded-full bg-white text-slate-700 shadow-lg hover:bg-slate-100"
        >
          <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <AuthImage
          src={image.downloadUrl}
          alt={image.originalFilename}
          className="max-h-[85vh] max-w-full rounded-xl object-contain shadow-2xl"
        />

        {/* Filename bar */}
        <div className="mt-2 flex items-center justify-between rounded-lg bg-black/40 px-3 py-1.5">
          <span className="text-xs text-white/80">{image.originalFilename}</span>
          <a
            href={image.downloadUrl}
            download={image.originalFilename}
            onClick={(e) => e.stopPropagation()}
            className="text-xs font-medium text-violet-300 hover:text-white hover:underline"
          >
            Download
          </a>
        </div>
      </motion.div>
    </div>
  )
}

// ── Image grid — thumbnail strip for one ticket ───────────────────────────────
function ImageGallery({ images, ticketId, canUpload, onUpload }) {
  const [lightboxImg, setLightboxImg] = useState(null)

  return (
    <>
      <div className="mt-4">
        <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-slate-400">
          Attachments ({images?.length ?? 0} / 3)
        </p>

        <div className="flex flex-wrap gap-3">
          {/* Thumbnail previews */}
          {images?.map((img) => (
            <div key={img.id} className="group relative">
              <AuthImage
                src={img.downloadUrl}
                alt={img.originalFilename}
                className="h-20 w-20 cursor-pointer rounded-xl object-cover ring-2 ring-slate-100 transition group-hover:ring-violet-400"
                onClick={() => setLightboxImg(img)}
              />
              {/* Hover overlay */}
              <div
                className="pointer-events-none absolute inset-0 flex items-center justify-center rounded-xl bg-black/0 transition group-hover:bg-black/30"
              >
                <svg className="h-6 w-6 text-white opacity-0 transition group-hover:opacity-100" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0zM10 7v3m0 0v3m0-3h3m-3 0H7" />
                </svg>
              </div>
              {/* Filename tooltip */}
              <p className="mt-0.5 max-w-[80px] truncate text-center text-[10px] text-slate-400">
                {img.originalFilename}
              </p>
            </div>
          ))}

          {/* Upload slot — shown if fewer than 3 images */}
          {canUpload && images?.length < 3 && (
            <label className="flex h-20 w-20 cursor-pointer flex-col items-center justify-center gap-1 rounded-xl border-2 border-dashed border-slate-200 text-slate-400 transition hover:border-violet-400 hover:text-violet-500">
              <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
              </svg>
              <span className="text-[10px] font-medium">Add image</span>
              <input
                type="file"
                accept="image/*"
                className="hidden"
                onChange={(e) => onUpload(ticketId, e.target.files?.[0])}
              />
            </label>
          )}
        </div>
      </div>

      {/* Lightbox */}
      <AnimatePresence>
        {lightboxImg && (
          <Lightbox image={lightboxImg} onClose={() => setLightboxImg(null)} />
        )}
      </AnimatePresence>
    </>
  )
}

// ── Main page ─────────────────────────────────────────────────────────────────
export default function MaintenancePage() {
  const { user } = useAuth()
  const [tickets, setTickets]                   = useState([])
  const [error, setError]                       = useState(null)
  const [form, setForm]                         = useState({ title: '', description: '', priority: 'MEDIUM' })
  const [commentDrafts, setCommentDrafts]       = useState({})
  const [resolutionDrafts, setResolutionDrafts] = useState({})
  const [statusFilter, setStatusFilter]         = useState('ALL')
  const [priorityFilter, setPriorityFilter]     = useState('ALL')

  const canResolve = user?.role === 'TECHNICIAN' || user?.role === 'ADMIN'

  async function load() {
    setError(null)
    try {
      const data = await apiGet('/maintenance/tickets')
      setTickets(Array.isArray(data) ? data : [])
    } catch (e) {
      setError(e.message)
    }
  }

  useEffect(() => { load() }, [])

  async function createTicket(e) {
    e.preventDefault()
    try {
      await apiSend('/maintenance/tickets', 'POST', {
        title: form.title,
        description: form.description,
        priority: form.priority,
      })
      setForm({ title: '', description: '', priority: 'MEDIUM' })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function uploadImage(ticketId, file) {
    if (!file) return
    const fd = new FormData()
    fd.append('file', file)
    try {
      await apiUpload('/maintenance/tickets/' + ticketId + '/images', fd)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  async function postComment(ticketId) {
    const text = commentDrafts[ticketId]
    if (!text) return
    try {
      await apiSend('/maintenance/tickets/' + ticketId + '/comments', 'POST', { content: text })
      setCommentDrafts((d) => ({ ...d, [ticketId]: '' }))
      load()
    } catch (err) { setError(err.message) }
  }

  async function saveComment(commentId, content) {
    try {
      await apiSend('/maintenance/tickets/comments/' + commentId, 'PUT', { content })
      load()
    } catch (err) { setError(err.message) }
  }

  async function removeComment(commentId) {
    try {
      await apiDelete('/maintenance/tickets/comments/' + commentId)
      load()
    } catch (err) { setError(err.message) }
  }

  async function saveResolution(ticketId) {
    const notes = resolutionDrafts[ticketId]
    if (!notes) return
    try {
      await apiSend('/maintenance/tickets/' + ticketId + '/resolution', 'PUT', {
        resolutionNotes: notes,
        status: 'RESOLVED',
      })
      setResolutionDrafts((d) => ({ ...d, [ticketId]: '' }))
      load()
    } catch (err) { setError(err.message) }
  }

  async function reopen(ticketId) {
    try {
      await apiSend('/maintenance/tickets/' + ticketId + '/reopen', 'POST')
      load()
    } catch (err) { setError(err.message) }
  }

  const filtered = tickets.filter((t) => {
    if (statusFilter !== 'ALL' && t.status !== statusFilter) return false
    if (priorityFilter !== 'ALL' && t.priority !== priorityFilter) return false
    return true
  })

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-bold text-slate-900">Maintenance</h2>
        <p className="mt-1 text-sm text-slate-500">Report incidents, attach images, and track resolution.</p>
      </div>

      {/* ── Report form ─────────────────────────────────────────────────────── */}
      <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className="card p-6">
        <h3 className="text-base font-semibold text-slate-900">Report incident</h3>
        <form onSubmit={createTicket} className="mt-5 grid gap-4 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <label className="mb-1.5 block text-sm font-medium text-slate-700">Title</label>
            <input
              required className="input" placeholder="Brief summary of the issue"
              value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })}
            />
          </div>
          <div className="sm:col-span-2">
            <label className="mb-1.5 block text-sm font-medium text-slate-700">Description</label>
            <textarea
              className="input resize-none" rows={3}
              placeholder="Describe the problem in detail…"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>
          <div>
            <label className="mb-1.5 block text-sm font-medium text-slate-700">Priority</label>
            <select className="input" value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="CRITICAL">Critical</option>
            </select>
          </div>
          <div className="flex items-end">
            <button type="submit" className="btn-primary">Create ticket</button>
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

      {/* ── Filter bars ─────────────────────────────────────────────────────── */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex gap-1 rounded-xl bg-slate-100 p-1">
          {['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map((s) => (
            <button
              key={s} type="button" onClick={() => setStatusFilter(s)}
              className={
                'rounded-lg px-3 py-1 text-xs font-semibold transition ' +
                (statusFilter === s ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700')
              }
            >
              {s === 'ALL' ? 'All' : statusConfig[s]?.label ?? s}
            </button>
          ))}
        </div>
        <div className="flex gap-1 rounded-xl bg-slate-100 p-1">
          {['ALL', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((p) => (
            <button
              key={p} type="button" onClick={() => setPriorityFilter(p)}
              className={
                'rounded-lg px-3 py-1 text-xs font-semibold transition ' +
                (priorityFilter === p ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-700')
              }
            >
              {p === 'ALL' ? 'All priorities' : priorityConfig[p]?.label ?? p}
            </button>
          ))}
        </div>
      </div>

      {/* ── Tickets list ────────────────────────────────────────────────────── */}
      <div className="space-y-4">
        {filtered.length === 0 && (
          <div className="card flex flex-col items-center gap-2 py-14 text-center">
            <svg className="h-10 w-10 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
            <p className="text-sm font-medium text-slate-500">No tickets match the current filter.</p>
          </div>
        )}

        {filtered.map((t) => {
          const pCfg = priorityConfig[t.priority] ?? priorityConfig.MEDIUM
          const sCfg = statusConfig[t.status]     ?? statusConfig.OPEN
          return (
            <motion.div key={t.id} layout className="card overflow-hidden">

              {/* ── Ticket header ─────────────────────────────────────────── */}
              <div className="p-5">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <h4 className="font-semibold text-slate-900">{t.title}</h4>
                      <SlaChip createdAt={t.createdAt} status={t.status} />
                    </div>
                    {t.description && (
                      <p className="mt-1 text-sm text-slate-500">{t.description}</p>
                    )}
                    <p className="mt-1 text-xs text-slate-400">
                      Reported by <span className="font-medium">{t.reporterEmail}</span>
                      {t.createdAt && ` · ${new Date(t.createdAt).toLocaleDateString()}`}
                    </p>
                  </div>
                  <div className="flex shrink-0 flex-wrap gap-2">
                    <span className={`badge ring-1 ${pCfg.cls}`}>{pCfg.label}</span>
                    <span className={`badge ring-1 ${sCfg.cls}`}>{sCfg.label}</span>
                  </div>
                </div>

                {/* Resolution notes */}
                {t.resolutionNotes && (
                  <div className="mt-4 flex gap-2 rounded-xl bg-emerald-50 p-3.5 ring-1 ring-emerald-100">
                    <svg className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    <div>
                      <p className="text-xs font-semibold text-emerald-700">Resolution</p>
                      <p className="mt-0.5 text-sm text-emerald-800">{t.resolutionNotes}</p>
                    </div>
                  </div>
                )}

                {/* Reopen button */}
                {(t.status === 'RESOLVED' || t.status === 'CLOSED') && (
                  <button
                    type="button"
                    className="mt-3 flex items-center gap-1.5 rounded-lg border border-amber-200 bg-amber-50 px-3 py-1.5 text-xs font-medium text-amber-700 transition hover:bg-amber-100"
                    onClick={() => reopen(t.id)}
                  >
                    <svg className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    Reopen ticket
                  </button>
                )}

                {/* ── Image gallery with previews ──────────────────────────── */}
                <ImageGallery
                  images={t.images ?? []}
                  ticketId={t.id}
                  canUpload={true}
                  onUpload={uploadImage}
                />
              </div>

              {/* ── Comments ─────────────────────────────────────────────── */}
              <div className="border-t border-slate-100 bg-slate-50/50 px-5 py-4">
                <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                  Comments ({t.comments?.length ?? 0})
                </p>
                {t.comments?.length > 0 && (
                  <ul className="mb-3 space-y-2">
                    {t.comments.map((c) => (
                      <li key={c.id} className="rounded-xl bg-white p-3 shadow-sm ring-1 ring-slate-100 text-sm">
                        <div className="flex items-center justify-between gap-2">
                          <span className="font-medium text-slate-800">{c.userEmail}</span>
                          <span className="text-xs text-slate-400">{new Date(c.createdAt).toLocaleString()}</span>
                        </div>
                        <p className="mt-1 text-slate-600">{c.content}</p>
                        {c.userId === user?.id && (
                          <div className="mt-2 flex gap-3">
                            <button
                              type="button" className="text-xs font-medium text-primary hover:underline"
                              onClick={() => {
                                const next = window.prompt('Edit comment', c.content)
                                if (next) saveComment(c.id, next)
                              }}
                            >Edit</button>
                            <button
                              type="button" className="text-xs font-medium text-red-500 hover:underline"
                              onClick={() => removeComment(c.id)}
                            >Delete</button>
                          </div>
                        )}
                      </li>
                    ))}
                  </ul>
                )}
                <div className="flex gap-2">
                  <input
                    className="input flex-1 py-2 text-sm"
                    placeholder="Add a comment…"
                    value={commentDrafts[t.id] || ''}
                    onChange={(e) => setCommentDrafts((d) => ({ ...d, [t.id]: e.target.value }))}
                    onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && postComment(t.id)}
                  />
                  <button type="button" className="btn-primary py-2 text-xs" onClick={() => postComment(t.id)}>
                    Post
                  </button>
                </div>
              </div>

              {/* ── Resolution (technician/admin) ─────────────────────────── */}
              {canResolve && t.status !== 'RESOLVED' && t.status !== 'CLOSED' && (
                <div className="border-t border-slate-100 px-5 py-4">
                  <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
                    Technician resolution
                  </p>
                  <textarea
                    className="input resize-none text-sm" placeholder="Enter resolution notes…" rows={2}
                    value={resolutionDrafts[t.id] || ''}
                    onChange={(e) => setResolutionDrafts((d) => ({ ...d, [t.id]: e.target.value }))}
                  />
                  <button
                    type="button" className="btn-primary mt-2 py-2 text-xs"
                    onClick={() => saveResolution(t.id)}
                  >
                    Mark resolved
                  </button>
                </div>
              )}
            </motion.div>
          )
        })}
      </div>
    </div>
  )
}
