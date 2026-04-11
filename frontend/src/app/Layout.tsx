import React from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router';
import { BarChart3, Package, ShoppingCart, Settings, LogOut, Users, FileText } from 'lucide-react';

interface LayoutProps {
  currentUser: string;
  onLogout: () => void;
}

const menuItems = [
  { path: '/analytics', icon: BarChart3, label: 'Analytics' },
  { path: '/inventory', icon: Package, label: 'Inventory' },
  { path: '/pos', icon: ShoppingCart, label: 'Point of Sale' },
  { path: '/orders', icon: FileText, label: 'Orders' },
  { path: '/customers', icon: Users, label: 'Customers' },
  { path: '/settings', icon: Settings, label: 'Settings' },
];

export default function Layout({ currentUser, onLogout }: LayoutProps) {
  const navigate = useNavigate();

  const handleLogout = () => {
    onLogout();
    navigate('/');
  };

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Sidebar */}
      <aside className="w-64 bg-white border-r border-gray-200 flex flex-col">
        {/* Logo/Brand */}
        <div className="p-6 border-b border-gray-200">
          <h1 className="text-xl">POS System</h1>
          <p className="text-sm text-gray-600 mt-1">Admin Dashboard</p>
        </div>

        {/* Navigation Menu */}
        <nav className="flex-1 p-4 space-y-2">
          {menuItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                    isActive
                      ? 'bg-blue-600 text-white'
                      : 'text-gray-700 hover:bg-gray-100'
                  }`
                }
              >
                <Icon className="w-5 h-5" />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* User Section */}
        <div className="p-4 border-t border-gray-200">
          <div className="flex items-center gap-3 mb-3 px-4 py-2">
            <div className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center text-white">
              {currentUser.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1">
              <p className="text-sm text-gray-900">{currentUser}</p>
              <p className="text-xs text-gray-500">Manager</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="w-full flex items-center gap-3 px-4 py-3 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <LogOut className="w-5 h-5" />
            <span>Logout</span>
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="flex-1 flex flex-col overflow-hidden">
        <Outlet />
      </main>
    </div>
  );
}