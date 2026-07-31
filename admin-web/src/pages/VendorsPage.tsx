import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getAllVendors,
  getPendingVendors,
  verifyAndActivate,
  activateVendor,
  deactivateVendor,
  deleteVendor,
  getVendorDashboard,
} from '../api/vendorsApi'
import { getVendorBalance, getVendorLedger } from '../api/paymentsApi'
import { getErrorMessage } from '../api/apiClient'
import { Button, ErrorAlert, LoadingSpinner, PageHeader, StatusBadge } from '../components/ui'
import { sortByLatest } from '../utils/sort'
import type { Vendor } from '../types'

export function VendorsPage() {
  const qc = useQueryClient()
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [view, setView] = useState<'all' | 'pending'>('all')
  const [search, setSearch] = useState('')
  const [actionError, setActionError] = useState('')

  const allVendors = useQuery({ queryKey: ['vendors'], queryFn: getAllVendors })
  const pending = useQuery({ queryKey: ['pendingVendors'], queryFn: getPendingVendors })
  const dashboard = useQuery({
    queryKey: ['vendorDashboard', selectedId],
    queryFn: () => getVendorDashboard(selectedId!),
    enabled: !!selectedId,
  })
  const balance = useQuery({
    queryKey: ['vendorBalance', selectedId],
    queryFn: () => getVendorBalance(selectedId!),
    enabled: !!selectedId,
  })
  const ledger = useQuery({
    queryKey: ['vendorLedger', selectedId],
    queryFn: () => getVendorLedger(selectedId!, 0, 10),
    enabled: !!selectedId,
  })

  const list = view === 'pending' ? pending.data ?? [] : allVendors.data ?? []
  const filtered = useMemo(() => {
    const filteredList = !search
      ? list
      : list.filter(
          (v) =>
            v.email?.toLowerCase().includes(search.toLowerCase()) ||
            v.username?.toLowerCase().includes(search.toLowerCase()) ||
            v.locationName?.toLowerCase().includes(search.toLowerCase()) ||
            v.chainName?.toLowerCase().includes(search.toLowerCase())
        )
    return sortByLatest(filteredList)
  }, [list, search])

  const selected = list.find((v) => v.id === selectedId) || allVendors.data?.find((v) => v.id === selectedId)

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['vendors'] })
    qc.invalidateQueries({ queryKey: ['pendingVendors'] })
  }

  const action = useMutation({
    mutationFn: async ({ type, id }: { type: string; id: number }) => {
      switch (type) {
        case 'verify':
          return verifyAndActivate(id)
        case 'activate':
          return activateVendor(id)
        case 'deactivate':
          return deactivateVendor(id)
        case 'delete':
          return deleteVendor(id)
      }
    },
    onSuccess: () => {
      setActionError('')
      invalidate()
    },
    onError: (e) => setActionError(getErrorMessage(e)),
  })

  if (allVendors.isLoading) return <LoadingSpinner />

  return (
    <div>
      <PageHeader title="Vendors" subtitle="Manage vendor accounts, approvals, and performance" />

      {actionError && (
        <div className="mb-4">
          <ErrorAlert message={actionError} />
        </div>
      )}

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <input
          type="search"
          placeholder="Search vendors…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="rounded-lg border border-stone-300 px-3 py-2 text-sm focus:border-brand-green focus:outline-none"
        />
        <div className="flex rounded-lg border border-stone-200 bg-white p-1">
          <button
            type="button"
            onClick={() => setView('all')}
            className={`rounded-md px-3 py-1.5 text-sm ${view === 'all' ? 'bg-brand-green text-white' : 'text-stone-600'}`}
          >
            All ({allVendors.data?.length ?? 0})
          </button>
          <button
            type="button"
            onClick={() => setView('pending')}
            className={`rounded-md px-3 py-1.5 text-sm ${view === 'pending' ? 'bg-brand-orange text-white' : 'text-stone-600'}`}
          >
            Pending ({pending.data?.length ?? 0})
          </button>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 overflow-hidden rounded-xl border border-stone-200 bg-white shadow-sm">
          <table className="min-w-full text-sm">
            <thead className="bg-stone-50 text-left text-stone-500">
              <tr>
                <th className="px-4 py-3">ID</th>
                <th className="px-4 py-3">Business</th>
                <th className="px-4 py-3">Email</th>
                <th className="px-4 py-3">Role</th>
                <th className="px-4 py-3">Status</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((v: Vendor) => (
                <tr
                  key={v.id}
                  onClick={() => setSelectedId(v.id)}
                  className={`cursor-pointer border-t hover:bg-brand-cream/50 ${selectedId === v.id ? 'bg-brand-cream' : ''}`}
                >
                  <td className="px-4 py-3">{v.id}</td>
                  <td className="px-4 py-3">{v.locationName || v.chainName || v.username}</td>
                  <td className="px-4 py-3">{v.email}</td>
                  <td className="px-4 py-3">{v.role}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={v.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="rounded-xl border border-stone-200 bg-white p-5 shadow-sm">
          {selected ? (
            <>
              <h3 className="font-semibold">{selected.locationName || selected.email}</h3>
              <dl className="mt-4 space-y-2 text-sm">
                <div>
                  <dt className="text-stone-500">Payout model</dt>
                  <dd>{selected.payoutModel || '—'}</dd>
                </div>
                <div>
                  <dt className="text-stone-500">Payment provider</dt>
                  <dd>{selected.paymentProvider || '—'}</dd>
                </div>
                <div>
                  <dt className="text-stone-500">Rating</dt>
                  <dd>
                    {selected.averageRating?.toFixed(1) ?? '—'} ({selected.reviewsCount ?? 0} reviews)
                  </dd>
                </div>
                <div>
                  <dt className="text-stone-500">Address</dt>
                  <dd>{selected.address || '—'}</dd>
                </div>
              </dl>

              <div className="mt-4 flex flex-wrap gap-2">
                {view === 'pending' && (
                  <Button onClick={() => action.mutate({ type: 'verify', id: selected.id })}>Verify & activate</Button>
                )}
                {selected.status === 'ACTIVE' ? (
                  <Button variant="ghost" onClick={() => action.mutate({ type: 'deactivate', id: selected.id })}>
                    Deactivate
                  </Button>
                ) : (
                  <Button onClick={() => action.mutate({ type: 'activate', id: selected.id })}>Activate</Button>
                )}
                <Button variant="danger" onClick={() => action.mutate({ type: 'delete', id: selected.id })}>
                  Delete
                </Button>
              </div>

              {dashboard.data?.summary && (
                <div className="mt-6 rounded-lg bg-brand-cream/60 p-3 text-sm">
                  <p className="font-medium text-brand-green">7-day summary</p>
                  <p className="mt-1">Units sold: {dashboard.data.summary.totalUnitsSold}</p>
                  <p>Active orders: {dashboard.data.summary.activeOrderCount}</p>
                  <p>
                    Gross revenue:{' '}
                    {(dashboard.data.summary.totalGrossRevenueCents / 100).toFixed(2)}
                  </p>
                </div>
              )}

              {balance.data && (
                <div className="mt-4 text-sm">
                  <p className="font-medium text-stone-700">Ledger balance</p>
                  <pre className="mt-1 overflow-auto rounded bg-stone-50 p-2 text-xs">
                    {JSON.stringify(balance.data, null, 2)}
                  </pre>
                </div>
              )}

              {ledger.data && ledger.data.content.length > 0 && (
                <div className="mt-4">
                  <p className="text-sm font-medium text-stone-700">Recent ledger entries</p>
                  <ul className="mt-2 space-y-1 text-xs text-stone-600">
                    {ledger.data.content.slice(0, 5).map((e, i) => (
                      <li key={i}>{JSON.stringify(e)}</li>
                    ))}
                  </ul>
                </div>
              )}
            </>
          ) : (
            <p className="text-sm text-stone-500">Select a vendor to view details and actions</p>
          )}
        </div>
      </div>
    </div>
  )
}
