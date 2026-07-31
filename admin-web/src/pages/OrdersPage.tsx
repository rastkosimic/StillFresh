import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getOrders, updateOrderStatus, cancelOrder, confirmPickup, deleteOrder } from '../api/ordersApi'
import { getErrorMessage } from '../api/apiClient'
import { Button, ErrorAlert, LoadingSpinner, PageHeader, StatusBadge } from '../components/ui'
import { sortOrdersByLatest } from '../utils/sort'
import type { Order } from '../types'

const ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'PROCESSING', 'READY', 'COMPLETED', 'CANCELLED', 'EXPIRED']

export function OrdersPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [statusFilter, setStatusFilter] = useState('')
  const [selected, setSelected] = useState<Order | null>(null)
  const [newStatus, setNewStatus] = useState('')
  const [actionError, setActionError] = useState('')

  const orders = useQuery({
    queryKey: ['orders', page, statusFilter],
    queryFn: () => getOrders({ page, size: 20, status: statusFilter || undefined }),
  })

  const sortedOrders = useMemo(
    () => sortOrdersByLatest(orders.data?.content ?? []),
    [orders.data]
  )

  const action = useMutation({
    mutationFn: async ({ type, id }: { type: string; id: number }) => {
      switch (type) {
        case 'status':
          return updateOrderStatus(id, newStatus)
        case 'cancel':
          return cancelOrder(id, 'Admin cancellation')
        case 'pickup':
          return confirmPickup(id)
        case 'delete':
          return deleteOrder(id)
      }
    },
    onSuccess: () => {
      setActionError('')
      qc.invalidateQueries({ queryKey: ['orders'] })
    },
    onError: (e) => setActionError(getErrorMessage(e)),
  })

  if (orders.isLoading) return <LoadingSpinner />

  const data = orders.data

  return (
    <div>
      <PageHeader title="Orders" subtitle="Platform-wide order management (SUPER_ADMIN)" />

      {actionError && (
        <div className="mb-4">
          <ErrorAlert message={actionError} />
        </div>
      )}

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <select
          value={statusFilter}
          onChange={(e) => {
            setStatusFilter(e.target.value)
            setPage(0)
          }}
          className="rounded-lg border border-stone-300 px-3 py-2 text-sm"
        >
          <option value="">All statuses</option>
          {ORDER_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <span className="text-sm text-stone-500">
          {data?.totalElements ?? 0} orders total
        </span>
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2 overflow-hidden rounded-xl border border-stone-200 bg-white shadow-sm">
          <table className="min-w-full text-sm">
            <thead className="bg-stone-50 text-left text-stone-500">
              <tr>
                <th className="px-4 py-3">ID</th>
                <th className="px-4 py-3">Customer</th>
                <th className="px-4 py-3">Vendor</th>
                <th className="px-4 py-3">Offer</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Total</th>
              </tr>
            </thead>
            <tbody>
              {sortedOrders.map((o) => (
                <tr
                  key={o.id}
                  onClick={() => {
                    setSelected(o)
                    setNewStatus(o.status)
                  }}
                  className={`cursor-pointer border-t hover:bg-brand-cream/50 ${selected?.id === o.id ? 'bg-brand-cream' : ''}`}
                >
                  <td className="px-4 py-3">{o.id}</td>
                  <td className="px-4 py-3">{o.userId}</td>
                  <td className="px-4 py-3">{o.locationName || o.vendorId}</td>
                  <td className="px-4 py-3">{o.offerName || o.offerId}</td>
                  <td className="px-4 py-3">
                    <StatusBadge status={o.status} />
                  </td>
                  <td className="px-4 py-3">
                    {o.totalPrice} {o.currency}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          <div className="flex items-center justify-between border-t px-4 py-3">
            <Button variant="ghost" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
              Previous
            </Button>
            <span className="text-sm text-stone-500">
              Page {(data?.number ?? 0) + 1} of {data?.totalPages ?? 1}
            </span>
            <Button
              variant="ghost"
              disabled={data?.last ?? true}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        </div>

        <div className="rounded-xl border border-stone-200 bg-white p-5 shadow-sm">
          {selected ? (
            <>
              <h3 className="font-semibold">Order #{selected.id}</h3>
              <dl className="mt-4 space-y-2 text-sm">
                <div>
                  <dt className="text-stone-500">Payment</dt>
                  <dd>{selected.paymentMethod || 'STRIPE'}</dd>
                </div>
                <div>
                  <dt className="text-stone-500">Quantity</dt>
                  <dd>{selected.quantity}</dd>
                </div>
                <div>
                  <dt className="text-stone-500">Pickup by</dt>
                  <dd>{selected.pickupBy || '—'}</dd>
                </div>
                <div>
                  <dt className="text-stone-500">Payment intent</dt>
                  <dd className="break-all text-xs">{selected.paymentIntentId || '—'}</dd>
                </div>
              </dl>

              <div className="mt-4">
                <label className="text-sm text-stone-600">Update status</label>
                <div className="mt-1 flex gap-2">
                  <select
                    value={newStatus}
                    onChange={(e) => setNewStatus(e.target.value)}
                    className="flex-1 rounded-lg border border-stone-300 px-2 py-1.5 text-sm"
                  >
                    {ORDER_STATUSES.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                  <Button onClick={() => action.mutate({ type: 'status', id: selected.id })}>Save</Button>
                </div>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                <Button variant="secondary" onClick={() => action.mutate({ type: 'pickup', id: selected.id })}>
                  Confirm pickup
                </Button>
                <Button variant="ghost" onClick={() => action.mutate({ type: 'cancel', id: selected.id })}>
                  Cancel
                </Button>
                <Button variant="danger" onClick={() => action.mutate({ type: 'delete', id: selected.id })}>
                  Delete
                </Button>
              </div>
            </>
          ) : (
            <p className="text-sm text-stone-500">Select an order to manage</p>
          )}
        </div>
      </div>
    </div>
  )
}
