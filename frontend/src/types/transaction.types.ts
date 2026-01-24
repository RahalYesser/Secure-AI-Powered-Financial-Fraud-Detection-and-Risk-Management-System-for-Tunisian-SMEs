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
  FLAGGED = 'FLAGGED'
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
  totalAmount: number;
  flaggedTransactions: number;
  transactionsByType: Record<TransactionType, number>;
  transactionsByStatus: Record<TransactionStatus, number>;
}
