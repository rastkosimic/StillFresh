import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getPlatformFee,
  updatePlatformFee,
  getMorPendingPayouts,
  getMorVendorBalances,
  getMorPayoutSummary,
  getPendingBankTransfers,
  confirmBankTransfer,
  cancelBankTransfer,
} from '../api/paymentsApi'
import {
  getPayoutBatches,
  approveBatch,
  executeBatch,
  pauseAutoPayouts,
  resumeAutoPayouts,
  getAutoPayoutStatus,
  runPayoutJob,
  holdBatch,
  releaseBatch,
  cancelBatch,
} from '../api/ledgerApi'
import { getErrorMessage } from '../api/apiClient'
import { Button, Card, ErrorAlert, LoadingSpinner, PageHeader, StatusBadge } from '../components/ui'
import { sortByLatest } from '../utils/sort'
import type { PayoutBatch } from '../types'

type Tab = 'overview' | 'ledger' | 'mor' | 'bank'

export function PaymentsPage() {
  const qc = useQueryClient()
  const [tab, setTab] = useState<Tab>('overview')
  const [feeInput, setFeeInput] = useState('')
  const [selectedBatch, setSelectedBatch] = useState<PayoutBatch | null>(null)
  const [actionError, setActionError] = useState('')

  const platformFee = useQuery({ queryKey: ['platformFee'], queryFn: getPlatformFee })
  const autoStatus = useQuery({ queryKey: ['autoPayoutStatus'], queryFn: getAutoPayoutStatus })
  const batches = useQuery({ queryKey: ['payoutBatches'], queryFn: () => getPayoutBatches(0, 20) })
  const morPending = useQuery({ queryKey: ['morPending'], queryFn: getMorPendingPayouts, enabled: tab === 'mor' })
  const morBalances = useQuery({ queryKey: ['morBalances'], queryFn: getMorVendorBalances, enabled: tab === 'mor' })
  const morSummary = useQuery({ queryKey: ['morSummary'], queryFn: getMorPayoutSummary, enabled: tab === 'mor' })
  const bankTransfers = useQuery({
    queryKey: ['bankTransfers'],
    queryFn: () => getPendingBankTransfers(0, 20),
    enabled: tab === 'bank',
  })

  const sortedBatches = useMemo(
    () => sortByLatest(batches.data?.content ?? [], (b) => b.scheduledAt, (b) => b.processedAt),
    [batches.data]
  )

  const sortedBankTransfers = useMemo(
    () => sortByLatest(bankTransfers.data?.content ?? [], (bt) => bt.createdAt),
    [bankTransfers.data]
  )

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['platformFee'] })
    qc.invalidateQueries({ queryKey: ['payoutBatches'] })
    qc.invalidateQueries({ queryKey: ['autoPayoutStatus'] })
    qc.invalidateQueries({ queryKey: ['bankTransfers'] })
    qc.invalidateQueries({ queryKey: ['morPending'] })
  }

  const action = useMutation({
    mutationFn: async (payload: { type: string; id?: number; ref?: string; fee?: number }) => {
      switch (payload.type) {
        case 'updateFee':
          return updatePlatformFee(payload.fee!)
        case 'approve':
          return approveBatch(payload.id!)
        case 'execute':
          return executeBatch(payload.id!)
        case 'hold':
          return holdBatch(payload.id!)
        case 'release':
          return releaseBatch(payload.id!)
        case 'cancelBatch':
          return cancelBatch(payload.id!)
        case 'run':
          return runPayoutJob()
        case 'pause':
          return pauseAutoPayouts()
        case 'resume':
          return resumeAutoPayouts()
        case 'confirmBank':
          return confirmBankTransfer(payload.ref!)
        case 'cancelBank':
          return cancelBankTransfer(payload.ref!)
      }
    },
    onSuccess: () => {
      setActionError('')
      invalidate()
    },
    onError: (e) => setActionError(getErrorMessage(e)),
  })

  const tabs: { id: Tab; label: string }[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'ledger', label: 'Ledger batches' },
    { id: 'mor', label: 'MoR payouts' },
    { id: 'bank', label: 'Bank transfers' },
  ]

  return (
    <div>
      <PageHeader title="Payments & Payouts" subtitle="Platform fee, ledger pipeline, MoR, and bank transfers" />

      {actionError && (
        <div className="mb-4">
          <ErrorAlert message={actionError} />
        </div>
      )}

      <div className="mb-6 flex flex-wrap gap-2">
        {tabs.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => setTab(t.id)}
            className={`rounded-lg px-4 py-2 text-sm font-medium ${
              tab === t.id ? 'bg-brand-green text-white' : 'bg-white text-stone-600 ring-1 ring-stone-200'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'overview' && (
        <div className="grid gap-4 lg:grid-cols-2">
          <Card>
            <h3 className="font-semibold text-brand-green">Platform fee</h3>
            <p className="mt-2 text-3xl font-bold">
              {platformFee.data ? `${platformFee.data.feePercent}%` : '—'}
            </p>
            <div className="mt-4 flex gap-2">
              <input
                type="number"
                min={0}
                max={100}
                step={0.1}
                placeholder="New fee %"
                value={feeInput}
                onChange={(e) => setFeeInput(e.target.value)}
                className="rounded-lg border border-stone-300 px-3 py-2 text-sm"
              />
              <Button
                onClick={() => action.mutate({ type: 'updateFee', fee: parseFloat(feeInput) })}
                disabled={!feeInput}
              >
                Update
              </Button>
            </div>
          </Card>

          <Card>
            <h3 className="font-semibold text-brand-green">Auto payout pipeline</h3>
            {autoStatus.isLoading ? (
              <LoadingSpinner />
            ) : (
              <>
                <p className="mt-2 text-lg">
                  Status:{' '}
                  <StatusBadge status={autoStatus.data?.paused ? 'PAUSED' : 'ACTIVE'} />
                </p>
                <div className="mt-4 flex flex-wrap gap-2">
                  <Button onClick={() => action.mutate({ type: 'run' })}>Run payout job</Button>
                  <Button variant="ghost" onClick={() => action.mutate({ type: 'pause' })}>
                    Pause auto
                  </Button>
                  <Button variant="secondary" onClick={() => action.mutate({ type: 'resume' })}>
                    Resume auto
                  </Button>
                </div>
                <pre className="mt-4 overflow-auto rounded bg-stone-50 p-2 text-xs">
                  {JSON.stringify(autoStatus.data, null, 2)}
                </pre>
              </>
            )}
          </Card>
        </div>
      )}

      {tab === 'ledger' && (
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2 overflow-hidden rounded-xl border border-stone-200 bg-white shadow-sm">
            {batches.isLoading ? (
              <LoadingSpinner />
            ) : (
              <table className="min-w-full text-sm">
                <thead className="bg-stone-50 text-left text-stone-500">
                  <tr>
                    <th className="px-4 py-3">Batch</th>
                    <th className="px-4 py-3">Status</th>
                    <th className="px-4 py-3">Items</th>
                    <th className="px-4 py-3">Total</th>
                    <th className="px-4 py-3">Scheduled</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedBatches.map((b) => (
                    <tr
                      key={b.id}
                      onClick={() => setSelectedBatch(b)}
                      className={`cursor-pointer border-t hover:bg-brand-cream/50 ${selectedBatch?.id === b.id ? 'bg-brand-cream' : ''}`}
                    >
                      <td className="px-4 py-3">#{b.id}</td>
                      <td className="px-4 py-3">
                        <StatusBadge status={b.status} />
                      </td>
                      <td className="px-4 py-3">
                        {b.completedCount}/{b.itemCount} ({b.failedCount} failed)
                      </td>
                      <td className="px-4 py-3">
                        {(b.totalAmountCents / 100).toFixed(2)} {b.currency}
                      </td>
                      <td className="px-4 py-3 text-xs">{b.scheduledAt}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>

          <Card>
            {selectedBatch ? (
              <>
                <h3 className="font-semibold">Batch #{selectedBatch.id}</h3>
                <div className="mt-4 flex flex-wrap gap-2">
                  <Button onClick={() => action.mutate({ type: 'approve', id: selectedBatch.id })}>Approve</Button>
                  <Button variant="secondary" onClick={() => action.mutate({ type: 'execute', id: selectedBatch.id })}>
                    Execute
                  </Button>
                  <Button variant="ghost" onClick={() => action.mutate({ type: 'hold', id: selectedBatch.id })}>
                    Hold
                  </Button>
                  <Button variant="ghost" onClick={() => action.mutate({ type: 'release', id: selectedBatch.id })}>
                    Release
                  </Button>
                  <Button variant="danger" onClick={() => action.mutate({ type: 'cancelBatch', id: selectedBatch.id })}>
                    Cancel
                  </Button>
                </div>
              </>
            ) : (
              <p className="text-sm text-stone-500">Select a batch to manage</p>
            )}
          </Card>
        </div>
      )}

      {tab === 'mor' && (
        <div className="space-y-4">
          {morSummary.data && (
            <Card>
              <h3 className="font-semibold">MoR payout summary</h3>
              <pre className="mt-2 overflow-auto text-xs">{JSON.stringify(morSummary.data, null, 2)}</pre>
            </Card>
          )}
          <Card>
            <h3 className="font-semibold">Pending MoR payouts</h3>
            {morPending.isLoading ? (
              <LoadingSpinner />
            ) : (
              <pre className="mt-2 max-h-64 overflow-auto text-xs">
                {JSON.stringify(morPending.data, null, 2)}
              </pre>
            )}
          </Card>
          <Card>
            <h3 className="font-semibold">MoR vendor balances</h3>
            {morBalances.isLoading ? (
              <LoadingSpinner />
            ) : (
              <pre className="mt-2 max-h-64 overflow-auto text-xs">
                {JSON.stringify(morBalances.data, null, 2)}
              </pre>
            )}
          </Card>
        </div>
      )}

      {tab === 'bank' && (
        <div className="overflow-hidden rounded-xl border border-stone-200 bg-white shadow-sm">
          {bankTransfers.isLoading ? (
            <LoadingSpinner />
          ) : (
            <table className="min-w-full text-sm">
              <thead className="bg-stone-50 text-left text-stone-500">
                <tr>
                  <th className="px-4 py-3">Reference</th>
                  <th className="px-4 py-3">Order</th>
                  <th className="px-4 py-3">User</th>
                  <th className="px-4 py-3">Amount</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {sortedBankTransfers.map((bt) => (
                  <tr key={bt.id} className="border-t">
                    <td className="px-4 py-3 font-mono text-xs">{bt.reference}</td>
                    <td className="px-4 py-3">{bt.orderId}</td>
                    <td className="px-4 py-3">{bt.userId}</td>
                    <td className="px-4 py-3">
                      {(bt.amountCents / 100).toFixed(2)} {bt.currency}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={bt.status} />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex gap-2">
                        <Button
                          className="px-2 py-1 text-xs"
                          onClick={() => action.mutate({ type: 'confirmBank', ref: bt.reference })}
                        >
                          Confirm
                        </Button>
                        <Button
                          variant="ghost"
                          className="px-2 py-1 text-xs"
                          onClick={() => action.mutate({ type: 'cancelBank', ref: bt.reference })}
                        >
                          Cancel
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  )
}
