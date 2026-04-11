const jsonHeaders = { 'Content-Type': 'application/json' }

const TOKEN_KEY = 'smartcampus_access_token'

export function getAccessToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setAccessToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

export function clearAccessToken() {
  localStorage.removeItem(TOKEN_KEY)
}

/** Call after login / register / auto-login responses that include JWT. */
export function persistAuthFromResponse(data) {
  if (data?.accessToken) setAccessToken(data.accessToken)
  return data?.user ?? null
}

function authHeaders(extra = {}) {
  const t = getAccessToken()
  const h = { ...extra }
  if (t) h.Authorization = `Bearer ${t}`
  return h
}

export function getErrorMessage(error) {
  if (error == null) return 'Unknown error'
  if (typeof error === 'string') return error
  if (error instanceof Error && error.message) return error.message
  if (typeof error === 'object') {
    if (typeof error.message === 'string' && error.message) return error.message
    const body = error.body
    if (body && typeof body === 'object') {
      if (typeof body.message === 'string' && body.message) return body.message
      if (body.errors && typeof body.errors === 'object') {
        const values = Object.values(body.errors)
          .flatMap((item) => (Array.isArray(item) ? item : [item]))
          .filter(Boolean)
          .map(String)
        const unique = [...new Set(values)]
        if (unique.length) return unique.join('; ')
      }
    }
    if (typeof error.status === 'number') return `Error ${error.status}`
  }
  return String(error)
}

async function handle(res) {
  if (res.status === 204) return null
  const text = await res.text()
  if (!text) {
    if (!res.ok) {
      if (res.status === 401) clearAccessToken()
      const err = new Error(res.statusText || 'Request failed')
      err.status = res.status
      throw err
    }
    return null
  }
  let data
  try {
    data = JSON.parse(text)
  } catch {
    if (!res.ok) {
      if (res.status === 401) clearAccessToken()
      const err = new Error(text || res.statusText)
      err.status = res.status
      throw err
    }
    return null
  }
  if (!res.ok) {
    if (res.status === 401) clearAccessToken()
    const msg = data.message || data.error || res.statusText
    const err = new Error(msg)
    err.status = res.status
    err.body = data
    throw err
  }
  return data
}

export async function apiGet(path) {
  const res = await fetch(`/api${path}`, { credentials: 'include', headers: authHeaders() })
  return handle(res)
}

export async function apiSend(path, method, body) {
  const opts = {
    method,
    credentials: 'include',
  }
  if (body !== undefined) {
    opts.headers = authHeaders(jsonHeaders)
    opts.body = JSON.stringify(body)
  } else {
    opts.headers = authHeaders()
  }
  const res = await fetch(`/api${path}`, opts)
  return handle(res)
}

export async function apiDelete(path) {
  const res = await fetch(`/api${path}`, { method: 'DELETE', credentials: 'include', headers: authHeaders() })
  return handle(res)
}

export async function apiUpload(path, formData) {
  const res = await fetch(`/api${path}`, {
    method: 'POST',
    credentials: 'include',
    body: formData,
    headers: authHeaders(),
  })
  return handle(res)
}
