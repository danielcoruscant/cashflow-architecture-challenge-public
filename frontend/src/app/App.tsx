import { Navigate, Route, Routes } from 'react-router-dom';
import { DashboardPage } from '../pages/DashboardPage';
import { AuthGate } from '../routes/AuthGate';

export function App() {
  return (
    <Routes>
      <Route path="/" element={<AuthGate><DashboardPage /></AuthGate>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
