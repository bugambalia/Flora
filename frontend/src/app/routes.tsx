import { createBrowserRouter, Navigate } from 'react-router';
import Layout from './Layout';
import AnalyticsPage from './pages/AnalyticsPage';
import InventoryPage from './pages/InventoryPage';
import POSPage from './pages/POSPage';
import SettingsPage from './pages/SettingsPage';
import CustomersPage from './pages/CustomersPage';
import OrdersPage from './pages/OrdersPage';

interface RouterConfig {
  currentUser: string;
  onLogout: () => void;
}

export function createRouter({ currentUser, onLogout }: RouterConfig) {
  return createBrowserRouter([
    {
      path: '/',
      element: <Layout currentUser={currentUser} onLogout={onLogout} />,
      children: [
        {
          index: true,
          element: <Navigate to="/analytics" replace />,
        },
        {
          path: 'analytics',
          element: <AnalyticsPage />,
        },
        {
          path: 'inventory',
          element: <InventoryPage />,
        },
        {
          path: 'pos',
          element: <POSPage />,
        },
        {
          path: 'orders',
          element: <OrdersPage />,
        },
        {
          path: 'customers',
          element: <CustomersPage />,
        },
        {
          path: 'settings',
          element: <SettingsPage />,
        },
        {
          path: '*',
          element: <Navigate to="/analytics" replace />,
        },
      ],
    },
  ]);
}