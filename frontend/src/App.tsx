import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { BasicLayout } from './layouts/BasicLayout';
import { ProductListPage } from './pages/products';
import { DashboardPage } from './pages/dashboard';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<BasicLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/products/list" element={<ProductListPage />} />
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
