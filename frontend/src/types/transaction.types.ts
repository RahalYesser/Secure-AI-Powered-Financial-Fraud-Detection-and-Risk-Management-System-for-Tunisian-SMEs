export enum TransactionType {
  DEPOSIT = 'DEPOSIT',
  WITHDRAWAL = 'WITHDRAWAL',
  TRANSFER = 'TRANSFER',
  PAYMENT = 'PAYMENT'
}

export enum TransactionStatus {
  PENDING = 'PENDING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  FRAUD_DETECTED = 'FRAUD_DETECTED',
  CANCELLED = 'CANCELLED'
}

export interface TransactionRequest {
  type: TransactionType;
  amount: number;
  description?: string;
}

export interface TransactionResponse {
  id: number;
  type: TransactionType;
  status: TransactionStatus;
  amount: number;
  userId: string;
  userEmail: string;
  description: string | null;
  fraudScore: number | null;
  referenceNumber: string;
  receipt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionStatistics {
  totalTransactions: number;
  pendingTransactions: number;
  completedTransactions: number;
  failedTransactions: number;
  fraudDetectedTransactions: number;
  cancelledTransactions: number;
  totalAmount: number;
  averageAmount: number;
  paymentCount: number;
  transferCount: number;
  withdrawalCount: number;
  depositCount: number;
}
