import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getAllCustomers } from '../api/customersApi'
import { getAllVendors, getPendingVendors } from '../api/vendorsApi'
import { getOrders } from '../api/ordersApi'
import { getPendingBankTransfers, getPlatformFee } from '../api/paymentsApi'
import { getAutoPayoutStatus } from '../api/ledgerApi'
import { useAuth } from '../auth/AuthContext'
import { ErrorAlert, KpiCard, LoadingSpinner, PageHeader } from '../components/ui'
import { sortOrdersByLatest } from '../utils/sort'

export function DashboardPage() {
  const { isSuperAdmin } = useAuth()

  const customers = useQuery({ queryKey: ['customers'], queryFn: getAllCustomers, retry: 1 })
  const vendors = useQuery({ queryKey: ['vendors'], queryFn: getAllVendors })
  const pendingVendors = useQuery({ queryKey: ['pendingVendors'], queryFn: getPendingVendors })
  const orders = useQuery({
    queryKey: ['orders', 'recent'],
    queryFn: () => getOrders({ page: 0, size: 100 }),
    enabled: isSuperAdmin,
  })
  const bankTransfers = useQuery({
    queryKey: ['bankTransfers', 'pending'],
    queryFn: () => getPendingBankTransfers(0, 1),
  })
  const autoPayout = useQuery({ queryKey: ['autoPayoutStatus'], queryFn: getAutoPayoutStatus })
  const platformFee = useQuery({ queryKey: ['platformFee'], queryFn: getPlatformFee })

  const recentOrders = useMemo(
    () => sortOrdersByLatest(orders.data?.content ?? []).slice(0, 5),
    [orders.data]
  )

  const loading =
    vendors.isLoading ||
    pendingVendors.isLoading ||
    bankTransfers.isLoading ||
    autoPayout.isLoading ||
    platformFee.isLoading

  if (loading) return <LoadingSpinner />

  const failedMetrics = [
    customers.isError && 'Customers',
    vendors.isError && 'Vendors',
    pendingVendors.isError && 'Pending vendors',
    bankTransfers.isError && 'Bank transfers',
    autoPayout.isError && 'Payout pipeline',
    platformFee.isError && 'Platform fee',
  ].filter(Boolean) as string[]

  return (
    <div>
      <PageHeader
        title="Dashboard"
        subtitle="Platform overview across customers, vendors, orders, and payments"
      />

      {failedMetrics.length > 0 && (
        <div className="mb-4">
          <ErrorAlert message={`Could not load: ${failedMetrics.join(', ')}.`} />
        </div>
      )}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard
          label="Customers"
          value={customers.isError ? '—' : customers.data?.length ?? (customers.isLoading ? '…' : '—')}
        />
        <KpiCard label="Vendors" value={vendors.data?.length ?? '—'} />
        <KpiCard label="Pending vendor approvals" value={pendingVendors.data?.length ?? '—'} />
        <KpiCard
          label="Pending bank transfers"
          value={bankTransfers.data?.totalElements ?? '—'}
        />
        {isSuperAdmin && (
          <KpiCard label="Total orders (sample)" value={orders.data?.totalElements ?? '—'} hint="Full list in Orders" />
        )}
        <KpiCard
          label="Platform fee"
          value={platformFee.data ? `${platformFee.data.feePercent}%` : '—'}
        />
        <KpiCard
          label="Auto payout pipeline"
          value={String(autoPayout.data?.paused === true ? 'Paused' : 'Active')}
        />
      </div>

      {isSuperAdmin && recentOrders.length > 0 && (
        <div className="mt-8 rounded-xl border border-stone-200 bg-white p-5 shadow-sm">
          <h2 className="mb-4 text-lg font-semibold text-stone-900">Recent orders</h2>
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm">
              <thead>
                <tr className="border-b text-left text-stone-500">
                  <th className="pb-2 pr-4">ID</th>
                  <th className="pb-2 pr-4">Customer</th>
                  <th className="pb-2 pr-4">Vendor</th>
                  <th className="pb-2 pr-4">Status</th>
                  <th className="pb-2">Total</th>
                </tr>
              </thead>
              <tbody>
                {recentOrders.map((o) => (
                  <tr key={o.id} className="border-b border-stone-100">
                    <td className="py-2 pr-4">{o.id}</td>
                    <td className="py-2 pr-4">{o.userId}</td>
                    <td className="py-2 pr-4">{o.locationName || o.vendorId}</td>
                    <td className="py-2 pr-4">{o.status}</td>
                    <td className="py-2">
                      {o.totalPrice} {o.currency}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
