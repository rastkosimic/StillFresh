# Complete MoR Payment Integration Guide for Mobile App

This guide provides a complete, ready-to-use integration for all MoR (Merchant of Record) payment endpoints in your mobile app. Follow this guide step-by-step to implement the full MoR payment flow.

## Table of Contents
1. [Quick Start](#quick-start)
2. [API Service Setup](#api-service-setup)
3. [Complete Endpoint Implementations](#complete-endpoint-implementations)
4. [State Management](#state-management)
5. [UI Components](#ui-components)
6. [Complete Integration Flow](#complete-integration-flow)
7. [Error Handling](#error-handling)
8. [Testing Guide](#testing-guide)

---

## Quick Start

### Prerequisites
- React Native or Flutter mobile app
- JWT authentication token stored securely
- Base API URL configured

### Base Configuration

```javascript
// config/api.js
export const API_BASE_URL = 'https://your-api-domain.com'; // or 'http://localhost:8083' for dev
export const API_PREFIX = '/vendors';

// Get stored JWT token
export const getAuthToken = async () => {
  // React Native: import AsyncStorage from '@react-native-async-storage/async-storage';
  // return await AsyncStorage.getItem('authToken');
  
  // Flutter: Use shared_preferences package
  // final prefs = await SharedPreferences.getInstance();
  // return prefs.getString('authToken');
};
```

---

## API Service Setup

### Complete API Service Class

```javascript
// services/morPaymentService.js

import { API_BASE_URL, API_PREFIX, getAuthToken } from '../config/api';

class MoRPaymentService {
  /**
   * Generic API request handler
   */
  async makeRequest(endpoint, options = {}) {
    try {
      const token = await getAuthToken();
      if (!token) {
        throw new Error('No authentication token found');
      }

      const url = `${API_BASE_URL}${API_PREFIX}${endpoint}`;
      const config = {
        ...options,
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
          ...options.headers,
        },
      };

      const response = await fetch(url, config);
      
      // Handle 401 Unauthorized
      if (response.status === 401) {
        // Clear token and redirect to login
        await this.clearAuthToken();
        throw new Error('Session expired. Please log in again.');
      }

      const data = await response.json();
      
      if (!response.ok) {
        throw new Error(data.error || `Request failed with status ${response.status}`);
      }

      return { success: true, data };
    } catch (error) {
      return { success: false, error: error.message };
    }
  }

  async clearAuthToken() {
    // React Native: await AsyncStorage.removeItem('authToken');
    // Flutter: await prefs.remove('authToken');
  }

  // ========== Unified Payment Endpoints ==========

  /**
   * Get payment account status
   * Returns payoutModel (CONNECT or MOR) and account readiness
   */
  async getPaymentStatus() {
    return this.makeRequest('/payment/status', { method: 'GET' });
  }

  /**
   * Get payment onboarding link
   * For MoR: returns link to bank details form
   * For CONNECT: returns Stripe onboarding URL
   */
  async getPaymentOnboardingLink() {
    return this.makeRequest('/payment/onboarding-link', { method: 'POST' });
  }

  // ========== MoR Payment Endpoints ==========

  /**
   * Get MoR vendor balance
   * Returns balance in cents, currency, and formatted balance
   */
  async getMoRBalance() {
    return this.makeRequest('/mor/balance', { method: 'GET' });
  }

  /**
   * Get MoR transaction history
   * @param {number} limit - Maximum number of transactions (default: 50)
   */
  async getMoRTransactions(limit = 50) {
    return this.makeRequest(`/mor/transactions?limit=${limit}`, { method: 'GET' });
  }

  /**
   * Submit bank details for MoR vendor
   * @param {Object} bankDetails - Bank account information
   */
  async submitBankDetails(bankDetails) {
    return this.makeRequest('/mor/bank-details', {
      method: 'POST',
      body: JSON.stringify(bankDetails),
    });
  }

  /**
   * Get MoR payout history
   */
  async getMoRPayouts() {
    return this.makeRequest('/mor/payouts', { method: 'GET' });
  }

  /**
   * Request MoR payout
   * @param {number} amount - Amount in cents
   * @param {string} currency - Currency code (default: "EUR")
   * @param {string} description - Payout description
   */
  async requestMoRPayout(amount, currency = 'EUR', description = 'Manual payout request') {
    return this.makeRequest('/mor/request-payout', {
      method: 'POST',
      body: JSON.stringify({ amount, currency, description }),
    });
  }
}

export default new MoRPaymentService();
```

---

## Complete Endpoint Implementations

### 1. Payment Status Check

```javascript
// hooks/usePaymentStatus.js (React Native) or providers/PaymentProvider.dart (Flutter)

import { useState, useEffect } from 'react';
import morPaymentService from '../services/morPaymentService';

export const usePaymentStatus = () => {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isMoR, setIsMoR] = useState(false);

  useEffect(() => {
    checkPaymentStatus();
  }, []);

  const checkPaymentStatus = async () => {
    setLoading(true);
    setError(null);
    
    const result = await morPaymentService.getPaymentStatus();
    
    if (result.success) {
      setStatus(result.data);
      setIsMoR(result.data.payoutModel === 'MOR');
    } else {
      setError(result.error);
    }
    
    setLoading(false);
  };

  return {
    status,
    loading,
    error,
    isMoR,
    refresh: checkPaymentStatus,
  };
};
```

### 2. MoR Balance Management

```javascript
// hooks/useMoRBalance.js

import { useState, useEffect } from 'react';
import morPaymentService from '../services/morPaymentService';

export const useMoRBalance = () => {
  const [balance, setBalance] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [needsBankDetails, setNeedsBankDetails] = useState(false);

  const fetchBalance = async () => {
    setLoading(true);
    setError(null);
    setNeedsBankDetails(false);
    
    const result = await morPaymentService.getMoRBalance();
    
    if (result.success) {
      setBalance(result.data);
    } else {
      setError(result.error);
      // Check if error is about missing bank details
      if (result.error && result.error.includes('Bank details')) {
        setNeedsBankDetails(true);
      }
    }
    
    setLoading(false);
  };

  useEffect(() => {
    fetchBalance();
  }, []);

  return {
    balance,
    loading,
    error,
    needsBankDetails,
    refresh: fetchBalance,
    formattedBalance: balance 
      ? `${(balance.balance / 100).toFixed(2)} ${balance.currency.toUpperCase()}`
      : null,
  };
};
```

### 3. MoR Transactions Management

```javascript
// hooks/useMoRTransactions.js

import { useState, useEffect } from 'react';
import morPaymentService from '../services/morPaymentService';

export const useMoRTransactions = (limit = 50) => {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchTransactions = async () => {
    setLoading(true);
    setError(null);
    
    const result = await morPaymentService.getMoRTransactions(limit);
    
    if (result.success) {
      // Format transactions for display
      const formatted = result.data.map(txn => ({
        ...txn,
        formattedAmount: `${txn.amount >= 0 ? '+' : ''}${(txn.amount / 100).toFixed(2)} ${txn.currency.toUpperCase()}`,
        isCredit: txn.amount >= 0,
        typeColor: getTransactionTypeColor(txn.type),
        typeIcon: getTransactionTypeIcon(txn.type),
      }));
      setTransactions(formatted);
    } else {
      setError(result.error);
    }
    
    setLoading(false);
  };

  useEffect(() => {
    fetchTransactions();
  }, [limit]);

  return {
    transactions,
    loading,
    error,
    refresh: fetchTransactions,
  };
};

const getTransactionTypeColor = (type) => {
  const colors = {
    'ORDER_PAYMENT': '#4CAF50', // Green
    'PAYOUT': '#F44336',        // Red
    'ADJUSTMENT': '#FF9800',    // Orange
    'REFUND': '#2196F3',        // Blue
  };
  return colors[type] || '#757575';
};

const getTransactionTypeIcon = (type) => {
  const icons = {
    'ORDER_PAYMENT': '💰',
    'PAYOUT': '💸',
    'ADJUSTMENT': '⚙️',
    'REFUND': '↩️',
  };
  return icons[type] || '📄';
};
```

### 4. Bank Details Submission

```javascript
// hooks/useBankDetails.js

import { useState } from 'react';
import morPaymentService from '../services/morPaymentService';

export const useBankDetails = () => {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const validateIBAN = (iban) => {
    if (!iban) return true; // IBAN is optional
    const cleaned = iban.replace(/\s/g, '').toUpperCase();
    return /^[A-Z]{2}\d{2}[A-Z0-9]{4,30}$/.test(cleaned);
  };

  const validateBankDetails = (bankDetails) => {
    if (!bankDetails.bankAccountHolderName || 
        !bankDetails.bankAccountNumber || 
        !bankDetails.bankName) {
      return { valid: false, error: 'Please fill in all required fields' };
    }

    if (bankDetails.bankIban && !validateIBAN(bankDetails.bankIban)) {
      return { valid: false, error: 'Invalid IBAN format' };
    }

    return { valid: true };
  };

  const submitBankDetails = async (bankDetails) => {
    setSubmitting(true);
    setError(null);

    // Validate
    const validation = validateBankDetails(bankDetails);
    if (!validation.valid) {
      setError(validation.error);
      setSubmitting(false);
      return { success: false, error: validation.error };
    }

    const result = await morPaymentService.submitBankDetails(bankDetails);
    
    if (!result.success) {
      setError(result.error);
    }
    
    setSubmitting(false);
    return result;
  };

  return {
    submitBankDetails,
    submitting,
    error,
    validateIBAN,
  };
};
```

### 5. MoR Payouts Management

```javascript
// hooks/useMoRPayouts.js

import { useState, useEffect } from 'react';
import morPaymentService from '../services/morPaymentService';

export const useMoRPayouts = () => {
  const [payouts, setPayouts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [requesting, setRequesting] = useState(false);

  const fetchPayouts = async () => {
    setLoading(true);
    setError(null);
    
    const result = await morPaymentService.getMoRPayouts();
    
    if (result.success) {
      // Format payouts for display
      const formatted = result.data.map(payout => ({
        ...payout,
        formattedAmount: `${(payout.amount / 100).toFixed(2)} ${payout.currency.toUpperCase()}`,
        statusColor: getPayoutStatusColor(payout.status),
        statusIcon: getPayoutStatusIcon(payout.status),
        daysPending: (payout.status === 'PENDING' || payout.status === 'PROCESSING') 
          ? Math.floor((new Date() - new Date(payout.requestedAt)) / (1000 * 60 * 60 * 24))
          : null,
      }));
      setPayouts(formatted);
    } else {
      setError(result.error);
    }
    
    setLoading(false);
  };

  const requestPayout = async (amount, currency = 'EUR', description = 'Manual payout request') => {
    setRequesting(true);
    setError(null);

    // Validate amount
    if (amount <= 0) {
      setError('Amount must be greater than zero');
      setRequesting(false);
      return { success: false, error: 'Invalid amount' };
    }

    const result = await morPaymentService.requestMoRPayout(amount, currency, description);
    
    if (result.success) {
      // Refresh payouts list
      await fetchPayouts();
    } else {
      setError(result.error);
    }
    
    setRequesting(false);
    return result;
  };

  useEffect(() => {
    fetchPayouts();
  }, []);

  return {
    payouts,
    loading,
    error,
    requesting,
    refresh: fetchPayouts,
    requestPayout,
  };
};

const getPayoutStatusColor = (status) => {
  const colors = {
    'COMPLETED': '#4CAF50',
    'PENDING': '#FF9800',
    'PROCESSING': '#2196F3',
    'FAILED': '#F44336',
  };
  return colors[status] || '#757575';
};

const getPayoutStatusIcon = (status) => {
  const icons = {
    'COMPLETED': '✅',
    'PENDING': '⏳',
    'PROCESSING': '🔄',
    'FAILED': '❌',
  };
  return icons[status] || '📄';
};
```

---

## State Management

### Complete MoR Payment Context (React Native)

```javascript
// contexts/MoRPaymentContext.js

import React, { createContext, useContext, useState, useEffect } from 'react';
import morPaymentService from '../services/morPaymentService';

const MoRPaymentContext = createContext();

export const MoRPaymentProvider = ({ children }) => {
  const [paymentStatus, setPaymentStatus] = useState(null);
  const [balance, setBalance] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [payouts, setPayouts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isMoR, setIsMoR] = useState(false);

  // Initialize payment status
  useEffect(() => {
    initializePaymentStatus();
  }, []);

  const initializePaymentStatus = async () => {
    setLoading(true);
    const result = await morPaymentService.getPaymentStatus();
    
    if (result.success) {
      setPaymentStatus(result.data);
      setIsMoR(result.data.payoutModel === 'MOR');
      
      // If MoR, load MoR data
      if (result.data.payoutModel === 'MOR') {
        await loadMoRData();
      }
    } else {
      setError(result.error);
    }
    
    setLoading(false);
  };

  const loadMoRData = async () => {
    try {
      const [balanceResult, transactionsResult, payoutsResult] = await Promise.all([
        morPaymentService.getMoRBalance(),
        morPaymentService.getMoRTransactions(50),
        morPaymentService.getMoRPayouts(),
      ]);

      if (balanceResult.success) {
        setBalance(balanceResult.data);
      }

      if (transactionsResult.success) {
        setTransactions(transactionsResult.data);
      }

      if (payoutsResult.success) {
        setPayouts(payoutsResult.data);
      }
    } catch (err) {
      console.error('Error loading MoR data:', err);
    }
  };

  const refreshAll = async () => {
    await initializePaymentStatus();
  };

  const submitBankDetails = async (bankDetails) => {
    const result = await morPaymentService.submitBankDetails(bankDetails);
    if (result.success) {
      await loadMoRData(); // Refresh balance after submitting bank details
    }
    return result;
  };

  const requestPayout = async (amount, currency, description) => {
    const result = await morPaymentService.requestMoRPayout(amount, currency, description);
    if (result.success) {
      await loadMoRData(); // Refresh balance and payouts
    }
    return result;
  };

  return (
    <MoRPaymentContext.Provider
      value={{
        paymentStatus,
        balance,
        transactions,
        payouts,
        loading,
        error,
        isMoR,
        refreshAll,
        submitBankDetails,
        requestPayout,
        loadMoRData,
      }}
    >
      {children}
    </MoRPaymentContext.Provider>
  );
};

export const useMoRPayment = () => {
  const context = useContext(MoRPaymentContext);
  if (!context) {
    throw new Error('useMoRPayment must be used within MoRPaymentProvider');
  }
  return context;
};
```

---

## UI Components

### 1. Payment Status Screen

```javascript
// screens/PaymentStatusScreen.js

import React from 'react';
import { View, Text, ActivityIndicator, StyleSheet } from 'react-native';
import { usePaymentStatus } from '../hooks/usePaymentStatus';

export const PaymentStatusScreen = ({ navigation }) => {
  const { status, loading, error, isMoR, refresh } = usePaymentStatus();

  if (loading) {
    return (
      <View style={styles.container}>
        <ActivityIndicator size="large" />
        <Text>Loading payment status...</Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.container}>
        <Text style={styles.error}>{error}</Text>
        <Button title="Retry" onPress={refresh} />
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Payment Account Status</Text>
      
      <View style={styles.statusCard}>
        <Text style={styles.label}>Payment Model:</Text>
        <Text style={styles.value}>{status.payoutModel}</Text>
        
        <Text style={styles.label}>Status:</Text>
        <Text style={[styles.value, status.isReady && styles.ready]}>
          {status.isReady ? 'Ready' : 'Not Ready'}
        </Text>
        
        <Text style={styles.message}>{status.message}</Text>
      </View>

      {isMoR ? (
        <Button
          title="Go to MoR Dashboard"
          onPress={() => navigation.navigate('MoRPaymentDashboard')}
        />
      ) : (
        <Button
          title="Go to Stripe Dashboard"
          onPress={() => navigation.navigate('StripeDashboard')}
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    justifyContent: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 20,
  },
  statusCard: {
    backgroundColor: '#f5f5f5',
    padding: 20,
    borderRadius: 10,
    marginBottom: 20,
  },
  label: {
    fontSize: 14,
    color: '#666',
    marginTop: 10,
  },
  value: {
    fontSize: 18,
    fontWeight: 'bold',
    marginTop: 5,
  },
  ready: {
    color: '#4CAF50',
  },
  message: {
    marginTop: 10,
    fontSize: 14,
    color: '#333',
  },
  error: {
    color: '#F44336',
    fontSize: 16,
    marginBottom: 20,
  },
});
```

### 2. MoR Balance Card

```javascript
// components/MoRBalanceCard.js

import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { useMoRBalance } from '../hooks/useMoRBalance';

export const MoRBalanceCard = ({ onRequestPayout }) => {
  const { balance, loading, error, needsBankDetails, formattedBalance, refresh } = useMoRBalance();

  if (loading) {
    return (
      <View style={styles.card}>
        <Text>Loading balance...</Text>
      </View>
    );
  }

  if (needsBankDetails) {
    return (
      <View style={styles.card}>
        <Text style={styles.title}>Bank Details Required</Text>
        <Text style={styles.message}>
          Please submit your bank details to view balance and request payouts.
        </Text>
        <TouchableOpacity
          style={styles.button}
          onPress={() => navigation.navigate('BankDetailsForm')}
        >
          <Text style={styles.buttonText}>Submit Bank Details</Text>
        </TouchableOpacity>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.card}>
        <Text style={styles.error}>{error}</Text>
        <TouchableOpacity style={styles.button} onPress={refresh}>
          <Text style={styles.buttonText}>Retry</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={styles.card}>
      <Text style={styles.label}>Available Balance</Text>
      <Text style={styles.balance}>{formattedBalance}</Text>
      
      {balance && balance.balance > 0 && (
        <TouchableOpacity style={styles.button} onPress={onRequestPayout}>
          <Text style={styles.buttonText}>Request Payout</Text>
        </TouchableOpacity>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  card: {
    backgroundColor: '#fff',
    padding: 20,
    borderRadius: 10,
    margin: 10,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
    elevation: 3,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  label: {
    fontSize: 14,
    color: '#666',
    marginBottom: 5,
  },
  balance: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#4CAF50',
    marginBottom: 15,
  },
  message: {
    fontSize: 14,
    color: '#666',
    marginBottom: 15,
  },
  button: {
    backgroundColor: '#2196F3',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  error: {
    color: '#F44336',
    fontSize: 14,
    marginBottom: 10,
  },
});
```

### 3. Bank Details Form

```javascript
// screens/BankDetailsFormScreen.js

import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { useBankDetails } from '../hooks/useBankDetails';

export const BankDetailsFormScreen = ({ navigation }) => {
  const { submitBankDetails, submitting, error, validateIBAN } = useBankDetails();
  
  const [formData, setFormData] = useState({
    bankAccountHolderName: '',
    bankAccountNumber: '',
    bankName: '',
    bankSwiftCode: '',
    bankIban: '',
  });

  const [validationErrors, setValidationErrors] = useState({});

  const handleSubmit = async () => {
    // Clear previous errors
    setValidationErrors({});

    // Validate required fields
    if (!formData.bankAccountHolderName) {
      setValidationErrors({ ...validationErrors, bankAccountHolderName: 'Required' });
      return;
    }
    if (!formData.bankAccountNumber) {
      setValidationErrors({ ...validationErrors, bankAccountNumber: 'Required' });
      return;
    }
    if (!formData.bankName) {
      setValidationErrors({ ...validationErrors, bankName: 'Required' });
      return;
    }

    // Validate IBAN if provided
    if (formData.bankIban && !validateIBAN(formData.bankIban)) {
      setValidationErrors({ ...validationErrors, bankIban: 'Invalid IBAN format' });
      return;
    }

    const result = await submitBankDetails(formData);
    
    if (result.success) {
      Alert.alert('Success', 'Bank details submitted successfully!', [
        { text: 'OK', onPress: () => navigation.goBack() },
      ]);
    } else {
      Alert.alert('Error', result.error || 'Failed to submit bank details');
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Bank Account Details</Text>
      <Text style={styles.subtitle}>Required for payouts</Text>

      <TextInput
        style={[styles.input, validationErrors.bankAccountHolderName && styles.inputError]}
        placeholder="Account Holder Name *"
        value={formData.bankAccountHolderName}
        onChangeText={(text) => setFormData({ ...formData, bankAccountHolderName: text })}
      />
      {validationErrors.bankAccountHolderName && (
        <Text style={styles.errorText}>{validationErrors.bankAccountHolderName}</Text>
      )}

      <TextInput
        style={[styles.input, validationErrors.bankAccountNumber && styles.inputError]}
        placeholder="Account Number *"
        value={formData.bankAccountNumber}
        onChangeText={(text) => setFormData({ ...formData, bankAccountNumber: text })}
        keyboardType="numeric"
      />
      {validationErrors.bankAccountNumber && (
        <Text style={styles.errorText}>{validationErrors.bankAccountNumber}</Text>
      )}

      <TextInput
        style={[styles.input, validationErrors.bankName && styles.inputError]}
        placeholder="Bank Name *"
        value={formData.bankName}
        onChangeText={(text) => setFormData({ ...formData, bankName: text })}
      />
      {validationErrors.bankName && (
        <Text style={styles.errorText}>{validationErrors.bankName}</Text>
      )}

      <TextInput
        style={styles.input}
        placeholder="SWIFT/BIC Code (optional)"
        value={formData.bankSwiftCode}
        onChangeText={(text) => setFormData({ ...formData, bankSwiftCode: text })}
      />

      <TextInput
        style={[styles.input, validationErrors.bankIban && styles.inputError]}
        placeholder="IBAN (optional)"
        value={formData.bankIban}
        onChangeText={(text) => setFormData({ ...formData, bankIban: text })}
      />
      {validationErrors.bankIban && (
        <Text style={styles.errorText}>{validationErrors.bankIban}</Text>
      )}

      {error && <Text style={styles.errorText}>{error}</Text>}

      <TouchableOpacity
        style={[styles.submitButton, submitting && styles.submitButtonDisabled]}
        onPress={handleSubmit}
        disabled={submitting}
      >
        <Text style={styles.submitButtonText}>
          {submitting ? 'Submitting...' : 'Submit Bank Details'}
        </Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: '#fff',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 5,
  },
  subtitle: {
    fontSize: 14,
    color: '#666',
    marginBottom: 20,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    marginBottom: 10,
    fontSize: 16,
  },
  inputError: {
    borderColor: '#F44336',
  },
  errorText: {
    color: '#F44336',
    fontSize: 12,
    marginBottom: 10,
    marginLeft: 5,
  },
  submitButton: {
    backgroundColor: '#2196F3',
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
    marginTop: 20,
  },
  submitButtonDisabled: {
    backgroundColor: '#ccc',
  },
  submitButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});
```

### 4. Request Payout Modal

```javascript
// components/RequestPayoutModal.js

import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, Modal, StyleSheet, Alert } from 'react-native';
import { useMoRPayouts } from '../hooks/useMoRPayouts';
import { useMoRBalance } from '../hooks/useMoRBalance';

export const RequestPayoutModal = ({ visible, onClose }) => {
  const { requestPayout, requesting } = useMoRPayouts();
  const { balance } = useMoRBalance();
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');

  const handleRequest = async () => {
    const amountInCents = Math.round(parseFloat(amount) * 100);
    
    if (!amount || amountInCents <= 0) {
      Alert.alert('Error', 'Please enter a valid amount');
      return;
    }

    if (balance && amountInCents > balance.balance) {
      Alert.alert('Error', `Insufficient balance. Available: ${(balance.balance / 100).toFixed(2)} ${balance.currency.toUpperCase()}`);
      return;
    }

    Alert.alert(
      'Confirm Payout',
      `Request payout of ${amount} ${balance?.currency.toUpperCase() || 'EUR'}? This will be processed manually within 1-5 business days.`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Confirm',
          onPress: async () => {
            const result = await requestPayout(
              amountInCents,
              balance?.currency || 'EUR',
              description || 'Manual payout request'
            );
            
            if (result.success) {
              Alert.alert('Success', `Payout request created! ID: ${result.payoutId}`);
              setAmount('');
              setDescription('');
              onClose();
            } else {
              Alert.alert('Error', result.error || 'Failed to request payout');
            }
          },
        },
      ]
    );
  };

  return (
    <Modal visible={visible} transparent animationType="slide">
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <Text style={styles.title}>Request Payout</Text>
          
          {balance && (
            <Text style={styles.balanceText}>
              Available: {((balance.balance / 100).toFixed(2))} {balance.currency.toUpperCase()}
            </Text>
          )}

          <TextInput
            style={styles.input}
            placeholder="Amount"
            keyboardType="decimal-pad"
            value={amount}
            onChangeText={setAmount}
          />

          <TextInput
            style={styles.input}
            placeholder="Description (optional)"
            value={description}
            onChangeText={setDescription}
            multiline
          />

          <View style={styles.buttonRow}>
            <TouchableOpacity style={styles.cancelButton} onPress={onClose}>
              <Text style={styles.cancelButtonText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.submitButton, requesting && styles.submitButtonDisabled]}
              onPress={handleRequest}
              disabled={requesting}
            >
              <Text style={styles.submitButtonText}>
                {requesting ? 'Requesting...' : 'Request Payout'}
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'flex-end',
  },
  modalContent: {
    backgroundColor: '#fff',
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 20,
    maxHeight: '80%',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  balanceText: {
    fontSize: 16,
    color: '#666',
    marginBottom: 20,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 8,
    padding: 12,
    marginBottom: 15,
    fontSize: 16,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 20,
  },
  cancelButton: {
    flex: 1,
    padding: 15,
    borderRadius: 8,
    backgroundColor: '#f5f5f5',
    alignItems: 'center',
    marginRight: 10,
  },
  cancelButtonText: {
    color: '#333',
    fontSize: 16,
    fontWeight: 'bold',
  },
  submitButton: {
    flex: 1,
    padding: 15,
    borderRadius: 8,
    backgroundColor: '#2196F3',
    alignItems: 'center',
  },
  submitButtonDisabled: {
    backgroundColor: '#ccc',
  },
  submitButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});
```

---

## Complete Integration Flow

### App Navigation Setup

```javascript
// App.js or navigation setup

import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { MoRPaymentProvider } from './contexts/MoRPaymentContext';

// Screens
import PaymentStatusScreen from './screens/PaymentStatusScreen';
import MoRPaymentDashboard from './screens/MoRPaymentDashboard';
import BankDetailsFormScreen from './screens/BankDetailsFormScreen';

const Stack = createStackNavigator();

export default function App() {
  return (
    <MoRPaymentProvider>
      <NavigationContainer>
        <Stack.Navigator>
          <Stack.Screen name="PaymentStatus" component={PaymentStatusScreen} />
          <Stack.Screen name="MoRPaymentDashboard" component={MoRPaymentDashboard} />
          <Stack.Screen name="BankDetailsForm" component={BankDetailsFormScreen} />
        </Stack.Navigator>
      </NavigationContainer>
    </MoRPaymentProvider>
  );
}
```

### Complete MoR Payment Dashboard

```javascript
// screens/MoRPaymentDashboard.js

import React, { useState } from 'react';
import { View, ScrollView, StyleSheet, RefreshControl } from 'react-native';
import { useMoRPayment } from '../contexts/MoRPaymentContext';
import { MoRBalanceCard } from '../components/MoRBalanceCard';
import { RequestPayoutModal } from '../components/RequestPayoutModal';
import { TransactionList } from '../components/TransactionList';
import { PayoutList } from '../components/PayoutList';

export const MoRPaymentDashboard = ({ navigation }) => {
  const {
    balance,
    transactions,
    payouts,
    loading,
    refreshAll,
  } = useMoRPayment();

  const [payoutModalVisible, setPayoutModalVisible] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  const onRefresh = async () => {
    setRefreshing(true);
    await refreshAll();
    setRefreshing(false);
  };

  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
      }
    >
      <MoRBalanceCard
        onRequestPayout={() => setPayoutModalVisible(true)}
      />

      <TransactionList transactions={transactions} />

      <PayoutList payouts={payouts} />

      <RequestPayoutModal
        visible={payoutModalVisible}
        onClose={() => setPayoutModalVisible(false)}
      />
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
});
```

---

## Testing Guide

### Test Checklist

1. **Payment Status Check**
   - [ ] Verify payment status returns `payoutModel: "MOR"`
   - [ ] Verify `isReady` status is correct
   - [ ] Test with expired token (should redirect to login)

2. **Balance Management**
   - [ ] Display balance correctly (cents to currency conversion)
   - [ ] Handle missing bank details error
   - [ ] Refresh balance after payout request

3. **Bank Details Submission**
   - [ ] Validate required fields
   - [ ] Validate IBAN format
   - [ ] Submit successfully
   - [ ] Handle errors gracefully

4. **Transaction History**
   - [ ] Display all transaction types
   - [ ] Format amounts correctly (positive/negative)
   - [ ] Show transaction icons/colors
   - [ ] Handle empty state

5. **Payout Management**
   - [ ] Display payout history
   - [ ] Show payout status colors
   - [ ] Request payout with valid amount
   - [ ] Handle insufficient balance error
   - [ ] Handle missing bank details error

6. **Error Handling**
   - [ ] Network errors
   - [ ] 401 Unauthorized (token expired)
   - [ ] 400 Bad Request (validation errors)
   - [ ] 500 Internal Server Error

---

## Additional Resources

- See `FRONTEND_NON_STRIPE_ENDPOINTS_GUIDE.md` for detailed endpoint documentation
- See `FRONTEND_STRIPE_ENDPOINTS_GUIDE.md` for Stripe Connect endpoints (for comparison)
- API Base URL: Configure in `config/api.js`
- Authentication: Ensure JWT token is stored securely

---

## Support

For issues:
1. Check error messages in API responses
2. Verify authentication token is valid
3. Ensure vendor has `payoutModel === "MOR"`
4. Check network connectivity
5. Review console logs for detailed errors

