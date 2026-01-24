import { useState, useEffect, ChangeEvent, FormEvent } from 'react';
import { transactionService } from '../services/transactionService';
import { 
  TransactionResponse, 
  TransactionRequest, 
  TransactionType, 
  TransactionStatus 
} from '../types/transaction.types';
import { PaginatedResponse } from '../types/common.types';
import { Modal } from '../components/ui/modal';
import Button from '../components/ui/button/Button';
import Badge from '../components/ui/badge/Badge';
import Input from '../components/form/input/InputField';
import Form from '../components/form/Form';
import { Table, TableHeader, TableBody, TableRow, TableCell } from '../components/ui/table';
import { usePermission } from '../hooks/usePermission';

const TransactionManagement = () => {
  const { hasPermission, user } = usePermission();
  const [transactions, setTransactions] = useState<PaginatedResponse<TransactionResponse> | null>(null);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStatus, setSelectedStatus] = useState<TransactionStatus | 'ALL'>('ALL');
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Check permissions
  const canViewAll = hasPermission('TRANSACTION_VIEW_ALL');
  const canCreate = hasPermission('TRANSACTION_CREATE');
  const canViewOwn = hasPermission('TRANSACTION_VIEW_OWN');

  useEffect(() => {
    loadTransactions();
  }, [selectedStatus]);

  const loadTransactions = async (page = 0, size = 10) => {
    setLoading(true);
    setError(null);
    try {
      let response;
      
      if (canViewAll) {
        // Admin, Auditor, Analyst can view all
        if (searchQuery) {
          response = await transactionService.searchTransactions(searchQuery, { page, size });
        } else if (selectedStatus !== 'ALL') {
          response = await transactionService.getTransactionsByStatus(selectedStatus as TransactionStatus, { page, size });
        } else {
          response = await transactionService.getAllTransactions({ page, size });
        }
      } else if (canViewOwn && user) {
        // SME_USER can view own transactions
        response = await transactionService.getTransactionsByUserId(user.id, { page, size });
      } else {
        setError('You don\'t have permission to view transactions');
        setLoading(false);
        return;
      }
      
      setTransactions(response);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load transactions');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    loadTransactions();
  };

  const getStatusBadgeColor = (status: TransactionStatus) => {
    switch (status) {
      case TransactionStatus.COMPLETED:
        return 'success';
      case TransactionStatus.PENDING:
        return 'warning';
      case TransactionStatus.FAILED:
        return 'error';
      case TransactionStatus.FLAGGED:
        return 'error';
      default:
        return 'light';
    }
  };

  const getTypeBadgeColor = (type: TransactionType) => {
    switch (type) {
      case TransactionType.DEPOSIT:
        return 'success';
      case TransactionType.WITHDRAWAL:
        return 'warning';
      case TransactionType.TRANSFER:
        return 'info';
      case TransactionType.PAYMENT:
        return 'primary';
      default:
        return 'light';
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  if (!canViewAll && !canViewOwn) {
    return (
      <div className="p-6">
        <div className="rounded-lg bg-error-50 p-4 text-error-700 dark:bg-error-500/10 dark:text-error-400">
          You don't have permission to view transactions.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900 dark:text-white">
            Transaction Management
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            {canViewAll ? 'View and manage all transactions' : 'View your transactions'}
          </p>
        </div>
        {canCreate && (
          <Button onClick={() => setIsCreateModalOpen(true)}>
            Create Transaction
          </Button>
        )}
      </div>

      {/* Filters */}
      <div className="mb-6 flex gap-4">
        {canViewAll && (
          <>
            <div className="flex-1">
              <Input
                type="text"
                placeholder="Search transactions..."
                value={searchQuery}
                onChange={(e: ChangeEvent<HTMLInputElement>) => setSearchQuery(e.target.value)}
              />
            </div>
            <div className="w-48">
              <select
                className="h-11 w-full appearance-none rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm"
                value={selectedStatus}
                onChange={(e: ChangeEvent<HTMLSelectElement>) => setSelectedStatus(e.target.value as TransactionStatus | 'ALL')}
              >
                <option value="ALL">All Status</option>
                <option value={TransactionStatus.COMPLETED}>Completed</option>
                <option value={TransactionStatus.PENDING}>Pending</option>
                <option value={TransactionStatus.FAILED}>Failed</option>
                <option value={TransactionStatus.FLAGGED}>Flagged</option>
              </select>
            </div>
            <Button onClick={handleSearch}>Search</Button>
          </>
        )}
      </div>

      {/* Error Message */}
      {error && (
        <div className="mb-4 rounded-lg bg-error-50 p-4 text-error-700 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}

      {/* Transactions Table */}
      <div className="rounded-lg border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-900">
        <Table>
          <TableHeader>
            <TableRow>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Reference
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Type
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Amount
              </th>
              {canViewAll && (
                <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  User
                </th>
              )}
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Fraud Score
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Date
              </th>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={canViewAll ? 7 : 6} className="text-center py-8">
                  Loading...
                </TableCell>
              </TableRow>
            ) : transactions?.content.length === 0 ? (
              <TableRow>
                <TableCell colSpan={canViewAll ? 7 : 6} className="text-center py-8 text-gray-500">
                  No transactions found
                </TableCell>
              </TableRow>
            ) : (
              transactions?.content.map((transaction: TransactionResponse) => (
                <TableRow key={transaction.id}>
                  <TableCell className="px-6 py-4">
                    <div className="text-sm">
                      <div className="font-medium text-gray-900 dark:text-white">
                        #{transaction.referenceNumber}
                      </div>
                      {transaction.description && (
                        <div className="text-gray-500 dark:text-gray-400">
                          {transaction.description}
                        </div>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="px-6 py-4">
                    <Badge color={getTypeBadgeColor(transaction.type)}>
                      {transaction.type}
                    </Badge>
                  </TableCell>
                  <TableCell className="px-6 py-4 font-medium text-gray-900 dark:text-white">
                    {formatCurrency(transaction.amount)}
                  </TableCell>
                  {canViewAll && (
                    <TableCell className="px-6 py-4">
                      <div className="text-sm text-gray-500 dark:text-gray-400">
                        {transaction.userEmail}
                      </div>
                    </TableCell>
                  )}
                  <TableCell className="px-6 py-4">
                    <Badge color={getStatusBadgeColor(transaction.status)}>
                      {transaction.status}
                    </Badge>
                  </TableCell>
                  <TableCell className="px-6 py-4">
                    {transaction.fraudScore !== null ? (
                      <div className="flex items-center gap-2">
                        <span className={`text-sm font-medium ${
                          transaction.fraudScore > 0.7 ? 'text-error-600 dark:text-error-400' :
                          transaction.fraudScore > 0.4 ? 'text-warning-600 dark:text-warning-400' :
                          'text-success-600 dark:text-success-400'
                        }`}>
                          {(transaction.fraudScore * 100).toFixed(1)}%
                        </span>
                      </div>
                    ) : (
                      <span className="text-sm text-gray-400">-</span>
                    )}
                  </TableCell>
                  <TableCell className="px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
                    {formatDate(transaction.createdAt)}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      {transactions && transactions.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between">
          <div className="text-sm text-gray-500 dark:text-gray-400">
            Showing {transactions.content.length} of {transactions.totalElements} transactions
          </div>
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="outline"
              disabled={transactions.page === 0}
              onClick={() => loadTransactions(transactions.page - 1)}
            >
              Previous
            </Button>
            <Button
              size="sm"
              variant="outline"
              disabled={transactions.page >= transactions.totalPages - 1}
              onClick={() => loadTransactions(transactions.page + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      {/* Create Transaction Modal */}
      <CreateTransactionModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={() => {
          setIsCreateModalOpen(false);
          loadTransactions();
        }}
      />
    </div>
  );
};

// Create Transaction Modal Component
const CreateTransactionModal = ({ isOpen, onClose, onSuccess }: any) => {
  const [formData, setFormData] = useState<TransactionRequest>({
    type: TransactionType.PAYMENT,
    amount: 0,
    description: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      await transactionService.createTransaction(formData);
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create transaction');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} className="max-w-md p-6">
      <h2 className="mb-4 text-xl font-semibold text-gray-900 dark:text-white">
        Create Transaction
      </h2>
      
      {error && (
        <div className="mb-4 rounded-lg bg-error-50 p-3 text-sm text-error-700 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}

      <Form onSubmit={handleSubmit}>
        <div className="space-y-4">
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Transaction Type
            </label>
            <select
              className="h-11 w-full appearance-none rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-sm"
              value={formData.type}
              onChange={(e: ChangeEvent<HTMLSelectElement>) => setFormData({ ...formData, type: e.target.value as TransactionType })}
            >
              <option value={TransactionType.DEPOSIT}>Deposit</option>
              <option value={TransactionType.WITHDRAWAL}>Withdrawal</option>
              <option value={TransactionType.TRANSFER}>Transfer</option>
              <option value={TransactionType.PAYMENT}>Payment</option>
            </select>
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Amount
            </label>
            <Input
              type="number"
              step={0.01}
              min="0.01"
              value={formData.amount}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, amount: parseFloat(e.target.value) })}
            />
          </div>

          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Description (Optional)
            </label>
            <Input
              type="text"
              value={formData.description}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setFormData({ ...formData, description: e.target.value })}
            />
          </div>

          <div className="flex gap-3 pt-4">
            <Button disabled={loading} className="flex-1">
              {loading ? 'Creating...' : 'Create Transaction'}
            </Button>
            <Button variant="outline" onClick={onClose} className="flex-1">
              Cancel
            </Button>
          </div>
        </div>
      </Form>
    </Modal>
  );
};

export default TransactionManagement;
