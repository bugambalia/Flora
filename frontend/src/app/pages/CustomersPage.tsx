import React, { useState } from 'react';
import { Users, Search, Plus, Edit, Trash2, Mail, Phone, MapPin, CreditCard, X } from 'lucide-react';

export interface Customer {
  id: string;
  name: string;
  nitCedula: string;
  email: string;
  phone?: string;
  address?: string;
  loyaltyPoints: number;
  totalPurchases: number;
  createdAt: string;
}

const INITIAL_CUSTOMERS: Customer[] = [
  {
    id: '1',
    name: 'John Smith',
    nitCedula: '123456789',
    email: 'john.smith@email.com',
    phone: '(555) 123-4567',
    address: '123 Main St, City',
    loyaltyPoints: 150,
    totalPurchases: 1250.50,
    createdAt: '2026-01-15',
  },
  {
    id: '2',
    name: 'Maria Garcia',
    nitCedula: '987654321',
    email: 'maria.garcia@email.com',
    phone: '(555) 234-5678',
    address: '456 Oak Ave, Town',
    loyaltyPoints: 320,
    totalPurchases: 2890.75,
    createdAt: '2025-12-20',
  },
  {
    id: '3',
    name: 'David Chen',
    nitCedula: '456789123',
    email: 'david.chen@email.com',
    phone: '(555) 345-6789',
    address: '',
    loyaltyPoints: 89,
    totalPurchases: 567.30,
    createdAt: '2026-02-01',
  },
  {
    id: '4',
    name: 'Sarah Johnson',
    nitCedula: '789123456',
    email: 'sarah.j@email.com',
    phone: '',
    address: '789 Pine Rd, Village',
    loyaltyPoints: 245,
    totalPurchases: 1890.00,
    createdAt: '2026-01-10',
  },
  {
    id: '5',
    name: 'Michael Brown',
    nitCedula: '321654987',
    email: 'm.brown@email.com',
    phone: '(555) 567-8901',
    address: '321 Elm St, City',
    loyaltyPoints: 412,
    totalPurchases: 3456.80,
    createdAt: '2025-11-28',
  },
];

interface CustomerFormData {
  name: string;
  nitCedula: string;
  email: string;
  phone: string;
  address: string;
}

