import React, { useState, useEffect, useMemo } from 'react';
import { Search, ShoppingCart, User, X, Percent, Tag, Save, CreditCard } from 'lucide-react';
import { ProductCard, Product } from '../components/ProductCard';
import { CartItem, CartItemType } from '../components/CartItem';
import { PaymentModal } from '../components/PaymentModal';
import { addOrder, getNextOrderNumber, Order } from '../types/order';
import { posService } from '../services/api';
import { useSaleStore } from '../services/saleStore';

const PRODUCTS: Product[] = [
  {
    id: '1',
    name: 'Espresso',
    price: 3.50,
    category: 'Beverages',
    image: 'https://images.unsplash.com/photo-1533776992670-a72f4c28235e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxjb2ZmZWUlMjBjdXB8ZW58MXx8fHwxNzY3MjQwMzUyfDA&ixlib=rb-4.1.0&q=80&w=1080',
  },
  {
    id: '2',
    name: 'Club Sandwich',
    price: 8.99,
    category: 'Food',
    image: 'https://images.unsplash.com/photo-1721980743593-9ff30ba867b8?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxzYW5kd2ljaCUyMGZvb2R8ZW58MXx8fHwxNzY3Mjc3MjY1fDA&ixlib=rb-4.1.0&q=80&w=1080',
  },
  {
    id: '3',
    name: 'Croissant',
    price: 4.25,
    category: 'Food',
    image: 'https://images.unsplash.com/photo-1712723246766-3eaea22e52ff?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxwYXN0cnklMjBjcm9pc3NhbnR8ZW58MXx8fHwxNzY3MjQ4ODE2fDA&ixlib=rb-4.1.0&q=80&w=1080',
  },
  {
    id: '4',
    name: 'Orange Juice',
    price: 4.50,
    category: 'Beverages',
    image: 'https://images.unsplash.com/photo-1544681655-6e5bff3ec253?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxqdWljZSUyMGRyaW5rfGVufDF8fHx8MTc2NzMwODY0OXww&ixlib=rb-4.1.0&q=80&w=1080',
  },
  {
    id: '5',
    name: 'Caesar Salad',
    price: 9.99,
    category: 'Food',
    image: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxzYWxhZCUyMGJvd2x8ZW58MXx8fHwxNzY3MjYwNzE5fDA&ixlib=rb-4.1.0&q=80&w=1080',
  },
  {
    id: '6',
    name: 'Cheeseburger',
    price: 12.50,
    category: 'Food',
    image: 'https://images.unsplash.com/photo-1607013401178-f9c15ab575bb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxidXJnZXIlMjBtZWFsfGVufDF8fHx8MTc2NzI1NTExM3ww&ixlib=rb-4.1.0&q=80&w=1080',
  },
  {
    id: '7',
    name: 'Cappuccino',
    price: 4.50,
    category: 'Beverages',
    image: 'https://images.unsplash.com/photo-1533776992670-a72f4c28235e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxjb2ZmZWUlMjBjdXB8ZW58MXx8fHwxNzY3MjQwMzUyfDA&ixlib=rb-4.1.0&q=80&w=1080',
  },
  {
    id: '8',
    name: 'Latte',
    price: 4.75,
    category: 'Beverages',
    image: 'https://images.unsplash.com/photo-1533776992670-a72f4c28235e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxjb2ZmZWUlMjBjdXB8ZW58MXx8fHwxNzY3MjQwMzUyfDA&ixlib=rb-4.1.0&q=80&w=1080',
  },
];

const CATEGORIES = ['All', 'Beverages', 'Food'];

// Mock customers - In a real app, these would come from a shared state or API
interface Customer {
  id: string;
  name: string;
  email: string;
  phone?: string;
  nitCedula: string;
  loyaltyPoints: number;
}

