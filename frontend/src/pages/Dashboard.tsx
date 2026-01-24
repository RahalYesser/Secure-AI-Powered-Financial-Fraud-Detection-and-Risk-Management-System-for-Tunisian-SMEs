import { useState, useEffect } from 'react';
import { usePermission } from '../hooks/usePermission';
import { transactionService } from '../services/transactionService';
import { fraudService } from '../services/fraudService';
import { creditRiskService } from '../services/creditRiskService';
import { userService } from '../services/userService';
import { TransactionStatistics } from '../types/transaction.types';
import { FraudStatistics } from '../types/fraud.types';
import { RiskStatistics } from '../types/credit-risk.types';
import { UserStatistics } from '../types/user.types';
import { UserRole } from '../types/user.types';
import FinancialMetrics from '../components/dashboard/FinancialMetrics';
import TransactionTrendsChart from '../components/dashboard/TransactionTrendsChart';
import RiskDistributionChart from '../components/dashboard/RiskDistributionChart';
import FraudPatternsChart from '../components/dashboard/FraudPatternsChart';

const Dashboard = () => {
  const { user, userRole } = usePermission();
  const [transactionStats, setTransactionStats] = useState<TransactionStatistics | null>(null);
  const [transactionTrends, setTransactionTrends] = useState<Array<{date: string, count: number, amount: number}>>([]);
  const [fraudStats, setFraudStats] = useState<FraudStatistics | null>(null);
  const [fraudPatterns, setFraudPatterns] = useState<Array<{type: string, count: number}>>([]);
  const [riskStats, setRiskStats] = useState<RiskStatistics | null>(null);
  const [userStats, setUserStats] = useState<UserStatistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadStatistics();
  }, [userRole]);

  const loadStatistics = async () => {
    setLoading(true);
    setError(null);
    
    try {
      const promises: Promise<any>[] = [];

      // Load transaction statistics
      if (userRole === UserRole.ADMIN || userRole === UserRole.FINANCIAL_ANALYST) {
        promises.push(
          transactionService.getTransactionStatistics()
            .then(setTransactionStats)
            .catch(() => null)
        );
        // Load transaction trends
        promises.push(
          transactionService.getTransactionTrends(30)
            .then(setTransactionTrends)
            .catch(() => [])
        );
      } else if (userRole === UserRole.SME_USER) {
        // SME users see their own statistics
        promises.push(
          transactionService.getMyStatistics()
            .then(setTransactionStats)
            .catch(() => null)
        );
      }

      // Load fraud statistics (Admin, Analyst, Auditor)
      if (userRole === UserRole.ADMIN || userRole === UserRole.FINANCIAL_ANALYST || userRole === UserRole.AUDITOR) {
        promises.push(
          fraudService.getFraudStatistics()
            .then((stats) => {
              setFraudStats(stats);
              // Convert patternsByType to array for chart
              if (stats.patternsByType) {
                const patternsArray = Object.entries(stats.patternsByType).map(([type, count]) => ({
                  type,
                  count: count as number
                }));
                setFraudPatterns(patternsArray);
              }
            })
            .catch(() => null)
        );
      }

      // Load credit risk statistics (Admin, Analyst, Auditor)
      if (userRole === UserRole.ADMIN || userRole === UserRole.FINANCIAL_ANALYST || userRole === UserRole.AUDITOR) {
        promises.push(
          creditRiskService.getRiskStatistics()
            .then(setRiskStats)
            .catch(() => null)
        );
      }

      // Load user statistics (Admin, Auditor)
      if (userRole === UserRole.ADMIN || userRole === UserRole.AUDITOR) {
        promises.push(
          userService.getUserStatistics()
            .then(setUserStats)
            .catch(() => null)
        );
      }

      await Promise.all(promises);
    } catch (err: any) {
      setError('Failed to load statistics');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="p-6">
        <div className="flex items-center justify-center h-64">
          <div className="flex flex-col items-center gap-3">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900 dark:border-white"></div>
            <p className="text-gray-500 dark:text-gray-400">Loading dashboard...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <div className="rounded-2xl border border-red-200 bg-red-50 p-6 dark:border-red-800 dark:bg-red-500/10">
          <div className="flex items-center gap-3">
            <svg className="h-6 w-6 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <p className="text-red-800 dark:text-red-200">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="p-4 md:p-6">
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white md:text-3xl">
          Financial Dashboard
        </h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Welcome back, {user?.firstName || user?.email || 'User'}! Here's what's happening with your financial platform.
        </p>
      </div>

      {/* Metrics Cards */}
      <div className="mb-6">
        <FinancialMetrics
          totalTransactions={transactionStats?.totalTransactions || 0}
          totalAmount={transactionStats?.totalAmount || 0}
          pendingTransactions={transactionStats?.pendingTransactions || 0}
          completedTransactions={transactionStats?.completedTransactions || 0}
          fraudCount={fraudStats?.totalPatterns || 0}
          highRiskCount={
            riskStats
              ? (riskStats.HIGH || 0) + (riskStats.CRITICAL || 0)
              : 0
          }
          totalUsers={userStats?.totalUsers || 0}
          unreviewedCount={fraudStats?.unresolvedPatterns || 0}
        />
      </div>

      {/* Charts Grid */}
      <div className="grid grid-cols-1 gap-4 md:gap-6 xl:grid-cols-2">
        {/* Transaction Trends - Full Width */}
        {transactionStats && (
          <div className="xl:col-span-2">
            <TransactionTrendsChart data={transactionTrends} />
          </div>
        )}

        {/* Risk Distribution */}
        {riskStats && (
          <RiskDistributionChart
            low={riskStats.LOW || 0}
            medium={riskStats.MEDIUM || 0}
            high={riskStats.HIGH || 0}
            critical={riskStats.CRITICAL || 0}
          />
        )}

        {/* Fraud Patterns */}
        {fraudStats && (
          <FraudPatternsChart patterns={fraudPatterns} />
        )}
      </div>

      {/* Additional Statistics Sections */}
      <div className="mt-6 grid grid-cols-1 gap-4 md:gap-6 lg:grid-cols-2">
        {/* Transaction Details */}
        {transactionStats && (
          <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
            <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
              Transaction Breakdown
            </h3>
            
            {/* By Type */}
            {([
              ['PAYMENT', transactionStats.paymentCount],
              ['TRANSFER', transactionStats.transferCount],
              ['WITHDRAWAL', transactionStats.withdrawalCount],
              ['DEPOSIT', transactionStats.depositCount],
            ] as Array<[string, number]>).filter(([, count]) => count > 0).length > 0 && (
              <div className="mb-4">
                <p className="mb-3 text-sm font-medium text-gray-700 dark:text-gray-300">By Type</p>
                <div className="grid grid-cols-2 gap-3">
                  {([
                    ['PAYMENT', transactionStats.paymentCount],
                    ['TRANSFER', transactionStats.transferCount],
                    ['WITHDRAWAL', transactionStats.withdrawalCount],
                    ['DEPOSIT', transactionStats.depositCount],
                  ] as Array<[string, number]>).filter(([, count]) => count > 0).map(([type, count]) => (
                    <div key={type} className="rounded-lg bg-gray-50 p-3 dark:bg-gray-800">
                      <p className="text-xs text-gray-500 dark:text-gray-400">{type}</p>
                      <p className="mt-1 text-lg font-semibold text-gray-900 dark:text-white">
                        {count.toLocaleString()}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* By Status */}
            {([
              ['PENDING', transactionStats.pendingTransactions],
              ['COMPLETED', transactionStats.completedTransactions],
              ['FAILED', transactionStats.failedTransactions],
              ['FRAUD_DETECTED', transactionStats.fraudDetectedTransactions],
              ['CANCELLED', transactionStats.cancelledTransactions],
            ] as Array<[string, number]>).filter(([, count]) => count > 0).length > 0 && (
              <div>
                <p className="mb-3 text-sm font-medium text-gray-700 dark:text-gray-300">By Status</p>
                <div className="grid grid-cols-2 gap-3">
                  {([
                    ['PENDING', transactionStats.pendingTransactions],
                    ['COMPLETED', transactionStats.completedTransactions],
                    ['FAILED', transactionStats.failedTransactions],
                    ['FRAUD_DETECTED', transactionStats.fraudDetectedTransactions],
                    ['CANCELLED', transactionStats.cancelledTransactions],
                  ] as Array<[string, number]>).filter(([, count]) => count > 0).map(([status, count]) => (
                    <div key={status} className="rounded-lg bg-gray-50 p-3 dark:bg-gray-800">
                      <p className="text-xs text-gray-500 dark:text-gray-400">{status}</p>
                      <p className="mt-1 text-lg font-semibold text-gray-900 dark:text-white">
                        {count.toLocaleString()}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Fraud Details */}
        {fraudStats && (
          <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
            <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
              Fraud Statistics
            </h3>
            
            <div className="space-y-4">
              <div className="flex items-center justify-between rounded-lg bg-gray-50 p-3 dark:bg-gray-800">
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Resolved Patterns</p>
                  <p className="mt-1 text-xl font-semibold text-green-600 dark:text-green-400">
                    {fraudStats.resolvedPatterns?.toLocaleString() || 0}
                  </p>
                </div>
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-green-100 dark:bg-green-500/10">
                  <svg className="h-6 w-6 text-green-600 dark:text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                </div>
              </div>

              <div className="flex items-center justify-between rounded-lg bg-gray-50 p-3 dark:bg-gray-800">
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Unresolved Patterns</p>
                  <p className="mt-1 text-xl font-semibold text-red-600 dark:text-red-400">
                    {fraudStats.unresolvedPatterns?.toLocaleString() || 0}
                  </p>
                </div>
                <div className="flex h-12 w-12 items-center justify-center rounded-full bg-red-100 dark:bg-red-500/10">
                  <svg className="h-6 w-6 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* User Statistics */}
        {userStats && (
          <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6 lg:col-span-2">
            <h3 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
              User Statistics
            </h3>
            
            {(() => {
              const activeUsers = userStats.totalUsers - userStats.lockedUsers;
              const activePercent = userStats.totalUsers > 0 ? (activeUsers / userStats.totalUsers) * 100 : 0;
              const smeUsers = userStats.smeUserCount;

              return (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <div className="rounded-lg bg-gradient-to-br from-blue-50 to-blue-100 p-4 dark:from-blue-500/10 dark:to-blue-600/10">
                <p className="text-sm text-blue-600 dark:text-blue-400">Active Users</p>
                <p className="mt-2 text-2xl font-bold text-blue-700 dark:text-blue-300">
						{activeUsers.toLocaleString()}
                </p>
                <p className="mt-1 text-xs text-blue-600 dark:text-blue-400">
						{activePercent.toFixed(1)}% of total
                </p>
              </div>

              <div className="rounded-lg bg-gradient-to-br from-green-50 to-green-100 p-4 dark:from-green-500/10 dark:to-green-600/10">
                <p className="text-sm text-green-600 dark:text-green-400">New This Month</p>
                <p className="mt-2 text-2xl font-bold text-green-700 dark:text-green-300">
						0
                </p>
                <p className="mt-1 text-xs text-green-600 dark:text-green-400">
						Recent signups
                </p>
              </div>

              <div className="rounded-lg bg-gradient-to-br from-purple-50 to-purple-100 p-4 dark:from-purple-500/10 dark:to-purple-600/10">
                <p className="text-sm text-purple-600 dark:text-purple-400">SME Users</p>
                <p className="mt-2 text-2xl font-bold text-purple-700 dark:text-purple-300">
						{smeUsers.toLocaleString()}
                </p>
                <p className="mt-1 text-xs text-purple-600 dark:text-purple-400">
                  Business accounts
                </p>
              </div>
            </div>
				);
			})()}
          </div>
        )}
      </div>

      {/* Role-specific messages */}
      {userRole === UserRole.SME_USER && (
        <div className="mt-6 rounded-2xl border border-blue-200 bg-gradient-to-br from-blue-50 to-blue-100 p-6 dark:border-blue-700 dark:from-blue-500/10 dark:to-blue-600/10">
          <div className="flex items-start gap-4">
            <div className="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-blue-500">
              <svg className="h-6 w-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <h3 className="text-lg font-semibold text-blue-900 dark:text-blue-100">
                Welcome to Your Dashboard
              </h3>
              <p className="mt-1 text-sm text-blue-700 dark:text-blue-300">
                View your transaction history, check your credit risk assessment, and monitor your financial activity.
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
