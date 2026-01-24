import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router";
import SignIn from "./pages/AuthPages/SignIn";
import Dashboard from "./pages/Dashboard";
import UserProfiles from "./pages/UserProfiles";
import UserManagement from "./pages/UserManagement";
import TransactionManagement from "./pages/TransactionManagement";
import FraudDetection from "./pages/FraudDetection";
import CreditRiskAssessment from "./pages/CreditRiskAssessment";
import AppLayout from "./layout/AppLayout";
import { ScrollToTop } from "./components/common/ScrollToTop";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/auth/ProtectedRoute";

export default function App() {
  return (
    <Router>
      <AuthProvider>
        <ScrollToTop />
        <Routes>
          {/* Dashboard Layout - Protected Routes */}
          <Route
            element={
              <ProtectedRoute>
                <AppLayout />
              </ProtectedRoute>
            }
          >
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/profile" element={<UserProfiles />} />
            <Route path="/users" element={<UserManagement />} />
            <Route path="/transactions" element={<TransactionManagement />} />
            <Route path="/fraud-detection" element={<FraudDetection />} />
            <Route path="/credit-risk" element={<CreditRiskAssessment />} />
          </Route>

          {/* Auth Routes - Public */}
          <Route path="/auth/signin" element={<SignIn />} />

          {/* Redirects */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/auth/signin" replace />} />
        </Routes>
      </AuthProvider>
    </Router>
  );
}