const CUSTOMERS: Customer[] = [
  { id: '1', name: 'John Smith', email: 'john.smith@email.com', phone: '(555) 123-4567', nitCedula: '123456789', loyaltyPoints: 150 },
  { id: '2', name: 'Maria Garcia', email: 'maria.garcia@email.com', phone: '(555) 234-5678', nitCedula: '987654321', loyaltyPoints: 320 },
  { id: '3', name: 'David Chen', email: 'david.chen@email.com', phone: '(555) 345-6789', nitCedula: '456789123', loyaltyPoints: 89 },
  { id: '4', name: 'Sarah Johnson', email: 'sarah.j@email.com', phone: '', nitCedula: '789123456', loyaltyPoints: 245 },
  { id: '5', name: 'Michael Brown', email: 'm.brown@email.com', phone: '(555) 567-8901', nitCedula: '321654987', loyaltyPoints: 412 },
];

// Standard discounts
interface Discount {
  id: string;
  name: string;
  type: 'percentage' | 'fixed';
  value: number;
}

const STANDARD_DISCOUNTS: Discount[] = [
  { id: '1', name: 'Senior Discount', type: 'percentage', value: 10 },
  { id: '2', name: 'Student Discount', type: 'percentage', value: 15 },
  { id: '3', name: 'Member Discount', type: 'percentage', value: 20 },
  { id: '4', name: 'Happy Hour', type: 'fixed', value: 5 },
];

const DEFAULT_CUSTOMER: Customer = {
  id: '0',
  name: 'Consumidor Final',
  email: '',
  nitCedula: '22.222.222',
  loyaltyPoints: 0
};

