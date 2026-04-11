import React, { useState } from 'react';
import { CreditCard, X } from 'lucide-react';

interface PaymentModalProps {
  total: number;
  onClose: () => void;
  onComplete: () => void;
}

export function PaymentModal({ total, onClose, onComplete }: PaymentModalProps) {
  const [paymentMethod, setPaymentMethod] = useState<'cash' | 'card'>('card');
  const [cashAmount, setCashAmount] = useState('');

  const handlePayment = () => {
    // Simulate payment processing
    setTimeout(() => {
      onComplete();
    }, 500);
  };

  const change = paymentMethod === 'cash' ? Math.max(0, parseFloat(cashAmount || '0') - total) : 0;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-md">
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-xl">Payment</h2>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-lg hover:bg-gray-100 flex items-center justify-center"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6">
          <div className="bg-blue-50 rounded-lg p-4 mb-6">
            <p className="text-gray-600 mb-1">Total Amount</p>
            <p className="text-3xl text-blue-600">${total.toFixed(2)}</p>
          </div>

          <div className="space-y-4 mb-6">
            <label className="block">
              <input
                type="radio"
                name="payment"
                value="card"
                checked={paymentMethod === 'card'}
                onChange={(e) => setPaymentMethod(e.target.value as 'card')}
                className="mr-3"
              />
              <span>Credit/Debit Card</span>
            </label>
            <label className="block">
              <input
                type="radio"
                name="payment"
                value="cash"
                checked={paymentMethod === 'cash'}
                onChange={(e) => setPaymentMethod(e.target.value as 'cash')}
                className="mr-3"
              />
              <span>Cash</span>
            </label>
          </div>

          {paymentMethod === 'cash' && (
            <div className="space-y-4 mb-6">
              <div>
                <label className="block text-sm text-gray-600 mb-2">Cash Received</label>
                <input
                  type="number"
                  value={cashAmount}
                  onChange={(e) => setCashAmount(e.target.value)}
                  placeholder="0.00"
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                  step="0.01"
                />
              </div>
              {cashAmount && parseFloat(cashAmount) >= total && (
                <div className="bg-green-50 rounded-lg p-3">
                  <p className="text-sm text-gray-600">Change</p>
                  <p className="text-xl text-green-600">${change.toFixed(2)}</p>
                </div>
              )}
            </div>
          )}

          <button
            onClick={handlePayment}
            disabled={paymentMethod === 'cash' && (parseFloat(cashAmount || '0') < total)}
            className="w-full py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed flex items-center justify-center gap-2"
          >
            <CreditCard className="w-5 h-5" />
            Complete Payment
          </button>
        </div>
      </div>
    </div>
  );
}
