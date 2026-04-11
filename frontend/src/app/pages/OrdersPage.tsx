import React, { useState, useEffect } from 'react';
import { Search, FileText, DollarSign, Clock, CheckCircle, XCircle, Eye, Edit2, Trash2, Filter } from 'lucide-react';
import { getOrders, updateOrder, deleteOrder, Order, OrderStatus, PaymentStatus } from '../types/order';

const ORDER_STATUS_CONFIG = {
  pending: { label: 'Pending', color: 'bg-yellow-100 text-yellow-700', icon: Clock },
  ready: { label: 'Ready', color: 'bg-blue-100 text-blue-700', icon: CheckCircle },
  completed: { label: 'Completed', color: 'bg-green-100 text-green-700', icon: CheckCircle },
  cancelled: { label: 'Cancelled', color: 'bg-red-100 text-red-700', icon: XCircle },
};

const PAYMENT_STATUS_CONFIG = {
  paid: { label: 'Paid', color: 'bg-green-100 text-green-700' },
  unpaid: { label: 'Unpaid', color: 'bg-orange-100 text-orange-700' },
};

export default function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState<'all' | OrderStatus>('all');
  const [filterPayment, setFilterPayment] = useState<'all' | PaymentStatus>('all');
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);

  useEffect(() => {
    loadOrders();
  }, []);

  const loadOrders = () => {
    setOrders(getOrders());
  };

  const filteredOrders = orders.filter(order => {
    const matchesSearch = 
      order.orderNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      order.customer?.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      order.customer?.email.toLowerCase().includes(searchQuery.toLowerCase());
    
    const matchesStatus = filterStatus === 'all' || order.orderStatus === filterStatus;
    const matchesPayment = filterPayment === 'all' || order.paymentStatus === filterPayment;

    return matchesSearch && matchesStatus && matchesPayment;
  });

  const handleStatusChange = (orderId: string, newStatus: OrderStatus) => {
    updateOrder(orderId, { orderStatus: newStatus });
    loadOrders();
    if (selectedOrder && selectedOrder.id === orderId) {
      setSelectedOrder({ ...selectedOrder, orderStatus: newStatus });
    }
  };

  const handlePaymentStatusChange = (orderId: string, newStatus: PaymentStatus) => {
    updateOrder(orderId, { paymentStatus: newStatus });
    loadOrders();
    if (selectedOrder && selectedOrder.id === orderId) {
      setSelectedOrder({ ...selectedOrder, paymentStatus: newStatus });
    }
  };

  const handleDeleteOrder = (orderId: string) => {
    if (confirm('Are you sure you want to delete this order?')) {
      deleteOrder(orderId);
      loadOrders();
      if (selectedOrder && selectedOrder.id === orderId) {
        setShowDetailsModal(false);
        setSelectedOrder(null);
      }
    }
  };

  const openOrderDetails = (order: Order) => {
    setSelectedOrder(order);
    setShowDetailsModal(true);
  };

  // Calculate statistics
  const totalOrders = orders.length;
  const totalRevenue = orders
    .filter(o => o.paymentStatus === 'paid')
    .reduce((sum, order) => sum + order.total, 0);
  const pendingOrders = orders.filter(o => o.orderStatus === 'pending').length;
  const unpaidOrders = orders.filter(o => o.paymentStatus === 'unpaid').length;

  return (
    <div className="flex-1 overflow-y-auto p-8 bg-gray-50">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <h1 className="mb-2">Orders Management</h1>
          <p className="text-gray-600">Track and manage all customer orders</p>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-3 bg-blue-100 rounded-lg">
                <FileText className="w-6 h-6 text-blue-600" />
              </div>
              <div>
                <p className="text-gray-600 text-sm">Total Orders</p>
                <p className="text-2xl">{totalOrders}</p>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-3 bg-green-100 rounded-lg">
                <DollarSign className="w-6 h-6 text-green-600" />
              </div>
              <div>
                <p className="text-gray-600 text-sm">Total Revenue</p>
                <p className="text-2xl">${totalRevenue.toFixed(2)}</p>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-3 bg-yellow-100 rounded-lg">
                <Clock className="w-6 h-6 text-yellow-600" />
              </div>
              <div>
                <p className="text-gray-600 text-sm">Pending Orders</p>
                <p className="text-2xl">{pendingOrders}</p>
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200">
            <div className="flex items-center gap-3 mb-2">
              <div className="p-3 bg-orange-100 rounded-lg">
                <DollarSign className="w-6 h-6 text-orange-600" />
              </div>
              <div>
                <p className="text-gray-600 text-sm">Unpaid Orders</p>
                <p className="text-2xl">{unpaidOrders}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Search and Filters */}
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 mb-6">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Search */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <input
                type="text"
                placeholder="Search by order #, customer..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Order Status Filter */}
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <select
                value={filterStatus}
                onChange={(e) => setFilterStatus(e.target.value as 'all' | OrderStatus)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 appearance-none"
              >
                <option value="all">All Statuses</option>
                <option value="pending">Pending</option>
                <option value="ready">Ready</option>
                <option value="completed">Completed</option>
                <option value="cancelled">Cancelled</option>
              </select>
            </div>

            {/* Payment Status Filter */}
            <div className="relative">
              <Filter className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
              <select
                value={filterPayment}
                onChange={(e) => setFilterPayment(e.target.value as 'all' | PaymentStatus)}
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 appearance-none"
              >
                <option value="all">All Payments</option>
                <option value="paid">Paid</option>
                <option value="unpaid">Unpaid</option>
              </select>
            </div>
          </div>
        </div>

        {/* Orders Table */}
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Order #</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Customer</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Items</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Total</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Payment</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Status</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Date</th>
                  <th className="text-left px-6 py-4 text-sm text-gray-600">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredOrders.length > 0 ? (
                  filteredOrders.map((order) => {
                    const statusConfig = ORDER_STATUS_CONFIG[order.orderStatus];
                    const paymentConfig = PAYMENT_STATUS_CONFIG[order.paymentStatus];
                    const StatusIcon = statusConfig.icon;

                    return (
                      <tr key={order.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                        <td className="px-6 py-4">
                          <span className="text-blue-600">{order.orderNumber}</span>
                        </td>
                        <td className="px-6 py-4">
                          {order.customer ? (
                            <div>
                              <p className="text-gray-900">{order.customer.name}</p>
                              <p className="text-sm text-gray-500">{order.customer.email}</p>
                            </div>
                          ) : (
                            <span className="text-gray-400">Walk-in</span>
                          )}
                        </td>
                        <td className="px-6 py-4 text-gray-900">
                          {order.items.reduce((sum, item) => sum + item.quantity, 0)} items
                        </td>
                        <td className="px-6 py-4 text-gray-900">
                          ${order.total.toFixed(2)}
                        </td>
                        <td className="px-6 py-4">
                          <span className={`px-3 py-1 rounded-full text-xs ${paymentConfig.color}`}>
                            {paymentConfig.label}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-xs ${statusConfig.color}`}>
                            <StatusIcon className="w-3 h-3" />
                            {statusConfig.label}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-gray-600 text-sm">
                          {new Date(order.createdAt).toLocaleDateString()} {new Date(order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2">
                            <button
                              onClick={() => openOrderDetails(order)}
                              className="p-2 text-blue-600 hover:bg-blue-50 rounded-lg transition-colors"
                              title="View Details"
                            >
                              <Eye className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => handleDeleteOrder(order.id)}
                              className="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                              title="Delete Order"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan={8} className="px-6 py-12 text-center text-gray-500">
                      No orders found
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Order Details Modal */}
      {showDetailsModal && selectedOrder && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-gray-200 sticky top-0 bg-white">
              <div className="flex items-center justify-between">
                <div>
                  <h2>{selectedOrder.orderNumber}</h2>
                  <p className="text-sm text-gray-500 mt-1">
                    {new Date(selectedOrder.createdAt).toLocaleString()}
                  </p>
                </div>
                <button
                  onClick={() => setShowDetailsModal(false)}
                  className="p-1 text-gray-400 hover:text-gray-600 transition-colors"
                >
                  <XCircle className="w-6 h-6" />
                </button>
              </div>
            </div>

            <div className="p-6 space-y-6">
              {/* Customer Info */}
              <div>
                <h3 className="text-sm text-gray-700 mb-3">Customer Information</h3>
                {selectedOrder.customer ? (
                  <div className="p-4 bg-gray-50 rounded-lg space-y-2">
                    <div className="flex justify-between">
                      <span className="text-gray-600">Name:</span>
                      <span className="text-gray-900">{selectedOrder.customer.name}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-gray-600">Email:</span>
                      <span className="text-gray-900">{selectedOrder.customer.email}</span>
                    </div>
                    {selectedOrder.customer.phone && (
                      <div className="flex justify-between">
                        <span className="text-gray-600">Phone:</span>
                        <span className="text-gray-900">{selectedOrder.customer.phone}</span>
                      </div>
                    )}
                    <div className="flex justify-between">
                      <span className="text-gray-600">NIT/Cédula:</span>
                      <span className="text-gray-900">{selectedOrder.customer.nitCedula}</span>
                    </div>
                  </div>
                ) : (
                  <p className="text-gray-500 text-sm p-4 bg-gray-50 rounded-lg">Walk-in customer</p>
                )}
              </div>

              {/* Order Items */}
              <div>
                <h3 className="text-sm text-gray-700 mb-3">Order Items</h3>
                <div className="border border-gray-200 rounded-lg overflow-hidden">
                  {selectedOrder.items.map((item, index) => (
                    <div
                      key={index}
                      className="flex items-center justify-between p-4 border-b border-gray-200 last:border-0"
                    >
                      <div className="flex-1">
                        <p className="text-gray-900">{item.name}</p>
                        <p className="text-sm text-gray-500">
                          ${item.price.toFixed(2)} x {item.quantity}
                        </p>
                      </div>
                      <span className="text-gray-900">
                        ${(item.price * item.quantity).toFixed(2)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Order Summary */}
              <div>
                <h3 className="text-sm text-gray-700 mb-3">Order Summary</h3>
                <div className="p-4 bg-gray-50 rounded-lg space-y-2">
                  <div className="flex justify-between text-gray-600">
                    <span>Subtotal</span>
                    <span>${selectedOrder.subtotal.toFixed(2)}</span>
                  </div>
                  {selectedOrder.discount && (
                    <div className="flex justify-between text-green-600">
                      <span>Discount ({selectedOrder.discount.name})</span>
                      <span>-${selectedOrder.discountAmount.toFixed(2)}</span>
                    </div>
                  )}
                  <div className="flex justify-between text-gray-600">
                    <span>Tax</span>
                    <span>${selectedOrder.tax.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-xl pt-2 border-t border-gray-300">
                    <span>Total</span>
                    <span className="text-blue-600">${selectedOrder.total.toFixed(2)}</span>
                  </div>
                </div>
              </div>

              {/* Notes */}
              {selectedOrder.notes && (
                <div>
                  <h3 className="text-sm text-gray-700 mb-3">Order Notes</h3>
                  <p className="p-4 bg-gray-50 rounded-lg text-gray-900">
                    {selectedOrder.notes}
                  </p>
                </div>
              )}

              {/* Status Management */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Payment Status */}
                <div>
                  <h3 className="text-sm text-gray-700 mb-3">Payment Status</h3>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handlePaymentStatusChange(selectedOrder.id, 'paid')}
                      className={`flex-1 py-2 px-4 rounded-lg border-2 transition-colors ${
                        selectedOrder.paymentStatus === 'paid'
                          ? 'border-green-600 bg-green-50 text-green-900'
                          : 'border-gray-200 text-gray-700 hover:border-gray-300'
                      }`}
                    >
                      Paid
                    </button>
                    <button
                      onClick={() => handlePaymentStatusChange(selectedOrder.id, 'unpaid')}
                      className={`flex-1 py-2 px-4 rounded-lg border-2 transition-colors ${
                        selectedOrder.paymentStatus === 'unpaid'
                          ? 'border-orange-600 bg-orange-50 text-orange-900'
                          : 'border-gray-200 text-gray-700 hover:border-gray-300'
                      }`}
                    >
                      Unpaid
                    </button>
                  </div>
                </div>

                {/* Order Status */}
                <div>
                  <h3 className="text-sm text-gray-700 mb-3">Order Status</h3>
                  <select
                    value={selectedOrder.orderStatus}
                    onChange={(e) => handleStatusChange(selectedOrder.id, e.target.value as OrderStatus)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="pending">Pending</option>
                    <option value="ready">Ready</option>
                    <option value="completed">Completed</option>
                    <option value="cancelled">Cancelled</option>
                  </select>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
