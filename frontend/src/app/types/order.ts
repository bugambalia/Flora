import { CartItemType } from '../components/CartItem';

export type OrderStatus = 'pending' | 'ready' | 'completed' | 'cancelled';
export type PaymentStatus = 'unpaid' | 'paid';

export interface OrderCustomer {
  id: string;
  name: string;
  email: string;
  phone?: string;
  nitCedula: string;
}

export interface OrderDiscount {
  id: string;
  name: string;
  type: 'percentage' | 'fixed';
  value: number;
}

export interface Order {
  id: string;
  orderNumber: string;
  customer: OrderCustomer | null;
  items: CartItemType[];
  subtotal: number;
  discount: OrderDiscount | null;
  discountAmount: number;
  tax: number;
  total: number;
  paymentStatus: PaymentStatus;
  orderStatus: OrderStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

// Global storage for orders (in a real app, this would be in a state management solution or backend)
let ordersStorage: Order[] = [
  {
    id: '1',
    orderNumber: 'ORD-001',
    customer: { id: '1', name: 'John Smith', email: 'john.smith@email.com', phone: '(555) 123-4567', nitCedula: '123456789' },
    items: [
      { id: '1', name: 'Espresso', price: 3.50, category: 'Beverages', quantity: 2, image: '' },
      { id: '3', name: 'Croissant', price: 4.25, category: 'Food', quantity: 1, image: '' },
    ],
    subtotal: 11.25,
    discount: { id: '1', name: 'Member Discount', type: 'percentage', value: 20 },
    discountAmount: 2.25,
    tax: 0.72,
    total: 9.72,
    paymentStatus: 'paid',
    orderStatus: 'completed',
    notes: 'Extra sugar',
    createdAt: '2026-02-24T08:30:00',
    updatedAt: '2026-02-24T08:45:00',
  },
  {
    id: '2',
    orderNumber: 'ORD-002',
    customer: { id: '2', name: 'Maria Garcia', email: 'maria.garcia@email.com', phone: '(555) 234-5678', nitCedula: '987654321' },
    items: [
      { id: '2', name: 'Club Sandwich', price: 8.99, category: 'Food', quantity: 2, image: '' },
      { id: '4', name: 'Orange Juice', price: 4.50, category: 'Beverages', quantity: 2, image: '' },
    ],
    subtotal: 26.98,
    discount: null,
    discountAmount: 0,
    tax: 2.16,
    total: 29.14,
    paymentStatus: 'unpaid',
    orderStatus: 'ready',
    createdAt: '2026-02-24T09:15:00',
    updatedAt: '2026-02-24T09:30:00',
  },
  {
    id: '3',
    orderNumber: 'ORD-003',
    customer: null,
    items: [
      { id: '6', name: 'Cheeseburger', price: 12.50, category: 'Food', quantity: 1, image: '' },
      { id: '7', name: 'Cappuccino', price: 4.50, category: 'Beverages', quantity: 1, image: '' },
    ],
    subtotal: 17.00,
    discount: { id: 'custom', name: 'Custom Discount', type: 'fixed', value: 3 },
    discountAmount: 3.00,
    tax: 1.12,
    total: 15.12,
    paymentStatus: 'paid',
    orderStatus: 'pending',
    notes: 'No onions',
    createdAt: '2026-02-24T10:00:00',
    updatedAt: '2026-02-24T10:00:00',
  },
];

export const getOrders = (): Order[] => {
  return [...ordersStorage];
};

export const addOrder = (order: Order): void => {
  ordersStorage = [order, ...ordersStorage];
};

export const updateOrder = (id: string, updates: Partial<Order>): void => {
  ordersStorage = ordersStorage.map(order =>
    order.id === id ? { ...order, ...updates, updatedAt: new Date().toISOString() } : order
  );
};

export const deleteOrder = (id: string): void => {
  ordersStorage = ordersStorage.filter(order => order.id !== id);
};

export const getNextOrderNumber = (): string => {
  const maxNumber = ordersStorage.reduce((max, order) => {
    const num = parseInt(order.orderNumber.split('-')[1]);
    return num > max ? num : max;
  }, 0);
  return `ORD-${String(maxNumber + 1).padStart(3, '0')}`;
};
