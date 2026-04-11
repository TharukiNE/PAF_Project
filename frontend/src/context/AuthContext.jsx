import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiGet, apiSend, clearAccessToken, persistAuthFromResponse, setAccessToken, getErrorMessage } from '../api/client'

const AuthContext = createContext(null)

/**
 * Provides authentication state and actions for the app.
 * Handles login, registration, current user refresh, and logout behavior.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(undefined)
  const [error, setError] = useState(null)
  const navigate = useNavigate()

  /**
   * Fetch the current logged-in user from the backend.
   * If the session is unauthorized, resets auth state to null.
   */
  const refresh = useCallback(async () => {
    setError(null)
    try {
      const me = await apiGet('/auth/me')
      setUser(me)
    } catch (e) {
      if (e.status === 401) {
        setUser(null)
      } else {
        setError(getErrorMessage(e))
        setUser(null)
      }
    }
  }, [])

  /**
   * Authenticate a user with email/password and persist the returned token.
   */
  const login = useCallback(
    async (email, password) => {
      clearAccessToken()
      const data = await apiSend('/auth/login', 'POST', { email, password })
      setUser(persistAuthFromResponse(data))
      navigate('/', { replace: true })
    },
    [navigate],
  )

  /**
   * Register a new account, persist the returned auth token, and redirect home.
   */
  const register = useCallback(
    async (email, name, password) => {
      clearAccessToken()
      const data = await apiSend('/auth/register', 'POST', { email, name, password })
      setUser(persistAuthFromResponse(data))
      navigate('/', { replace: true })
    },
    [navigate],
  )

  useEffect(() => {
    const url = new URL(window.location.href)
    const oauthToken = url.searchParams.get('accessToken')
    if (oauthToken) {
      setAccessToken(oauthToken)
      url.searchParams.delete('accessToken')
      url.searchParams.delete('expiresIn')
      const qs = url.searchParams.toString()
      window.history.replaceState({}, '', url.pathname + (qs ? `?${qs}` : '') + url.hash)
    }
    refresh()
  }, [refresh])

  const value = useMemo(
    () => ({
      user,
      loading: user === undefined,
      error,
      refresh,
      login,
      register,
      logout: () => {
        clearAccessToken()
        window.location.href = '/logout'
      },
    }),
    [user, error, refresh, login, register],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth outside AuthProvider')
  return ctx
}
