/**
 * Centralized API Routes Configuration
 * All API endpoints are defined here for easy maintenance and updates
 */

export const API_ROUTES = {
  // Authentication routes
  auth: {
    login: '/auth/login',
    register: '/auth/register',
    googleLogin: '/auth/oauth2/google/login', // Backend endpoint: /auth/oauth2/google/login
    logout: '/auth/logout',
  },

  // User routes
  users: {
    profile: '/users/profile',
    favorites: {
      add: (offerId: string) => `/users/favorites/${offerId}`,
      remove: (offerId: string) => `/users/favorites/${offerId}`,
      list: '/users/favorites',
      check: (offerId: string) => `/users/favorites/${offerId}/check`,
      count: '/users/favorites/count',
    },
  },

  // Vendor routes
  vendors: {
    profile: '/vendors/profile',
    updateProfile: '/vendors/profile',
    paymentStatus: '/vendors/payment-status',
    stripeAccountStatus: '/vendors/stripe/account-status',
    stripeOnboardingLink: '/vendors/stripe/onboarding-link',
    paymentOnboardingLink: '/vendors/stripe/onboarding-link',
    stripeReturn: '/vendors/stripe/return',
    stripeRefresh: '/vendors/stripe/refresh',
    stripeAccount: '/vendors/stripe/account',
    stripeLoginLink: '/vendors/stripe/login-link',
    stripeBalance: '/vendors/stripe/balance',
    stripePayouts: '/vendors/stripe/payouts',
    stripePayout: (payoutId: string) => `/vendors/stripe/payouts/${payoutId}`,
    stripeRequirements: '/vendors/stripe/requirements',
    stripeBankAccounts: '/vendors/stripe/bank-accounts',
    stripeAddBankAccount: '/vendors/stripe/bank-accounts',
    stripeDeleteBankAccount: (bankAccountId: string) => `/vendors/stripe/bank-accounts/${bankAccountId}`,
    stripeSetDefaultBankAccount: (bankAccountId: string) => `/vendors/stripe/bank-accounts/${bankAccountId}/default`,
    stripeTransactions: '/vendors/stripe/transactions',
    stripeAccountUpdateLink: '/vendors/stripe/account-update-link',
    morBalance: '/vendors/mor/balance',
    morTransactions: '/vendors/mor/transactions',
    morPayouts: '/vendors/mor/payouts',
    morBankDetails: '/vendors/mor/bank-details',
    morPayoutRequest: '/vendors/mor/payout-request',
  },

  // Offer routes
  offers: {
    nearby: '/offers/nearby',
    list: '/offers',
    getOffer: (offerId: string) => `/offers/${offerId}`,
    categories: '/offers/categories',
  },

  // Order routes
  orders: {
    create: '/orders',
    list: '/orders',
    getOrder: (orderId: string) => `/orders/${orderId}`,
    updateStatus: (orderId: string) => `/orders/${orderId}/status`,
    cancel: (orderId: string) => `/orders/${orderId}/cancel`,
    reject: (orderId: string) => `/orders/${orderId}/reject`,
  },

  // Payment routes
  payment: {
    registerCard: '/payment/methods/card',
    registerBankAccount: '/payment/methods/bank-account',
    paymentMethods: '/payment/methods',
    getPaymentMethod: (paymentMethodId: string) => `/payment/methods/${paymentMethodId}`,
    setDefaultPaymentMethod: (paymentMethodId: string) => `/payment/methods/${paymentMethodId}/default`,
    deletePaymentMethod: (paymentMethodId: string) => `/payment/methods/${paymentMethodId}`,
    processPayment: '/payment/process',
    capture: (paymentIntentId: string) => `/payment/capture/${paymentIntentId}`,
    cancel: (paymentIntentId: string) => `/payment/cancel/${paymentIntentId}`,
  },

  // Notification routes (only these have /api prefix per API Gateway configuration)
  notifications: {
    fcmTokenRegister: '/api/notifications/fcm-token/register',
    fcmToken: '/api/notifications/fcm-token',
    getUserNotifications: '/api/notifications/user',
    markRead: (notificationId: string) => `/api/notifications/mark-read/${notificationId}`,
    markAllRead: '/api/notifications/mark-all-read',
    preferences: '/api/notifications/preferences',
    test: '/api/notifications/test',
  },
};


