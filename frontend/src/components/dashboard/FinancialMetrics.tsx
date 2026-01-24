import {
  ArrowDownIcon,
  ArrowUpIcon,
} from "../../icons";
import Badge from "../ui/badge/Badge";

interface MetricCardProps {
  title: string;
  value: string | number;
  change?: number;
  icon: React.ReactNode;
  trend?: 'up' | 'down' | 'neutral';
}

const MetricCard = ({ title, value, change, icon, trend = 'neutral' }: MetricCardProps) => {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
      <div className="flex items-center justify-center w-12 h-12 bg-gray-100 rounded-xl dark:bg-gray-800">
        {icon}
      </div>

      <div className="flex items-end justify-between mt-5">
        <div>
          <span className="text-sm text-gray-500 dark:text-gray-400">
            {title}
          </span>
          <h4 className="mt-2 font-bold text-gray-800 text-title-sm dark:text-white/90">
            {value}
          </h4>
        </div>
        {change !== undefined && (
          <Badge color={trend === 'up' ? 'success' : trend === 'down' ? 'error' : 'light'}>
            {trend === 'up' && <ArrowUpIcon />}
            {trend === 'down' && <ArrowDownIcon />}
            {Math.abs(change).toFixed(2)}%
          </Badge>
        )}
      </div>
    </div>
  );
};

interface FinancialMetricsProps {
  totalTransactions?: number;
  totalAmount?: number;
  pendingTransactions?: number;
  completedTransactions?: number;
  fraudCount?: number;
  highRiskCount?: number;
  totalUsers?: number;
  unreviewedCount?: number;
}

export default function FinancialMetrics({
  totalTransactions = 0,
  totalAmount = 0,
  pendingTransactions = 0,
  completedTransactions = 0,
  fraudCount = 0,
  highRiskCount = 0,
  totalUsers = 0,
  unreviewedCount = 0,
}: FinancialMetricsProps) {
  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(amount);
  };

  const formatNumber = (num: number) => {
    return new Intl.NumberFormat('en-US').format(num);
  };

  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 md:gap-6">
      {/* Total Transactions */}
      <MetricCard
        title="Total Transactions"
        value={formatNumber(totalTransactions)}
        icon={
          <svg className="text-gray-800 size-6 dark:text-white/90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
        }
      />

      {/* Total Amount */}
      <MetricCard
        title="Total Amount"
        value={formatCurrency(totalAmount)}
        icon={
          <svg className="text-gray-800 size-6 dark:text-white/90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        }
      />

      {/* Pending Transactions */}
      <MetricCard
        title="Pending"
        value={formatNumber(pendingTransactions)}
        icon={
          <svg className="text-gray-800 size-6 dark:text-white/90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        }
      />

      {/* Completed Transactions */}
      <MetricCard
        title="Completed"
        value={formatNumber(completedTransactions)}
        trend="up"
        icon={
          <svg className="text-gray-800 size-6 dark:text-white/90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        }
      />

      {/* Fraud Detected */}
      {fraudCount > 0 && (
        <MetricCard
          title="Fraud Detected"
          value={formatNumber(fraudCount)}
          trend="down"
          icon={
            <svg className="text-gray-800 size-6 dark:text-white/90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          }
        />
      )}

      {/* High Risk Count */}
      {highRiskCount > 0 && (
        <MetricCard
          title="High Risk"
          value={formatNumber(highRiskCount)}
          trend="down"
          icon={
            <svg className="text-gray-800 size-6 dark:text-white/90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          }
        />
      )}

      {/* Total Users */}
      {totalUsers > 0 && (
        <MetricCard
          title="Total Users"
          value={formatNumber(totalUsers)}
          icon={
            <svg className="text-gray-800 size-6 dark:text-white/90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
            </svg>
          }
        />
      )}

      {/* Unreviewed */}
      {unreviewedCount > 0 && (
        <MetricCard
          title="Unreviewed"
          value={formatNumber(unreviewedCount)}
          icon={
            <svg className="text-gray-800 size-6 dark:text-white/90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
            </svg>
          }
        />
      )}
    </div>
  );
}