export default function CustomersPage() {
  const [customers, setCustomers] = useState<Customer[]>(INITIAL_CUSTOMERS);
  const [searchQuery, setSearchQuery] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null);
  const [formData, setFormData] = useState<CustomerFormData>({
    name: '',
    nitCedula: '',
    email: '',
    phone: '',
    address: '',
  });
  const [formErrors, setFormErrors] = useState<Partial<CustomerFormData>>({});

  const filteredCustomers = customers.filter(customer =>
    customer.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    customer.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
    customer.nitCedula.includes(searchQuery) ||
    customer.phone?.includes(searchQuery)
  );

  const validateForm = (): boolean => {
    const errors: Partial<CustomerFormData> = {};

    if (!formData.name.trim()) {
      errors.name = 'Name is required';
    }

    if (!formData.nitCedula.trim()) {
      errors.nitCedula = 'NIT/Cédula is required';
    }

    if (!formData.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = 'Invalid email format';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleOpenModal = (customer?: Customer) => {
    if (customer) {
      setEditingCustomer(customer);
      setFormData({
        name: customer.name,
        nitCedula: customer.nitCedula,
        email: customer.email,
        phone: customer.phone || '',
        address: customer.address || '',
      });
    } else {
      setEditingCustomer(null);
      setFormData({
        name: '',
        nitCedula: '',
        email: '',
        phone: '',
        address: '',
      });
    }
    setFormErrors({});
    setShowModal(true);
  };

  const handleCloseModal = () => {
    setShowModal(false);
    setEditingCustomer(null);
    setFormData({
      name: '',
      nitCedula: '',
      email: '',
      phone: '',
      address: '',
    });
    setFormErrors({});
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    if (editingCustomer) {
      // Update existing customer
      setCustomers(prev =>
        prev.map(customer =>
          customer.id === editingCustomer.id
            ? {
                ...customer,
                name: formData.name.trim(),
                nitCedula: formData.nitCedula.trim(),
                email: formData.email.trim(),
                phone: formData.phone.trim(),
                address: formData.address.trim(),
              }
            : customer
        )
      );
    } else {
      // Add new customer
      const newCustomer: Customer = {
        id: Date.now().toString(),
        name: formData.name.trim(),
        nitCedula: formData.nitCedula.trim(),
        email: formData.email.trim(),
        phone: formData.phone.trim(),
        address: formData.address.trim(),
        loyaltyPoints: 0,
        totalPurchases: 0,
        createdAt: new Date().toISOString().split('T')[0],
      };
      setCustomers(prev => [newCustomer, ...prev]);
    }

    handleCloseModal();
  };

  const handleDelete = (id: string) => {
    if (confirm('Are you sure you want to delete this customer?')) {
      setCustomers(prev => prev.filter(customer => customer.id !== id));
    }
  };

  const totalCustomers = customers.length;
  const totalRevenue = customers.reduce((sum, customer) => sum + customer.totalPurchases, 0);
  const averagePurchase = totalCustomers > 0 ? totalRevenue / totalCustomers : 0;

  return (
    <div className="flex-1 overflow-y-auto p-8 bg-gray-50">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <h1>Customer Management</h1>
          <button
            onClick={() => handleOpenModal()}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Plus className="w-5 h-5" />
            Add Customer
          </button>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-3 bg-blue-100 rounded-lg">
                <Users className="w-6 h-6 text-blue-600" />
              </div>
              <div>
                <p className="text-gray-600 text-sm">Total Customers</p>
                <p className="text-2xl">{totalCustomers}</p>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-3 bg-green-100 rounded-lg">
                <CreditCard className="w-6 h-6 text-green-600" />
              </div>
              <div>
                <p className="text-gray-600 text-sm">Total Revenue</p>
                <p className="text-2xl">${totalRevenue.toFixed(2)}</p>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-3 bg-purple-100 rounded-lg">
                <CreditCard className="w-6 h-6 text-purple-600" />
              </div>
              <div>
                <p className="text-gray-600 text-sm">Avg. Purchase</p>
                <p className="text-2xl">${averagePurchase.toFixed(2)}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Search */}
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 mb-6">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search by name, email, NIT/Cédula, or phone..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        {/* Customers Table */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Customer</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">NIT/Cédula</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Contact</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Address</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Loyalty Points</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Total Purchases</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredCustomers.length > 0 ? (
                  filteredCustomers.map((customer) => (
                    <tr key={customer.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                      <td className="px-6 py-4">
                        <div>
                          <p className="text-gray-900">{customer.name}</p>
                          <p className="text-sm text-gray-500">Since {customer.createdAt}</p>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-gray-900">{customer.nitCedula}</td>
                      <td className="px-6 py-4">
                        <div className="space-y-1">
                          <div className="flex items-center gap-2 text-gray-600 text-sm">
                            <Mail className="w-4 h-4" />
                            <span>{customer.email}</span>
                          </div>
                          {customer.phone && (
                            <div className="flex items-center gap-2 text-gray-600 text-sm">
                              <Phone className="w-4 h-4" />
                              <span>{customer.phone}</span>
                            </div>
                          )}
                        </div>
                      </td>
                      <td className="px-6 py-4">
                        {customer.address ? (
                          <div className="flex items-center gap-2 text-gray-600 text-sm">
                            <MapPin className="w-4 h-4 flex-shrink-0" />
                            <span>{customer.address}</span>
                          </div>
                        ) : (
                          <span className="text-gray-400 text-sm">-</span>
                        )}
                      </td>
                      <td className="px-6 py-4">
                        <span className="px-3 py-1 bg-blue-100 text-blue-700 rounded-full text-sm">
                          {customer.loyaltyPoints}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-gray-900">
                        ${customer.totalPurchases.toFixed(2)}
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => handleOpenModal(customer)}
                            className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                          >
                            <Edit className="w-4 h-4" />
                          </button>
                          <button
                            onClick={() => handleDelete(customer.id)}
                            className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={7} className="px-6 py-12 text-center text-gray-500">
                      No customers found
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Add/Edit Customer Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-gray-200 sticky top-0 bg-white">
              <div className="flex items-center justify-between">
                <h2>{editingCustomer ? 'Edit Customer' : 'Add New Customer'}</h2>
                <button
                  onClick={handleCloseModal}
                  className="p-1 text-gray-400 hover:text-gray-600 transition-colors"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-6">
              {/* Name - Required */}
              <div>
                <label className="block text-sm text-gray-700 mb-2">
                  Full Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                    formErrors.name ? 'border-red-500' : 'border-gray-300'
                  }`}
                  placeholder="Enter customer name"
                />
                {formErrors.name && (
                  <p className="text-red-500 text-sm mt-1">{formErrors.name}</p>
                )}
              </div>

              {/* NIT/Cédula - Required */}
              <div>
                <label className="block text-sm text-gray-700 mb-2">
                  NIT/Cédula <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={formData.nitCedula}
                  onChange={(e) => setFormData({ ...formData, nitCedula: e.target.value })}
                  className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                    formErrors.nitCedula ? 'border-red-500' : 'border-gray-300'
                  }`}
                  placeholder="Enter NIT or Cédula number"
                />
                {formErrors.nitCedula && (
                  <p className="text-red-500 text-sm mt-1">{formErrors.nitCedula}</p>
                )}
              </div>

              {/* Email - Required */}
              <div>
                <label className="block text-sm text-gray-700 mb-2">
                  Email <span className="text-red-500">*</span>
                </label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    className={`w-full pl-10 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                      formErrors.email ? 'border-red-500' : 'border-gray-300'
                    }`}
                    placeholder="customer@email.com"
                  />
                </div>
                {formErrors.email && (
                  <p className="text-red-500 text-sm mt-1">{formErrors.email}</p>
                )}
              </div>

              {/* Phone - Optional */}
              <div>
                <label className="block text-sm text-gray-700 mb-2">
                  Phone Number
                </label>
                <div className="relative">
                  <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input
                    type="tel"
                    value={formData.phone}
                    onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                    className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    placeholder="(555) 123-4567"
                  />
                </div>
              </div>

              {/* Address - Optional */}
              <div>
                <label className="block text-sm text-gray-700 mb-2">
                  Address
                </label>
                <div className="relative">
                  <MapPin className="absolute left-3 top-3 w-5 h-5 text-gray-400" />
                  <textarea
                    value={formData.address}
                    onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                    className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                    placeholder="Enter customer address"
                    rows={3}
                  />
                </div>
              </div>

              {/* Form Actions */}
              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  className="flex-1 px-6 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                >
                  {editingCustomer ? 'Update Customer' : 'Add Customer'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
