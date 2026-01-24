import { useState, useEffect, ChangeEvent } from 'react';
import { fraudService } from '../services/fraudService';
import { 
  FraudPatternResponse, 
  FraudDetectionResult 
} from '../types/fraud.types';
import { PaginatedResponse } from '../types/common.types';
import { Modal } from '../components/ui/modal';
import Button from '../components/ui/button/Button';
import Badge from '../components/ui/badge/Badge';
import Input from '../components/form/input/InputField';
import Form from '../components/form/Form';
import { Table, TableHeader, TableBody, TableRow, TableCell } from '../components/ui/table';
import { usePermission } from '../hooks/usePermission';

const FraudDetection = () => {
  const { hasPermission } = usePermission();
  const [patterns, setPatterns] = useState<PaginatedResponse<FraudPatternResponse> | null>(null);
  const [loading, setLoading] = useState(false);
  const [isDetectModalOpen, setIsDetectModalOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Check permissions
  const canAccessFraud = hasPermission('FRAUD_DETECT');
  const canResolve = hasPermission('FRAUD_RESOLVE');

  useEffect(() => {
    if (canAccessFraud) {
      loadPatterns();
    }
  }, []);

  const loadPatterns = async (page = 0, size = 10) => {
    setLoading(true);
    setError(null);
    try {
      const response = await fraudService.getAllPatterns({ page, size });
      setPatterns(response);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load fraud patterns');
    } finally {
      setLoading(false);
    }
  };

  const handleResolve = async (patternId: number) => {
    if (!confirm('Mark this fraud pattern as resolved?')) return;
    try {
      await fraudService.resolvePattern(patternId);
      loadPatterns();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to resolve pattern');
    }
  };

  const getSeverityBadgeColor = (severity: string) => {
    switch (severity.toUpperCase()) {
      case 'CRITICAL':
      case 'HIGH':
        return 'error';
      case 'MEDIUM':
        return 'warning';
      case 'LOW':
        return 'info';
      default:
        return 'light';
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  if (!canAccessFraud) {
    return (
      <div className="p-6">
        <div className="rounded-lg bg-error-50 p-4 text-error-700 dark:bg-error-500/10 dark:text-error-400">
          You don't have permission to access fraud detection features.
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
            Fraud Detection
          </h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            AI-powered fraud detection and pattern analysis
          </p>
        </div>
        <Button onClick={() => setIsDetectModalOpen(true)}>
          Detect Fraud
        </Button>
      </div>

      {/* Error Message */}
      {error && (
        <div className="mb-4 rounded-lg bg-error-50 p-4 text-error-700 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}

      {/* Statistics Cards */}
      <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-900">
          <div className="text-sm text-gray-500 dark:text-gray-400">Total Patterns</div>
          <div className="mt-1 text-2xl font-semibold text-gray-900 dark:text-white">
            {patterns?.totalElements || 0}
          </div>
        </div>
        <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-900">
          <div className="text-sm text-gray-500 dark:text-gray-400">Unresolved</div>
          <div className="mt-1 text-2xl font-semibold text-error-600 dark:text-error-400">
            {patterns?.content.filter(p => !p.resolved).length || 0}
          </div>
        </div>
        <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-900">
          <div className="text-sm text-gray-500 dark:text-gray-400">Resolved</div>
          <div className="mt-1 text-2xl font-semibold text-success-600 dark:text-success-400">
            {patterns?.content.filter(p => p.resolved).length || 0}
          </div>
        </div>
        <div className="rounded-lg border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-gray-900">
          <div className="text-sm text-gray-500 dark:text-gray-400">Detection Rate</div>
          <div className="mt-1 text-2xl font-semibold text-brand-600 dark:text-brand-400">
            {patterns?.totalElements ? 
              ((patterns.content.filter(p => p.resolved).length / patterns.totalElements) * 100).toFixed(1) 
              : '0'}%
          </div>
        </div>
      </div>

      {/* Fraud Patterns Table */}
      <div className="rounded-lg border border-gray-200 bg-white dark:border-gray-700 dark:bg-gray-900">
        <Table>
          <TableHeader>
            <TableRow>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Pattern ID
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Transaction
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Type
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Severity
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Description
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Status
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                Detected At
              </th>
              {canResolve && (
                <th className="px-6 py-3 text-right text-xs font-medium uppercase tracking-wider text-gray-500 dark:text-gray-400">
                  Actions
                </th>
              )}
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={canResolve ? 8 : 7} className="text-center py-8">
                  Loading...
                </TableCell>
              </TableRow>
            ) : patterns?.content.length === 0 ? (
              <TableRow>
                <TableCell colSpan={canResolve ? 8 : 7} className="text-center py-8 text-gray-500">
                  No fraud patterns detected
                </TableCell>
              </TableRow>
            ) : (
              patterns?.content.map((pattern) => (
                <TableRow key={pattern.id}>
                  <TableCell className="px-6 py-4 font-medium text-gray-900 dark:text-white">
                    #{pattern.id}
                  </TableCell>
                  <TableCell className="px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
                    Transaction #{pattern.transactionId}
                  </TableCell>
                  <TableCell className="px-6 py-4">
                    <Badge color="info">{pattern.patternType}</Badge>
                  </TableCell>
                  <TableCell className="px-6 py-4">
                    <Badge color={getSeverityBadgeColor(pattern.severity)}>
                      {pattern.severity}
                    </Badge>
                  </TableCell>
                  <TableCell className="px-6 py-4">
                    <div className="max-w-xs truncate text-sm text-gray-500 dark:text-gray-400">
                      {pattern.description}
                    </div>
                  </TableCell>
                  <TableCell className="px-6 py-4">
                    {pattern.resolved ? (
                      <Badge color="success">Resolved</Badge>
                    ) : (
                      <Badge color="error">Unresolved</Badge>
                    )}
                  </TableCell>
                  <TableCell className="px-6 py-4 text-sm text-gray-500 dark:text-gray-400">
                    {formatDate(pattern.detectedAt)}
                  </TableCell>
                  {canResolve && (
                    <TableCell className="px-6 py-4 text-right">
                      {!pattern.resolved && (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleResolve(pattern.id)}
                        >
                          Resolve
                        </Button>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      {patterns && patterns.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-between">
          <div className="text-sm text-gray-500 dark:text-gray-400">
            Showing {patterns.content.length} of {patterns.totalElements} patterns
          </div>
          <div className="flex gap-2">
            <Button
              size="sm"
              variant="outline"
              disabled={patterns.page === 0}
              onClick={() => loadPatterns(patterns.page - 1)}
            >
              Previous
            </Button>
            <Button
              size="sm"
              variant="outline"
              disabled={patterns.page >= patterns.totalPages - 1}
              onClick={() => loadPatterns(patterns.page + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      {/* Detect Fraud Modal */}
      <DetectFraudModal
        isOpen={isDetectModalOpen}
        onClose={() => setIsDetectModalOpen(false)}
        onSuccess={() => {
          setIsDetectModalOpen(false);
          loadPatterns();
        }}
      />
    </div>
  );
};

// Detect Fraud Modal Component
const DetectFraudModal = ({ isOpen, onClose, onSuccess }: any) => {
  const [transactionId, setTransactionId] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<FraudDetectionResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleDetect = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const detectionResult = await fraudService.detectFraud(parseInt(transactionId));
      setResult(detectionResult);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to detect fraud');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setTransactionId('');
    setResult(null);
    setError(null);
    onClose();
  };

  return (
    <Modal isOpen={isOpen} onClose={handleClose} className="max-w-2xl p-6">
      <h2 className="mb-4 text-xl font-semibold text-gray-900 dark:text-white">
        Detect Fraud
      </h2>
      
      {error && (
        <div className="mb-4 rounded-lg bg-error-50 p-3 text-sm text-error-700 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}

      <Form onSubmit={handleDetect}>
        <div className="space-y-4">
          <div>
            <label className="mb-2 block text-sm font-medium text-gray-700 dark:text-gray-300">
              Transaction ID
            </label>
            <Input
              type="number"
              value={transactionId}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setTransactionId(e.target.value)}
              placeholder="Enter transaction ID"
            />
          </div>

          {!result && (
            <div className="flex gap-3">
              <Button disabled={loading} className="flex-1">
                {loading ? 'Analyzing...' : 'Detect Fraud'}
              </Button>
              <Button variant="outline" onClick={handleClose} className="flex-1">
                Cancel
              </Button>
            </div>
          )}
        </div>
      </Form>

      {/* Detection Result */}
      {result && (
        <div className="mt-6 space-y-4">
          <div className="rounded-lg border-2 border-gray-200 p-4 dark:border-gray-700">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                Detection Result
              </h3>
              <Badge color={result.isFraud ? 'error' : 'success'} size="md">
                {result.isFraud ? 'FRAUD DETECTED' : 'LEGITIMATE'}
              </Badge>
            </div>
            
            <div className="mt-4 grid grid-cols-2 gap-4">
              <div>
                <div className="text-sm text-gray-500 dark:text-gray-400">Confidence</div>
                <div className="mt-1 text-xl font-semibold text-gray-900 dark:text-white">
                  {(result.confidence * 100).toFixed(1)}%
                </div>
              </div>
              <div>
                <div className="text-sm text-gray-500 dark:text-gray-400">Fraud Score</div>
                <div className="mt-1 text-xl font-semibold text-gray-900 dark:text-white">
                  {(result.fraudScore * 100).toFixed(1)}%
                </div>
              </div>
            </div>

            <div className="mt-4">
              <div className="text-sm text-gray-500 dark:text-gray-400">Primary Reason</div>
              <div className="mt-1 text-sm text-gray-900 dark:text-white">
                {result.primaryReason}
              </div>
            </div>
          </div>

          {/* Model Predictions */}
          <div>
            <h4 className="mb-3 text-sm font-semibold text-gray-900 dark:text-white">
              Model Predictions
            </h4>
            <div className="space-y-2">
              {result.modelPredictions.map((prediction, index) => (
                <div
                  key={index}
                  className="rounded-lg border border-gray-200 p-3 dark:border-gray-700"
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="font-medium text-gray-900 dark:text-white">
                        {prediction.modelName}
                      </div>
                      <div className="text-sm text-gray-500 dark:text-gray-400">
                        {prediction.reason}
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                        {(prediction.confidence * 100).toFixed(1)}%
                      </span>
                      <Badge color={prediction.isFraud ? 'error' : 'success'} size="sm">
                        {prediction.isFraud ? 'Fraud' : 'Safe'}
                      </Badge>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="flex gap-3">
            <Button onClick={onSuccess} className="flex-1">
              View All Patterns
            </Button>
            <Button variant="outline" onClick={handleClose} className="flex-1">
              Close
            </Button>
          </div>
        </div>
      )}
    </Modal>
  );
};

export default FraudDetection;
