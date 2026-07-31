import { FormEvent, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/apiClient'
import { useAuth } from '../auth/AuthContext'
import { Button, ErrorAlert } from '../components/ui'

export function LoginPage() {
  const { login, isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(username, password)
      navigate('/')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-brand-cream px-4">
      <div className="w-full max-w-md rounded-2xl border border-stone-200 bg-white p-8 shadow-lg">
        <div className="mb-8 flex flex-col items-center text-center">
          <img src="/logo.png" alt="StillFresh" className="mb-4 h-20 w-20 rounded-full object-cover shadow-md" />
          <h1 className="text-2xl font-bold text-brand-green">StillFresh Admin</h1>
          <p className="mt-1 text-sm text-stone-600">Sign in with your admin credentials</p>
        </div>

        {error && (
          <div className="mb-4">
            <ErrorAlert message={error} />
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-700">Email or username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full rounded-lg border border-stone-300 px-3 py-2 focus:border-brand-green focus:outline-none focus:ring-1 focus:ring-brand-green"
              required
              autoComplete="username"
              placeholder="Email or username"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-700">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-lg border border-stone-300 px-3 py-2 focus:border-brand-green focus:outline-none focus:ring-1 focus:ring-brand-green"
              required
              autoComplete="current-password"
            />
          </div>
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign in'}
          </Button>
        </form>

        <p className="mt-6 text-center text-xs text-stone-500">
          First-time setup: create a SUPER_ADMIN via POST /admin/create-initial-admin
        </p>
      </div>
    </div>
  )
}
