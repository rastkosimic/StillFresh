import type { ReactNode } from 'react'

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <div className={`rounded-xl border border-stone-200 bg-white p-5 shadow-sm ${className}`}>
      {children}
    </div>
  )
}

export function PageHeader({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div className="mb-6">
      <h1 className="text-2xl font-semibold text-stone-900">{title}</h1>
      {subtitle && <p className="mt-1 text-sm text-stone-600">{subtitle}</p>}
    </div>
  )
}

export function Button({
  children,
  variant = 'primary',
  className = '',
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'danger' | 'ghost' }) {
  const base = 'inline-flex items-center justify-center rounded-lg px-4 py-2 text-sm font-medium transition disabled:opacity-50'
  const variants = {
    primary: 'bg-brand-orange text-white hover:bg-brand-orange-hover',
    secondary: 'bg-brand-green text-white hover:bg-brand-green-dark',
    danger: 'bg-red-600 text-white hover:bg-red-700',
    ghost: 'border border-stone-300 bg-white text-stone-700 hover:bg-stone-50',
  }
  return (
    <button className={`${base} ${variants[variant]} ${className}`} {...props}>
      {children}
    </button>
  )
}

export function StatusBadge({ status }: { status: string }) {
  const normalized = status?.toUpperCase() || 'UNKNOWN'
  const colors: Record<string, string> = {
    ACTIVE: 'bg-green-100 text-green-800',
    INACTIVE: 'bg-stone-100 text-stone-700',
    PENDING: 'bg-amber-100 text-amber-800',
    DELETED: 'bg-red-100 text-red-800',
    COMPLETED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-red-100 text-red-800',
    CONFIRMED: 'bg-blue-100 text-blue-800',
    PROCESSING: 'bg-purple-100 text-purple-800',
    READY: 'bg-teal-100 text-teal-800',
    EXPIRED: 'bg-orange-100 text-orange-800',
  }
  const cls = colors[normalized] || 'bg-stone-100 text-stone-700'
  return <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${cls}`}>{normalized}</span>
}

export function LoadingSpinner() {
  return (
    <div className="flex items-center justify-center py-12">
      <div className="h-8 w-8 animate-spin rounded-full border-4 border-brand-green border-t-transparent" />
    </div>
  )
}

export function ErrorAlert({ message }: { message: string }) {
  return (
    <div className="rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
      {message}
    </div>
  )
}

export function KpiCard({ label, value, hint }: { label: string; value: string | number; hint?: string }) {
  return (
    <Card>
      <p className="text-sm font-medium text-stone-500">{label}</p>
      <p className="mt-2 text-3xl font-bold text-brand-green">{value}</p>
      {hint && <p className="mt-1 text-xs text-stone-500">{hint}</p>}
    </Card>
  )
}
