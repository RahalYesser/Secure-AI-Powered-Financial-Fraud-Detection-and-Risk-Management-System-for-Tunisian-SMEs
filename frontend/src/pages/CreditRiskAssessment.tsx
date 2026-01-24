import { useState, useEffect } from 'react';
import { creditRiskService } from '../services/creditRiskService';
import { 
  CreditRiskAssessment,
  RiskAssessment,
  RiskCategory,
  FinancialData
} from '../types/credit-risk.types';
import { PaginatedResponse } from '../types/common.types';
import { Modal } from '../components/ui/modal';
import Button from '../components/ui/button/Button';
import Badge from '../components/ui/badge/Badge';
import Input from '../components/form/input/InputField';
import { Table, TableHeader, TableBody, TableRow, TableCell } from '../components/ui/table';
import { usePermission } from '../hooks/usePermission';

const CreditRiskAssessmentPage = () => {
  const { hasPermission } = usePermission();
  const [assessments, setAssessments] = useState<PaginatedResponse<CreditRiskAssessment> | null>(null);
  const [loading, setLoading] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState<RiskCategory | 'ALL'>('ALL');
  const [showUnreviewedOnly, setShowUnreviewedOnly] = useState(false);
  const [isAssessModalOpen, setIsAssessModalOpen] = useState(false);
  const [selectedAssessment, setSelectedAssessment] = useState<CreditRiskAssessment | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Check permissions
  const canAssess = hasPermission('CREDIT_RISK_ASSESS');
  const canView = hasPermission('CREDIT_RISK_VIEW');
  const canReview = hasPermission('CREDIT_RISK_REVIEW');

  useEffect(() => {
    if (canView) {
      loadAssessments();
    }
  }, [selectedCategory, showUnreviewedOnly]);

  const loadAssessments = async (page = 0, size = 10) => {
    setLoading(true);
    setError(null);
    try {
      let response;
      
      if (showUnreviewedOnly) {
        response = await creditRiskService.getUnreviewedHighRiskAssessments({ page, size });
      } else if (selectedCategory !== 'ALL') {
        response = await creditRiskService.getAssessmentsByCategory(selectedCategory as RiskCategory, { page, size });
      } else {
        response = await creditRiskService.getAllAssessments({ page, size });
      }
      
      setAssessments(response);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load assessments');
    } finally {
      setLoading(false);
    }
  };

  const handleReview = async (assessmentId: number, notes: string) => {
    try {
      await creditRiskService.reviewAssessment(assessmentId, { reviewNotes: notes });
      loadAssessments();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to review assessment');
    }
  };

  const getRiskCategoryBadgeColor = (category: RiskCategory): 'success' | 'warning' | 'error' | 'light' => {
    switch (category) {
      case RiskCategory.LOW:
        return 'success';
      case RiskCategory.MEDIUM:
        return 'warning';
      case RiskCategory.HIGH:
        return 'error';
      case RiskCategory.CRITICAL:
        return 'error';
      default:
        return 'light';
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  if (!canView) {
    return (
      <div className="p-6">
        <div className="rounded-lg bg-error-50 p-4 text-error-700 dark:bg-error-500/10 dark:text-error-400">
          You don't have permission to access credit risk assessments.
        </div>
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Credit Risk Assessments</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Assess and monitor credit risk for SME users
        </p>
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-error-50 p-4 text-error-700 dark:bg-error-500/10 dark:text-error-400">
          {error}
        </div>
      )}

      <div className="mb-6 flex flex-wrap gap-4">
        {canAssess && (
          <Button onClick={() => setIsAssessModalOpen(true)}>
            New Assessment
          </Button>
        )}

        <select
          value={selectedCategory}
          onChange={(e) => setSelectedCategory(e.target.value as RiskCategory | 'ALL')}
          className="rounded-lg border border-gray-300 bg-white px-4 py-2 dark:border-gray-600 dark:bg-gray-800"
        >
          <option value="ALL">All Categories</option>
          <option value={RiskCategory.LOW}>Low Risk</option>
          <option value={RiskCategory.MEDIUM}>Medium Risk</option>
          <option value={RiskCategory.HIGH}>High Risk</option>
          <option value={RiskCategory.CRITICAL}>Critical Risk</option>
        </select>

        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={showUnreviewedOnly}
            onChange={(e) => setShowUnreviewedOnly(e.target.checked)}
            className="rounded"
          />
          <span className="text-sm text-gray-700 dark:text-gray-300">Unreviewed High Risk Only</span>
        </label>
      </div>

      {loading && !assessments ? (
        <div className="flex h-64 items-center justify-center">
          <div className="text-gray-500 dark:text-gray-400">Loading assessments...</div>
        </div>
      ) : assessments?.content.length === 0 ? (
        <div className="rounded-lg border border-gray-200 p-8 text-center dark:border-gray-700">
          <p className="text-gray-500 dark:text-gray-400">No assessments found</p>
        </div>
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-gray-200 dark:border-gray-700">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableCell>Assessment ID</TableCell>
                  <TableCell>Risk Score</TableCell>
                  <TableCell>Category</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell>Assessed At</TableCell>
                  <TableCell>Actions</TableCell>
                </TableRow>
              </TableHeader>
              <TableBody>
                {assessments?.content.map((assessment) => (
                  <TableRow key={assessment.id}>
                    <TableCell>#{assessment.id}</TableCell>
                    <TableCell>
                      <span className="font-mono">{assessment.riskScore.toFixed(2)}</span>
                    </TableCell>
                    <TableCell>
                      <Badge color={getRiskCategoryBadgeColor(assessment.riskCategory)}>
                        {assessment.riskCategory}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {assessment.reviewed ? (
                        <Badge color="success">Reviewed</Badge>
                      ) : (
                        <Badge color="warning">Pending</Badge>
                      )}
                    </TableCell>
                    <TableCell>{formatDate(assessment.assessedAt)}</TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => setSelectedAssessment(assessment)}
                        >
                          View
                        </Button>
                        {!assessment.reviewed && canReview && (
                          <Button
                            size="sm"
                            onClick={() => {
                              const notes = prompt('Enter review notes:');
                              if (notes) handleReview(assessment.id, notes);
                            }}
                          >
                            Review
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          {/* Pagination */}
          {assessments && assessments.totalPages > 1 && (
            <div className="mt-6 flex items-center justify-center gap-2">
              <Button
                size="sm"
                variant="outline"
                disabled={assessments.number === 0}
                onClick={() => loadAssessments(assessments.number - 1, assessments.size)}
              >
                Previous
              </Button>
              <span className="text-sm text-gray-700 dark:text-gray-300">
                Page {assessments.number + 1} of {assessments.totalPages}
              </span>
              <Button
                size="sm"
                variant="outline"
                disabled={assessments.number + 1 >= assessments.totalPages}
                onClick={() => loadAssessments(assessments.number + 1, assessments.size)}
              >
                Next
              </Button>
            </div>
          )}
        </>
      )}

      {/* Assess Modal */}
      {isAssessModalOpen && (
        <AssessModal
          isOpen={isAssessModalOpen}
          onClose={() => setIsAssessModalOpen(false)}
          onSuccess={() => {
            setIsAssessModalOpen(false);
            loadAssessments();
          }}
        />
      )}

      {/* View Details Modal */}
      {selectedAssessment && (
        <ViewAssessmentModal
          assessment={selectedAssessment}
          onClose={() => setSelectedAssessment(null)}
        />
      )}
    </div>
  );
};

// Assess Modal Component
const AssessModal = ({ isOpen, onClose, onSuccess }: { isOpen: boolean; onClose: () => void; onSuccess: () => void }) => {
  const [formData, setFormData] = useState({
    smeUserId: '',
    totalRevenue: 0,
    totalExpenses: 0,
    totalAssets: 0,
    totalLiabilities: 0,
    cashFlow: 0,
    outstandingDebt: 0,
    creditHistory: 0,
    industryRisk: 0,
    marketConditions: 0,
    sector: ''
  });
  const [result, setResult] = useState<RiskAssessment | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'smeUserId' || name === 'sector' ? value : parseFloat(value) || 0
    }));
  };

  const handleSubmit = async () => {
    setLoading(true);
    setError(null);
    try {
      const financialData: FinancialData = {
        smeUserId: formData.smeUserId,
        totalRevenue: formData.totalRevenue,
        totalExpenses: formData.totalExpenses,
        totalAssets: formData.totalAssets,
        totalLiabilities: formData.totalLiabilities,
        cashFlow: formData.cashFlow,
        outstandingDebt: formData.outstandingDebt,
        creditHistory: formData.creditHistory,
        industryRisk: formData.industryRisk,
        marketConditions: formData.marketConditions,
        sector: formData.sector || undefined
      };
      
      const assessment = await creditRiskService.assessCreditRisk(financialData);
      setResult(assessment);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to perform assessment');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="p-6 max-h-[80vh] overflow-y-auto">
        <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
          New Credit Risk Assessment
        </h2>
        {!result ? (
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                SME User ID
              </label>
              <Input
                name="smeUserId"
                type="text"
                value={formData.smeUserId}
                onChange={handleChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Total Revenue
              </label>
              <Input
                name="totalRevenue"
                type="number"
                step={0.01}
                value={formData.totalRevenue.toString()}
                onChange={handleChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Total Expenses
              </label>
              <Input
                name="totalExpenses"
                type="number"
                step={0.01}
                value={formData.totalExpenses.toString()}
                onChange={handleChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Total Assets
              </label>
              <Input
                name="totalAssets"
                type="number"
                step={0.01}
                value={formData.totalAssets.toString()}
                onChange={handleChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Total Liabilities
              </label>
              <Input
                name="totalLiabilities"
                type="number"
                step={0.01}
                value={formData.totalLiabilities.toString()}
                onChange={handleChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Cash Flow
              </label>
              <Input
                name="cashFlow"
                type="number"
                step={0.01}
                value={formData.cashFlow.toString()}
                onChange={handleChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Outstanding Debt
              </label>
              <Input
                name="outstandingDebt"
                type="number"
                step={0.01}
                value={formData.outstandingDebt.toString()}
                onChange={handleChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Credit History (0-100)
              </label>
              <Input
                name="creditHistory"
                type="number"
                min="0"
                max="100"
                value={formData.creditHistory.toString()}
                onChange={handleChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Industry Risk (0-100)
              </label>
              <Input
                name="industryRisk"
                type="number"
                min="0"
                max="100"
                value={formData.industryRisk.toString()}
                onChange={handleChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Market Conditions (0-100)
              </label>
              <Input
                name="marketConditions"
                type="number"
                min="0"
                max="100"
                value={formData.marketConditions.toString()}
                onChange={handleChange}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Sector (Optional)
              </label>
              <Input
                name="sector"
                type="text"
                value={formData.sector}
                onChange={handleChange}
              />
            </div>

            {error && (
              <div className="rounded-lg bg-error-50 p-3 text-sm text-error-700 dark:bg-error-500/10 dark:text-error-400">
                {error}
              </div>
            )}

            <div className="flex justify-end gap-3">
              <Button variant="outline" onClick={onClose}>
                Cancel
              </Button>
              <Button disabled={loading} onClick={handleSubmit}>
                {loading ? 'Assessing...' : 'Assess Risk'}
              </Button>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="rounded-lg bg-gray-50 p-4 dark:bg-gray-800">
              <h3 className="text-lg font-semibold mb-4">Assessment Results</h3>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Risk Score</p>
                  <p className="text-2xl font-bold text-gray-900 dark:text-white">
                    {result.riskScore.toFixed(2)}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Category</p>
                  <p className="text-2xl font-bold text-gray-900 dark:text-white">
                    {result.riskCategory}
                  </p>
                </div>
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Assessed At</p>
                  <p className="text-base font-medium text-gray-900 dark:text-white">
                    {new Date(result.assessedAt).toLocaleString()}
                  </p>
                </div>
              </div>
              {result.assessmentSummary && (
                <div className="mt-4">
                  <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">Summary</p>
                  <p className="text-sm text-gray-900 dark:text-white">{result.assessmentSummary}</p>
                </div>
              )}
              <div className="mt-4">
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">Model Predictions</p>
                <div className="space-y-2">
                  {result.modelPredictions.map((pred, idx) => (
                    <div key={idx} className="flex justify-between text-sm">
                      <div>
                        <p className="text-gray-700 dark:text-gray-300 font-medium">{pred.modelName}</p>
                        <p className="text-xs text-gray-500 dark:text-gray-400">Category: {pred.predictedCategory}</p>
                      </div>
                      <div className="text-right">
                        <p className="font-medium text-gray-900 dark:text-white">{pred.riskScore.toFixed(2)}</p>
                        <p className="text-xs text-gray-500 dark:text-gray-400 max-w-xs ml-auto">{pred.rationale}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
            <div className="flex justify-end">
              <Button onClick={() => { setResult(null); onSuccess(); }}>
                Close
              </Button>
            </div>
          </div>
        )}
      </div>
    </Modal>
  );
};

// View Assessment Modal Component
const ViewAssessmentModal = ({ assessment, onClose }: { assessment: CreditRiskAssessment; onClose: () => void }) => {
  return (
    <Modal isOpen={true} onClose={onClose}>
      <div className="p-6 max-h-[80vh] overflow-y-auto">
        <h2 className="mb-4 text-lg font-semibold text-gray-900 dark:text-white">
          Assessment Details
        </h2>
        <div className="space-y-4">
          {/* Risk Assessment */}
          <div className="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
            <h3 className="mb-3 font-semibold text-gray-900 dark:text-white">Risk Assessment</h3>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Risk Score</p>
                <p className="text-lg font-medium">{assessment.riskScore.toFixed(2)}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500 dark:text-gray-400">Category</p>
                <p className="text-lg font-medium">{assessment.riskCategory}</p>
              </div>
              {assessment.industrySector && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Industry Sector</p>
                  <p className="text-sm">{assessment.industrySector}</p>
                </div>
              )}
              {typeof assessment.debtRatio === 'number' && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Debt Ratio</p>
                  <p className="text-sm">{assessment.debtRatio.toFixed(2)}</p>
                </div>
              )}
              {typeof assessment.creditHistoryScore === 'number' && (
                <div>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Credit History Score</p>
                  <p className="text-sm">{assessment.creditHistoryScore.toFixed(1)}</p>
                </div>
              )}
            </div>
            {assessment.assessmentSummary && (
              <div className="mt-4">
                <p className="text-sm text-gray-500 dark:text-gray-400 mb-1">Summary</p>
                <p className="text-sm text-gray-900 dark:text-white">{assessment.assessmentSummary}</p>
              </div>
            )}
          </div>

          {/* Additional Information */}
          <div className="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
            <h3 className="mb-3 font-semibold text-gray-900 dark:text-white">Assessment Information</h3>
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <span className="text-gray-500 dark:text-gray-400">Assessment ID:</span>
                <span className="ml-2 font-medium">{assessment.id}</span>
              </div>
              <div>
                <span className="text-gray-500 dark:text-gray-400">Assessed At:</span>
                <span className="ml-2 font-medium">{new Date(assessment.assessedAt).toLocaleString()}</span>
              </div>
            </div>
          </div>

          {/* Review Information */}
          {assessment.reviewed && (
            <div className="rounded-lg border border-gray-200 p-4 dark:border-gray-700">
              <h3 className="mb-3 font-semibold text-gray-900 dark:text-white">Review</h3>
              <p className="text-sm text-gray-700 dark:text-gray-300">{assessment.reviewNotes}</p>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                Reviewed on {new Date(assessment.updatedAt || assessment.assessedAt).toLocaleString()}
              </p>
            </div>
          )}

          <div className="flex justify-end">
            <Button onClick={onClose}>Close</Button>
          </div>
        </div>
      </div>
    </Modal>
  );
};

export default CreditRiskAssessmentPage;
