export type Role = 'USER' | 'ADMIN' | 'SUPER_ADMIN' | 'VENDOR' | 'VENDOR_ADMIN'
export type Status = 'ACTIVE' | 'INACTIVE' | 'DELETED' | 'PENDING'

export interface AuthUser {
  id: number
  username: string
  email: string
  role: Role
  status: Status
  oauth2Provider?: string
}

export interface CustomerProfile {
  id: number
  username: string
  email: string
  role: Role
  status: Status
  firstName?: string
  lastName?: string
  address?: string
  country?: string
  birthday?: string
  dietaryPreference?: string
  phoneNumber?: string
  bypassStrikeCount?: number
  noShowStrikeCount?: number
}

export interface Vendor {
  id: number
  username: string
  email: string
  address?: string
  phone?: string
  role: Role
  status: Status
  businessType?: string
  averageRating?: number
  reviewsCount?: number
  imageUrl?: string
  website?: string
  aboutBusiness?: string
  contactPerson?: string
  zipCode?: string
  latitude?: number
  longitude?: number
  stripeAccountId?: string
  payoutModel?: string
  paymentProvider?: string
  country?: string
  chainName?: string
  locationName?: string
  balance?: number
  bankAccountHolderName?: string
  bankIban?: string
  bankName?: string
}

export interface Order {
  id: number
  offerId: number
  userId: number
  quantity: number
  totalPrice: number
  vendorId: number
  unitPrice: number
  currency: string
  paymentIntentId?: string
  paymentMethod?: string
  bankTransferReference?: string
  status: string
  pickupBy?: string
  locationName?: string
  chainName?: string
  offerName?: string
  createdAt?: string
}

export interface SpringPage<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first?: boolean
  last?: boolean
}

export interface LoginResponse {
  accessJwt: string
  refreshToken: string
  jwt: string
  role: Role
  accountWasDeleted?: boolean
}

export interface PayoutBatch {
  id: number
  status: string
  scheduledAt: string
  approvedAt?: string
  approvedBy?: string
  processedAt?: string
  totalAmountCents: number
  currency: string
  itemCount: number
  completedCount: number
  failedCount: number
}

export interface VendorPayoutItem {
  id: number
  batchId: number
  vendorId: number
  amountCents: number
  currency: string
  status: string
  iban?: string
  accountHolderName?: string
}

export interface BankTransferPayment {
  id: number
  reference: string
  orderId: number
  userId: number
  amountCents: number
  currency: string
  status: string
  createdAt?: string
}

export interface VendorDashboard {
  vendorId: number
  period?: string
  summary?: {
    totalUnitsSold: number
    totalGrossRevenueCents: number
    totalVendorEarningsCents: number
    totalPlatformFeeCents: number
    activeOrderCount: number
    sellThroughRate: number
  }
  ratings?: { averageRating: number; reviewsCount: number }
  payoutBalance?: {
    unsettledCents: number
    currency: string
    lastPayoutAmountCents?: number
    lastPayoutAt?: string
  }
  activeOrders?: Array<{
    orderId: number
    offerName?: string
    quantity: number
    totalPrice: number
    status: string
  }>
}
