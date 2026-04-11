import React from 'react';
import { BarChart3, TrendingUp, DollarSign, ShoppingBag, ArrowUp, ArrowDown } from 'lucide-react';
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

const salesData = [
  { name: 'Mon', sales: 1200, orders: 45 },
  { name: 'Tue', sales: 1900, orders: 67 },
  { name: 'Wed', sales: 1500, orders: 52 },
  { name: 'Thu', sales: 2100, orders: 78 },
  { name: 'Fri', sales: 2800, orders: 95 },
  { name: 'Sat', sales: 3200, orders: 112 },
  { name: 'Sun', sales: 2600, orders: 89 },
];

const categoryData = [
  { name: 'Beverages', value: 45, color: '#3B82F6' },
  { name: 'Food', value: 35, color: '#10B981' },
  { name: 'Pastries', value: 20, color: '#F59E0B' },
];

const topProducts = [
  { name: 'Espresso', sales: 145, revenue: 507.50 },
  { name: 'Cheeseburger', sales: 98, revenue: 1225.00 },
  { name: 'Caesar Salad', sales: 87, revenue: 869.13 },
  { name: 'Cappuccino', sales: 134, revenue: 603.00 },
  { name: 'Club Sandwich', sales: 76, revenue: 683.24 },
];

export default function AnalyticsPage() {
  return (
    <div className="flex-1 overflow-y-auto p-8 bg-gray-50">
      <div className="max-w-7xl mx-auto">
        <h1 className="mb-8">Analytics Dashboard</h1>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {/* Total Sales */}
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-blue-100 rounded-lg">
                <DollarSign className="w-6 h-6 text-blue-600" />
              </div>
              <span className="flex items-center text-green-600 text-sm">
                <ArrowUp className="w-4 h-4 mr-1" />
                12.5%
              </span>
            </div>
            <h3 className="text-gray-600 text-sm mb-1">Total Sales</h3>
            <p className="text-3xl">$15,300</p>
            <p className="text-gray-500 text-sm mt-2">This week</p>
          </div>

          {/* Total Orders */}
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-green-100 rounded-lg">
                <ShoppingBag className="w-6 h-6 text-green-600" />
              </div>
              <span className="flex items-center text-green-600 text-sm">
                <ArrowUp className="w-4 h-4 mr-1" />
                8.2%
              </span>
            </div>
            <h3 className="text-gray-600 text-sm mb-1">Total Orders</h3>
            <p className="text-3xl">538</p>
            <p className="text-gray-500 text-sm mt-2">This week</p>
          </div>

          {/* Average Order */}
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-purple-100 rounded-lg">
                <TrendingUp className="w-6 h-6 text-purple-600" />
              </div>
              <span className="flex items-center text-red-600 text-sm">
                <ArrowDown className="w-4 h-4 mr-1" />
                2.1%
              </span>
            </div>
            <h3 className="text-gray-600 text-sm mb-1">Average Order</h3>
            <p className="text-3xl">$28.44</p>
            <p className="text-gray-500 text-sm mt-2">Per transaction</p>
          </div>

          {/* Top Category */}
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center justify-between mb-4">
              <div className="p-3 bg-orange-100 rounded-lg">
                <BarChart3 className="w-6 h-6 text-orange-600" />
              </div>
              <span className="flex items-center text-green-600 text-sm">
                <ArrowUp className="w-4 h-4 mr-1" />
                15.3%
              </span>
            </div>
            <h3 className="text-gray-600 text-sm mb-1">Top Category</h3>
            <p className="text-3xl">Beverages</p>
            <p className="text-gray-500 text-sm mt-2">45% of sales</p>
          </div>
        </div>

        {/* Charts Section */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
          {/* Sales Trend */}
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <h2 className="mb-6">Sales Trend</h2>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={salesData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="name" stroke="#6b7280" />
                <YAxis stroke="#6b7280" />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: 'white', 
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px' 
                  }}
                />
                <Line 
                  type="monotone" 
                  dataKey="sales" 
                  stroke="#3B82F6" 
                  strokeWidth={3}
                  dot={{ fill: '#3B82F6', r: 4 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </div>

          {/* Orders by Day */}
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <h2 className="mb-6">Orders by Day</h2>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={salesData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
                <XAxis dataKey="name" stroke="#6b7280" />
                <YAxis stroke="#6b7280" />
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: 'white', 
                    border: '1px solid #e5e7eb',
                    borderRadius: '8px' 
                  }}
                />
                <Bar dataKey="orders" fill="#10B981" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Bottom Section */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Sales by Category */}
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <h2 className="mb-6">Sales by Category</h2>
            <div className="flex justify-center mb-6">
              <ResponsiveContainer width="100%" height={200}>
                <PieChart>
                  <Pie
                    data={categoryData}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {categoryData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                </PieChart>
              </ResponsiveContainer>
            </div>
            <div className="space-y-3">
              {categoryData.map((category) => (
                <div key={category.name} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <div 
                      className="w-3 h-3 rounded-full" 
                      style={{ backgroundColor: category.color }}
                    />
                    <span className="text-gray-700">{category.name}</span>
                  </div>
                  <span className="text-gray-900">{category.value}%</span>
                </div>
              ))}
            </div>
          </div>

          {/* Top Products */}
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 lg:col-span-2">
            <h2 className="mb-6">Top Products</h2>
            <div className="space-y-4">
              {topProducts.map((product, index) => (
                <div key={product.name} className="flex items-center gap-4">
                  <div className="flex items-center justify-center w-8 h-8 bg-blue-100 text-blue-600 rounded-lg text-sm">
                    {index + 1}
                  </div>
                  <div className="flex-1">
                    <p className="text-gray-900">{product.name}</p>
                    <p className="text-gray-500 text-sm">{product.sales} sales</p>
                  </div>
                  <div className="text-right">
                    <p className="text-gray-900">${product.revenue.toFixed(2)}</p>
                    <p className="text-gray-500 text-sm">Revenue</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
