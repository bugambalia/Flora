import React, { useState } from 'react';
import { Package, Search, Plus, Edit, Trash2, AlertCircle } from 'lucide-react';

interface InventoryItem {
  id: string;
  name: string;
  category: string;
  stock: number;
  minStock: number;
  price: number;
  lastRestocked: string;
}

const INITIAL_INVENTORY: InventoryItem[] = [
  { id: '1', name: 'Espresso Beans', category: 'Beverages', stock: 45, minStock: 20, price: 15.00, lastRestocked: '2026-02-15' },
  { id: '2', name: 'Milk', category: 'Beverages', stock: 12, minStock: 15, price: 4.50, lastRestocked: '2026-02-18' },
  { id: '3', name: 'Bread', category: 'Food', stock: 30, minStock: 10, price: 3.50, lastRestocked: '2026-02-19' },
  { id: '4', name: 'Cheese', category: 'Food', stock: 8, minStock: 10, price: 7.50, lastRestocked: '2026-02-17' },
  { id: '5', name: 'Lettuce', category: 'Food', stock: 25, minStock: 15, price: 2.50, lastRestocked: '2026-02-18' },
  { id: '6', name: 'Tomatoes', category: 'Food', stock: 18, minStock: 12, price: 3.00, lastRestocked: '2026-02-18' },
  { id: '7', name: 'Orange Juice', category: 'Beverages', stock: 22, minStock: 15, price: 5.00, lastRestocked: '2026-02-19' },
  { id: '8', name: 'Croissants', category: 'Pastries', stock: 15, minStock: 10, price: 4.25, lastRestocked: '2026-02-19' },
];

export default function InventoryPage() {
  const [inventory, setInventory] = useState<InventoryItem[]>(INITIAL_INVENTORY);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');

  const categories = ['All', 'Beverages', 'Food', 'Pastries'];

  const filteredInventory = inventory.filter(item => {
    const matchesCategory = selectedCategory === 'All' || item.category === selectedCategory;
    const matchesSearch = item.name.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const lowStockItems = inventory.filter(item => item.stock <= item.minStock);

  return (
    <div className="flex-1 overflow-y-auto p-8 bg-gray-50">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <h1>Inventory Management</h1>
          <button className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
            <Plus className="w-5 h-5" />
            Add Item
          </button>
        </div>

        {/* Low Stock Alert */}
        {lowStockItems.length > 0 && (
          <div className="mb-6 p-4 bg-orange-50 border border-orange-200 rounded-lg flex items-start gap-3">
            <AlertCircle className="w-5 h-5 text-orange-600 flex-shrink-0 mt-0.5" />
            <div>
              <p className="text-orange-900">
                <strong>Low Stock Alert:</strong> {lowStockItems.length} item{lowStockItems.length > 1 ? 's' : ''} need restocking
              </p>
              <p className="text-orange-700 text-sm mt-1">
                {lowStockItems.map(item => item.name).join(', ')}
              </p>
            </div>
          </div>
        )}

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-3 bg-blue-100 rounded-lg">
                <Package className="w-6 h-6 text-blue-600" />
              </div>
              <div>
                <p className="text-gray-600 text-sm">Total Items</p>
                <p className="text-2xl">{inventory.length}</p>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-3 bg-green-100 rounded-lg">
                <Package className="w-6 h-6 text-green-600" />
              </div>
              <div>
                <p className="text-gray-600 text-sm">In Stock</p>
                <p className="text-2xl">{inventory.filter(i => i.stock > i.minStock).length}</p>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-3 bg-orange-100 rounded-lg">
                <AlertCircle className="w-6 h-6 text-orange-600" />
              </div>
              <div>
                <p className="text-gray-600 text-sm">Low Stock</p>
                <p className="text-2xl">{lowStockItems.length}</p>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-3 bg-purple-100 rounded-lg">
                <Package className="w-6 h-6 text-purple-600" />
              </div>
              <div>
                <p className="text-gray-600 text-sm">Total Value</p>
                <p className="text-2xl">
                  ${inventory.reduce((sum, item) => sum + (item.stock * item.price), 0).toFixed(0)}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Filters */}
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 mb-6">
          <div className="flex flex-col md:flex-row gap-4">
            {/* Search */}
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Search inventory..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Category Filter */}
            <div className="flex gap-2">
              {categories.map(category => (
                <button
                  key={category}
                  onClick={() => setSelectedCategory(category)}
                  className={`px-4 py-2 rounded-lg transition-colors ${
                    selectedCategory === category
                      ? 'bg-blue-600 text-white'
                      : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
                >
                  {category}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Inventory Table */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Item Name</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Category</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Stock</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Min Stock</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Price</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Last Restocked</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredInventory.map((item) => {
                  const isLowStock = item.stock <= item.minStock;
                  return (
                    <tr key={item.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <span className="text-gray-900">{item.name}</span>
                          {isLowStock && (
                            <span className="px-2 py-0.5 bg-orange-100 text-orange-700 text-xs rounded-full">
                              Low
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4 text-gray-600">{item.category}</td>
                      <td className="px-6 py-4">
                        <span className={`${isLowStock ? 'text-orange-600' : 'text-gray-900'}`}>
                          {item.stock}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-gray-600">{item.minStock}</td>
                      <td className="px-6 py-4 text-gray-900">${item.price.toFixed(2)}</td>
                      <td className="px-6 py-4 text-gray-600">{item.lastRestocked}</td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <button className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors">
                            <Edit className="w-4 h-4" />
                          </button>
                          <button className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors">
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