export default function POSPage() {
  const {
    activeSales,
    currentSaleId,
    addSale,
    removeSale,
    setCurrentSale,
    addItemToCurrentSale,
    updateItemQuantity,
    updateItemPrice,
    clearCurrentSale
  } = useSaleStore();

  const [selectedCategory, setSelectedCategory] = useState('All');
  const [searchQuery, setSearchQuery] = useState('');
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [showOrderModal, setShowOrderModal] = useState(false);

  // Active Sale derived state
  const activeSale = activeSales.find(s => s.id === currentSaleId) || activeSales[0];
  const { items: cart, total } = activeSale;

  // Local UI state (not persisted)
  const [orderNotes, setOrderNotes] = useState('');
  const [appliedDiscount, setAppliedDiscount] = useState<Discount | null>(null);

  // Customer search
  const [customerSearchQuery, setCustomerSearchQuery] = useState('');
  const [showCustomerDropdown, setShowCustomerDropdown] = useState(false);

  // Discount
  const [showDiscountModal, setShowDiscountModal] = useState(false);
  const [customDiscountType, setCustomDiscountType] = useState<'percentage' | 'fixed'>('percentage');
  const [customDiscountValue, setCustomDiscountValue] = useState('');

  // Order payment status (local to the checkout flow)
  const [orderPaymentStatus, setOrderPaymentStatus] = useState<'paid' | 'unpaid'>('unpaid');


  const filteredProducts = PRODUCTS.filter(product => {
    const matchesCategory = selectedCategory === 'All' || product.category === selectedCategory;
    const matchesSearch = product.name.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  const filteredCustomers = CUSTOMERS.filter(customer =>
    customer.name.toLowerCase().includes(customerSearchQuery.toLowerCase()) ||
    customer.phone?.includes(customerSearchQuery) ||
    customer.email.toLowerCase().includes(customerSearchQuery.toLowerCase())
  );

  const addToCart = (product: Product) => {
    addItemToCurrentSale({
      producto_id: parseInt(product.id),
      nombre: product.name,
      cantidad: 1,
      precio_aplicado: product.price,
      precio_original: product.price
    });
  };

  const updateQuantity = (id: string, quantity: number) => {
    updateItemQuantity(currentSaleId!, id, quantity);
  };

  const updatePrice = (id: string, price: number) => {
    updateItemPrice(currentSaleId!, id, price);
  };

  const removeFromCart = (id: string) => {
    updateItemQuantity(currentSaleId!, id, 0);
  };

  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);

  const selectCustomer = (customer: Customer) => {
    setSelectedCustomer(customer);
    setCustomerSearchQuery('');
    setShowCustomerDropdown(false);
  };

  const setExpressCustomer = () => {
    updateActiveTab({ selectedCustomer: DEFAULT_CUSTOMER });
  };

  const removeCustomer = () => {
    updateActiveTab({ selectedCustomer: null });
  };

  const applyStandardDiscount = (discount: Discount) => {
    updateActiveTab({ appliedDiscount: discount });
    setShowDiscountModal(false);
  };

  const applyCustomDiscount = () => {
    const value = parseFloat(customDiscountValue);
    if (isNaN(value) || value <= 0) {
      alert('Please enter a valid discount value');
      return;
    }

    if (customDiscountType === 'percentage' && value > 100) {
      alert('Percentage discount cannot exceed 100%');
      return;
    }

    updateActiveTab({
      appliedDiscount: {
        id: 'custom',
        name: 'Custom Discount',
        type: customDiscountType,
        value: value,
      }
    });
    setCustomDiscountValue('');
    setShowDiscountModal(false);
  };

  const removeDiscount = () => {
    updateActiveTab({ appliedDiscount: null });
  };

  // Calculate totals
  const subtotal = cart.reduce((sum, item) => sum + item.precio_aplicado * item.cantidad, 0);

  let discountAmount = 0;
  if (appliedDiscount) {
    if (appliedDiscount.type === 'percentage') {
      discountAmount = subtotal * (appliedDiscount.value / 100);
    } else {
      discountAmount = appliedDiscount.value;
    }
  }

  const subtotalAfterDiscount = subtotal - discountAmount;
  const tax = subtotalAfterDiscount * 0.08; // 8% tax
  const finalTotal = subtotalAfterDiscount + tax;

  const clearOrder = () => {
    clearCurrentSale();
    setSelectedCustomer(null);
    setAppliedDiscount(null);
    setOrderNotes('');
    setOrderPaymentStatus('unpaid');
  };

  const handleSaveOrder = async () => {
    const saleData = {
      items: cart,
      customer_id: selectedCustomer?.id || '0',
      total: finalTotal,
      notes: orderNotes
    };

    const response = await posService.createSale(saleData);
    if (response.status === 'success') {
      clearOrder();
      setShowOrderModal(false);
    } else {
      alert(`Error saving order: ${response.message}`);
    }
  };

  const handlePaymentComplete = () => {
    // Basic completion logic - in a real app, this would also hit the backend
    clearOrder();
    setShowPaymentModal(false);
    alert('Payment completed successfully! Order created.');
  };



  return (
    <div className="flex h-full bg-gray-100">
      {/* Left Side - Products */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Header */}
        <div className="bg-white border-b px-6 py-4">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-2xl font-bold">Point of Sale</h1>

            {/* Sales Tabs */}
            <div className="flex bg-gray-100 rounded-lg p-1">
              {activeSales.map((sale) => (
                <div key={sale.id} className="relative group">
                  <button
                    onClick={() => setCurrentSale(sale.id)}
                    className={`px-4 py-2 rounded-md text-sm font-medium transition-all flex items-center gap-2 ${currentSaleId === sale.id
                      ? 'bg-white text-blue-600 shadow-sm'
                      : 'text-gray-500 hover:text-gray-700'
                      }`}
                  >
                    Venta {sale.id}
                    {activeSales.length > 1 && (
                      <X
                        className="w-3 h-3 hover:text-red-500"
                        onClick={(e: React.MouseEvent) => {
                          e.stopPropagation();
                          removeSale(sale.id);
                        }}
                      />
                    )}
                  </button>
                </div>
              ))}
              {activeSales.length < 5 && (
                <button
                  onClick={addSale}
                  className="px-3 py-2 text-gray-500 hover:text-blue-600 transition-colors"
                  title="Nueva Venta"
                >
                  +
                </button>
              )}
            </div>
          </div>

          {/* Search Bar */}
          <div className="relative mb-4">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search products..."
              value={searchQuery}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Category Filters */}
          <div className="flex gap-2">
            {CATEGORIES.map(category => (
              <button
                key={category}
                onClick={() => setSelectedCategory(category)}
                className={`px-4 py-2 rounded-lg transition-colors ${selectedCategory === category
                  ? 'bg-blue-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                  }`}
              >
                {category}
              </button>
            ))}
          </div>
        </div>

        {/* Products Grid */}
        <div className="flex-1 overflow-y-auto p-6">
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {filteredProducts.map((product: Product) => (
              <ProductCard
                key={product.id}
                product={product}
                onAddToCart={(p: Product) => addToCart(p)}
              />
            ))}
          </div>
        </div>
      </div>

      {/* Right Side - Cart */}
      <div className="w-96 bg-white border-l flex flex-col">
        {/* Cart Header */}
        <div className="p-6 border-b">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2">
              <ShoppingCart className="w-6 h-6" />
              <h2>Venta #{activeTabId}</h2>
            </div>
            <p className="text-gray-600 text-sm">{cart.length} items</p>
          </div>
        </div>

        {/* Customer Search & Express Client */}
        <div className="p-6 border-b bg-gray-50 space-y-3">
          {selectedCustomer ? (
            <div className="bg-white p-4 rounded-lg border border-gray-200">
              <div className="flex items-start justify-between mb-2">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-blue-600 rounded-full flex items-center justify-center text-white">
                    {selectedCustomer.name.charAt(0)}
                  </div>
                  <div>
                    <p className="text-gray-900 font-medium">{selectedCustomer.name}</p>
                    <p className="text-sm text-gray-500">{selectedCustomer.nitCedula}</p>
                  </div>
                </div>
                <button
                  onClick={removeCustomer}
                  className="p-1 text-gray-400 hover:text-gray-600 transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>
          ) : (
            <div className="space-y-3">
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search customer..."
                  value={customerSearchQuery}
                  onChange={(e) => {
                    setCustomerSearchQuery(e.target.value);
                    setShowCustomerDropdown(true);
                  }}
                  onFocus={() => setShowCustomerDropdown(true)}
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />

                {/* Customer Dropdown */}
                {showCustomerDropdown && customerSearchQuery && (
                  <div className="absolute z-10 w-full mt-2 bg-white border border-gray-200 rounded-lg shadow-lg max-h-60 overflow-y-auto">
                    {filteredCustomers.length > 0 ? (
                      filteredCustomers.map(customer => (
                        <button
                          key={customer.id}
                          onClick={() => selectCustomer(customer)}
                          className="w-full p-3 text-left hover:bg-gray-50 transition-colors border-b border-gray-100 last:border-0"
                        >
                          <p className="text-gray-900">{customer.name}</p>
                          <p className="text-sm text-gray-500">{customer.nitCedula}</p>
                        </button>
                      ))
                    ) : (
                      <div className="p-4 text-center text-gray-500 text-sm">
                        No customers found
                      </div>
                    )}
                  </div>
                )}
              </div>
              <button
                onClick={setExpressCustomer}
                className="w-full py-2 bg-blue-50 text-blue-700 border border-blue-200 rounded-lg text-sm font-medium hover:bg-blue-100 transition-colors flex items-center justify-center gap-2"
              >
                <User className="w-4 h-4" />
                Consumidor Final
              </button>
            </div>
          )}
        </div>

        {/* Cart Items */}
        <div className="flex-1 overflow-y-auto p-6">
          {cart.length === 0 ? (
            <div className="text-center text-gray-400 mt-8">
              <ShoppingCart className="w-16 h-16 mx-auto mb-3 opacity-50" />
              <p>Cart is empty</p>
              <p className="text-sm">Add items to get started</p>
            </div>
          ) : (
            <div className="space-y-3">
              {cart.map((item) => (
                <CartItem
                  key={item.id}
                  item={{
                    id: item.id,
                    name: item.nombre,
                    price: item.precio_aplicado,
                    category: '', // Not strictly needed for CartItem display
                    image: '',
                    quantity: item.cantidad
                  }}
                  onUpdateQuantity={updateQuantity}
                  onUpdatePrice={updatePrice}
                  onRemove={removeFromCart}
                />
              ))}
            </div>
          )}
        </div>

        {/* Cart Footer */}
        {cart.length > 0 && (
          <div className="border-t p-6 space-y-4">
            {/* Discount Section */}
            <div>
              {appliedDiscount ? (
                <div className="flex items-center justify-between p-3 bg-green-50 border border-green-200 rounded-lg mb-3">
                  <div className="flex items-center gap-2">
                    <Tag className="w-4 h-4 text-green-600" />
                    <div>
                      <p className="text-sm text-green-900">{appliedDiscount.name}</p>
                      <p className="text-xs text-green-700">
                        {appliedDiscount.type === 'percentage'
                          ? `${appliedDiscount.value}% off`
                          : `$${appliedDiscount.value.toFixed(2)} off`}
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={removeDiscount}
                    className="p-1 text-green-600 hover:text-green-700 transition-colors"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>
              ) : (
                <button
                  onClick={() => setShowDiscountModal(true)}
                  className="w-full flex items-center justify-center gap-2 p-3 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
                >
                  <Percent className="w-4 h-4" />
                  <span>Apply Discount</span>
                </button>
              )}
            </div>

            {/* Totals */}
            <div className="space-y-2">
              <div className="flex justify-between text-gray-600">
                <span>Subtotal</span>
                <span>${subtotal.toFixed(2)}</span>
              </div>
              {appliedDiscount && (
                <div className="flex justify-between text-green-600">
                  <span>Discount</span>
                  <span>-${discountAmount.toFixed(2)}</span>
                </div>
              )}
              <div className="flex justify-between text-gray-600">
                <span>Tax (8%)</span>
                <span>${tax.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-xl pt-2 border-t">
                <span>Total</span>
                <span className="text-blue-600">${total.toFixed(2)}</span>
              </div>
            </div>

            {/* Action Buttons */}
            <div className="space-y-2">
              <button
                onClick={() => setShowOrderModal(true)}
                className="w-full flex items-center justify-center gap-2 py-3 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
              >
                <Save className="w-5 h-5" />
                Create Order
              </button>
              <button
                onClick={() => setShowPaymentModal(true)}
                className="w-full flex items-center justify-center gap-2 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                <CreditCard className="w-5 h-5" />
                Pay Now
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Create Order Modal */}
      {showOrderModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md">
            <div className="p-6 border-b border-gray-200">
              <div className="flex items-center justify-between">
                <h2>Create Order</h2>
                <button
                  onClick={() => setShowOrderModal(false)}
                  className="p-1 text-gray-400 hover:text-gray-600 transition-colors"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>
            </div>

            <div className="p-6 space-y-6">
              {/* Payment Status */}
              <div>
                <label className="block text-sm text-gray-700 mb-3">Payment Status</label>
                <div className="flex gap-3">
                  <button
                    onClick={() => setOrderPaymentStatus('paid')}
                    className={`flex-1 py-3 px-4 rounded-lg border-2 transition-colors ${orderPaymentStatus === 'paid'
                      ? 'border-green-600 bg-green-50 text-green-900'
                      : 'border-gray-200 text-gray-700 hover:border-gray-300'
                      }`}
                  >
                    Paid
                  </button>
                  <button
                    onClick={() => setOrderPaymentStatus('unpaid')}
                    className={`flex-1 py-3 px-4 rounded-lg border-2 transition-colors ${orderPaymentStatus === 'unpaid'
                      ? 'border-orange-600 bg-orange-50 text-orange-900'
                      : 'border-gray-200 text-gray-700 hover:border-gray-300'
                      }`}
                  >
                    Unpaid
                  </button>
                </div>
              </div>

              {/* Order Notes */}
              <div>
                <label className="block text-sm text-gray-700 mb-2">Order Notes (Optional)</label>
                <textarea
                  value={orderNotes}
                  onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => updateActiveTab({ orderNotes: e.target.value })}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"
                  placeholder="Add special instructions or notes..."
                  rows={4}
                />
              </div>

              {/* Order Summary */}
              <div className="p-4 bg-gray-50 rounded-lg space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Items</span>
                  <span className="text-gray-900">{cart.length}</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Total Amount</span>
                  <span className="text-blue-600">${total.toFixed(2)}</span>
                </div>
                {selectedCustomer && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Customer</span>
                    <span className="text-gray-900">{selectedCustomer.name}</span>
                  </div>
                )}
              </div>

              {/* Action Buttons */}
              <div className="flex gap-3">
                <button
                  onClick={() => setShowOrderModal(false)}
                  className="flex-1 py-3 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-50 transition-colors"
                >
                  Cancel
                </button>
                <button
                  onClick={handleSaveOrder}
                  className="flex-1 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                >
                  Create Order
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Discount Modal */}
      {showDiscountModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-2xl w-full max-w-md max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-gray-200">
              <div className="flex items-center justify-between">
                <h2>Apply Discount</h2>
                <button
                  onClick={() => setShowDiscountModal(false)}
                  className="p-1 text-gray-400 hover:text-gray-600 transition-colors"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>
            </div>

            <div className="p-6 space-y-6">
              {/* Standard Discounts */}
              <div>
                <h3 className="text-sm text-gray-700 mb-3">Standard Discounts</h3>
                <div className="space-y-2">
                  {STANDARD_DISCOUNTS.map(discount => (
                    <button
                      key={discount.id}
                      onClick={() => applyStandardDiscount(discount)}
                      className="w-full flex items-center justify-between p-3 border border-gray-200 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition-colors text-left"
                    >
                      <div className="flex items-center gap-3">
                        <div className="p-2 bg-blue-100 rounded-lg">
                          <Tag className="w-4 h-4 text-blue-600" />
                        </div>
                        <span className="text-gray-900">{discount.name}</span>
                      </div>
                      <span className="text-blue-600">
                        {discount.type === 'percentage'
                          ? `${discount.value}%`
                          : `$${discount.value.toFixed(2)}`}
                      </span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Custom Discount */}
              <div>
                <h3 className="text-sm text-gray-700 mb-3">Custom Discount</h3>
                <div className="space-y-3">
                  <div className="flex gap-2">
                    <button
                      onClick={() => setCustomDiscountType('percentage')}
                      className={`flex-1 py-2 px-4 rounded-lg transition-colors ${customDiscountType === 'percentage'
                        ? 'bg-blue-600 text-white'
                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                    >
                      Percentage
                    </button>
                    <button
                      onClick={() => setCustomDiscountType('fixed')}
                      className={`flex-1 py-2 px-4 rounded-lg transition-colors ${customDiscountType === 'fixed'
                        ? 'bg-blue-600 text-white'
                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                    >
                      Fixed Amount
                    </button>
                  </div>
                  <div className="relative">
                    <input
                      type="number"
                      placeholder={customDiscountType === 'percentage' ? 'Enter percentage' : 'Enter amount'}
                      value={customDiscountValue}
                      onChange={(e) => setCustomDiscountValue(e.target.value)}
                      className="w-full px-4 py-3 pr-12 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                      min="0"
                      step="0.01"
                    />
                    <span className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-500">
                      {customDiscountType === 'percentage' ? '%' : '$'}
                    </span>
                  </div>
                  <button
                    onClick={applyCustomDiscount}
                    className="w-full py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    Apply Custom Discount
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Payment Modal */}
      {showPaymentModal && (
        <PaymentModal
          total={total}
          onClose={() => setShowPaymentModal(false)}
          onComplete={handlePaymentComplete}
        />
      )}
    </div>
  );
}
