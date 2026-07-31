import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getAllAuthUsers, activateUser, deactivateUser, deleteUser, promoteUser, demoteUser } from '../api/adminUsersApi'
import { getAllCustomers } from '../api/customersApi'
import { getOrders } from '../api/ordersApi'
import { getErrorMessage } from '../api/apiClient'
import { useAuth } from '../auth/AuthContext'
import { Button, ErrorAlert, LoadingSpinner, PageHeader, StatusBadge } from '../components/ui'
import { sortByLatest, sortOrdersByLatest } from '../utils/sort'
import type { CustomerProfile } from '../types'

export function CustomersPage() {
  const { isSuperAdmin } = useAuth()
  const qc = useQueryClient()
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [search, setSearch] = useState('')
  const [tab, setTab] = useState<'profiles' | 'auth'>('profiles')
  const [actionError, setActionError] = useState('')

  const profiles = useQuery({
    queryKey: ['customers'],
    queryFn: getAllCustomers,
    retry: 1,
  })
  const authUsers = useQuery({ queryKey: ['authUsers'], queryFn: getAllAuthUsers })
  const orders = useQuery({
    queryKey: ['orders', 'all'],
    queryFn: () => getOrders({ page: 0, size: 500 }),
    enabled: isSuperAdmin,
  })

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['customers'] })
    qc.invalidateQueries({ queryKey: ['authUsers'] })
  }

  const mutate = useMutation({
    mutationFn: async ({ action, id }: { action: string; id: number }) => {
      switch (action) {
        case 'activate':
          return activateUser(id)
        case 'deactivate':
          return deactivateUser(id)
        case 'delete':
          return deleteUser(id)
        case 'promote':
          return promoteUser(id)
        case 'demote':
          return demoteUser(id)
      }
    },
    onSuccess: () => {
      setActionError('')
      invalidate()
    },
    onError: (e) => setActionError(getErrorMessage(e)),
  })

  const customerUsers = useMemo(() => {
    const auth = authUsers.data?.filter((u) => u.role === 'USER') ?? []
    const profileMap = new Map(profiles.data?.map((p) => [p.id, p]))
    return auth.map((u) => ({ auth: u, profile: profileMap.get(u.id) }))
  }, [authUsers.data, profiles.data])

  const filteredProfiles = useMemo(() => {
    const list = profiles.data ?? []
    const filtered = !search
      ? list
      : list.filter(
          (p) =>
            p.email?.toLowerCase().includes(search.toLowerCase()) ||
            p.username?.toLowerCase().includes(search.toLowerCase()) ||
            `${p.firstName} ${p.lastName}`.toLowerCase().includes(search.toLowerCase())
        )
    return sortByLatest(filtered)
  }, [profiles.data, search])

  const filteredAuth = useMemo(() => {
    const filtered = !search
      ? customerUsers
      : customerUsers.filter(
          (c) =>
            c.auth.email?.toLowerCase().includes(search.toLowerCase()) ||
            c.auth.username?.toLowerCase().includes(search.toLowerCase())
        )
    return [...filtered].sort((a, b) => b.auth.id - a.auth.id)
  }, [customerUsers, search])

  const selectedProfile = profiles.data?.find((p) => p.id === selectedId)
  const selectedAuth = authUsers.data?.find((u) => u.id === selectedId)
  const customerOrders = useMemo(
    () => sortOrdersByLatest(orders.data?.content.filter((o) => o.userId === selectedId) ?? []),
    [orders.data, selectedId]
  )

  if (authUsers.isLoading) return <LoadingSpinner />

  const profilesUnavailable = profiles.isError

  return (
    <div>
      <PageHeader title="Customers" subtitle="Customer profiles and auth account management" />

      {actionError && (
        <div className="mb-4">
          <ErrorAlert message={actionError} />
        </div>
      )}

      {profilesUnavailable && (
        <div className="mb-4">
          <ErrorAlert message="Customer profiles could not be loaded. Auth accounts are still available." />
        </div>
      )}

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <input
          type="search"
          placeholder="Search by name or email…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="rounded-lg border border-stone-300 px-3 py-2 text-sm focus:border-brand-green focus:outline-none"
        />
        <div className="flex rounded-lg border border-stone-200 bg-white p-1">
          <button
            type="button"
            onClick={() => setTab('profiles')}
            className={`rounded-md px-3 py-1.5 text-sm ${tab === 'profiles' ? 'bg-brand-green text-white' : 'text-stone-600'}`}
          >
            Profiles
          </button>
          <button
            type="button"
            onClick={() => setTab('auth')}
            className={`rounded-md px-3 py-1.5 text-sm ${tab === 'auth' ? 'bg-brand-green text-white' : 'text-stone-600'}`}
          >
            Auth accounts
          </button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 overflow-hidden rounded-xl border border-stone-200 bg-white shadow-sm">
          <table className="min-w-full text-sm">
            <thead className="bg-stone-50 text-left text-stone-500">
              <tr>
                <th className="px-4 py-3">ID</th>
                <th className="px-4 py-3">Email</th>
                <th className="px-4 py-3">Name</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Role</th>
              </tr>
            </thead>
            <tbody>
              {tab === 'profiles' ? (
                profiles.isLoading ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-stone-500">
                      Loading profiles…
                    </td>
                  </tr>
                ) : filteredProfiles.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-stone-500">
                      {profilesUnavailable ? 'Profiles unavailable' : 'No customer profiles found'}
                    </td>
                  </tr>
                ) : (
                  filteredProfiles.map((p: CustomerProfile) => (
                    <tr
                      key={p.id}
                      onClick={() => setSelectedId(p.id)}
                      className={`cursor-pointer border-t hover:bg-brand-cream/50 ${selectedId === p.id ? 'bg-brand-cream' : ''}`}
                    >
                      <td className="px-4 py-3">{p.id}</td>
                      <td className="px-4 py-3">{p.email}</td>
                      <td className="px-4 py-3">
                        {[p.firstName, p.lastName].filter(Boolean).join(' ') || '—'}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={p.status} />
                      </td>
                      <td className="px-4 py-3">{p.role}</td>
                    </tr>
                  ))
                )
              ) : filteredAuth.length === 0 ? (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-stone-500">
                    No customer auth accounts found
                  </td>
                </tr>
              ) : (
                filteredAuth.map(({ auth }) => (
                    <tr
                      key={auth.id}
                      onClick={() => setSelectedId(auth.id)}
                      className={`cursor-pointer border-t hover:bg-brand-cream/50 ${selectedId === auth.id ? 'bg-brand-cream' : ''}`}
                    >
                      <td className="px-4 py-3">{auth.id}</td>
                      <td className="px-4 py-3">{auth.email}</td>
                      <td className="px-4 py-3">{auth.username}</td>
                      <td className="px-4 py-3">
                        <StatusBadge status={auth.status} />
                      </td>
                      <td className="px-4 py-3">{auth.role}</td>
                    </tr>
                  ))
              )}
            </tbody>
          </table>
        </div>

        <div className="rounded-xl border border-stone-200 bg-white p-5 shadow-sm">
          {selectedId ? (
            <>
              <h3 className="font-semibold text-stone-900">Customer #{selectedId}</h3>
              {selectedProfile && (
                <dl className="mt-4 space-y-2 text-sm">
                  <div>
                    <dt className="text-stone-500">Phone</dt>
                    <dd>{selectedProfile.phoneNumber || '—'}</dd>
                  </div>
                  <div>
                    <dt className="text-stone-500">Country</dt>
                    <dd>{selectedProfile.country || '—'}</dd>
                  </div>
                  <div>
                    <dt className="text-stone-500">Strikes</dt>
                    <dd>
                      Bypass: {selectedProfile.bypassStrikeCount ?? 0}, No-show:{' '}
                      {selectedProfile.noShowStrikeCount ?? 0}
                    </dd>
                  </div>
                </dl>
              )}
              {selectedAuth && (
                <div className="mt-4 flex flex-wrap gap-2">
                  {selectedAuth.status === 'ACTIVE' ? (
                    <Button variant="ghost" onClick={() => mutate.mutate({ action: 'deactivate', id: selectedId })}>
                      Deactivate
                    </Button>
                  ) : (
                    <Button onClick={() => mutate.mutate({ action: 'activate', id: selectedId })}>Activate</Button>
                  )}
                  {selectedAuth.role === 'USER' && (
                    <>
                      <Button variant="danger" onClick={() => mutate.mutate({ action: 'delete', id: selectedId })}>
                        Delete
                      </Button>
                      <Button variant="secondary" onClick={() => mutate.mutate({ action: 'promote', id: selectedId })}>
                        Promote to Admin
                      </Button>
                    </>
                  )}
                  {selectedAuth.role === 'ADMIN' && (
                    <Button variant="ghost" onClick={() => mutate.mutate({ action: 'demote', id: selectedId })}>
                      Demote
                    </Button>
                  )}
                </div>
              )}
              {isSuperAdmin && (
                <div className="mt-6">
                  <h4 className="text-sm font-medium text-stone-700">Recent orders</h4>
                  {customerOrders.length === 0 ? (
                    <p className="mt-2 text-sm text-stone-500">No orders found</p>
                  ) : (
                    <ul className="mt-2 space-y-1 text-sm">
                      {customerOrders.slice(0, 5).map((o) => (
                        <li key={o.id} className="flex justify-between">
                          <span>#{o.id}</span>
                          <StatusBadge status={o.status} />
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </>
          ) : (
            <p className="text-sm text-stone-500">Select a customer to view details and actions</p>
          )}
        </div>
      </div>
    </div>
  )
}
