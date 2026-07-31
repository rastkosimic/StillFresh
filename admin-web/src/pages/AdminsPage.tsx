import { FormEvent, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getAllAuthUsers, deleteAdmin } from '../api/adminUsersApi'
import { registerAdmin } from '../api/vendorsApi'
import { getErrorMessage } from '../api/apiClient'
import { Button, Card, ErrorAlert, LoadingSpinner, PageHeader, StatusBadge } from '../components/ui'
import { sortByLatest } from '../utils/sort'

export function AdminsPage() {
  const qc = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [actionError, setActionError] = useState('')
  const [form, setForm] = useState({
    username: '',
    email: '',
    password: '',
    address: '',
    phone: '',
    zipCode: '',
  })

  const authUsers = useQuery({ queryKey: ['authUsers'], queryFn: getAllAuthUsers })
  const sortedAdmins = useMemo(
    () => sortByLatest(authUsers.data?.filter((u) => u.role === 'ADMIN' || u.role === 'SUPER_ADMIN') ?? []),
    [authUsers.data]
  )

  const invalidate = () => qc.invalidateQueries({ queryKey: ['authUsers'] })

  const deleteMut = useMutation({
    mutationFn: deleteAdmin,
    onSuccess: invalidate,
    onError: (e) => setActionError(getErrorMessage(e)),
  })

  const registerMut = useMutation({
    mutationFn: () => registerAdmin(form),
    onSuccess: () => {
      setActionError('')
      setShowForm(false)
      setForm({ username: '', email: '', password: '', address: '', phone: '', zipCode: '' })
      invalidate()
    },
    onError: (e) => setActionError(getErrorMessage(e)),
  })

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    registerMut.mutate()
  }

  if (authUsers.isLoading) return <LoadingSpinner />

  return (
    <div>
      <PageHeader title="Admin Users" subtitle="Manage platform administrators (SUPER_ADMIN)" />

      {actionError && (
        <div className="mb-4">
          <ErrorAlert message={actionError} />
        </div>
      )}

      <div className="mb-4">
        <Button onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancel' : 'Register new admin'}
        </Button>
      </div>

      {showForm && (
        <Card className="mb-6 max-w-lg">
          <form onSubmit={handleSubmit} className="space-y-3">
            {(['username', 'email', 'password', 'address', 'phone', 'zipCode'] as const).map((field) => (
              <div key={field}>
                <label className="mb-1 block text-sm capitalize text-stone-600">{field}</label>
                <input
                  type={field === 'password' ? 'password' : 'text'}
                  required
                  value={form[field]}
                  onChange={(e) => setForm({ ...form, [field]: e.target.value })}
                  className="w-full rounded-lg border border-stone-300 px-3 py-2 text-sm"
                />
              </div>
            ))}
            <Button type="submit" disabled={registerMut.isPending}>
              Create admin
            </Button>
          </form>
        </Card>
      )}

      <div className="overflow-hidden rounded-xl border border-stone-200 bg-white shadow-sm">
        <table className="min-w-full text-sm">
          <thead className="bg-stone-50 text-left text-stone-500">
            <tr>
              <th className="px-4 py-3">ID</th>
              <th className="px-4 py-3">Username</th>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">Role</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Actions</th>
            </tr>
          </thead>
          <tbody>
            {sortedAdmins.map((a) => (
              <tr key={a.id} className="border-t">
                <td className="px-4 py-3">{a.id}</td>
                <td className="px-4 py-3">{a.username}</td>
                <td className="px-4 py-3">{a.email}</td>
                <td className="px-4 py-3">{a.role}</td>
                <td className="px-4 py-3">
                  <StatusBadge status={a.status} />
                </td>
                <td className="px-4 py-3">
                  {a.role === 'ADMIN' && (
                    <Button
                      variant="danger"
                      className="px-2 py-1 text-xs"
                      onClick={() => deleteMut.mutate(a.id)}
                    >
                      Delete
                    </Button>
                  )}
                  {a.role === 'SUPER_ADMIN' && (
                    <span className="text-xs text-stone-400">Protected</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
