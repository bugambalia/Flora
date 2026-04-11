import React from 'react';
import { Plus, Minus, Trash2 } from 'lucide-react';
import { Product } from './ProductCard';

export interface CartItemType extends Product {
  quantity: number;
}

interface CartItemProps {
  item: CartItemType;
  onUpdateQuantity: (id: string, quantity: number) => void;
  onUpdatePrice: (id: string, price: number) => void;
  onRemove: (id: string) => void;
}

export function CartItem({ item, onUpdateQuantity, onUpdatePrice, onRemove }: CartItemProps) {
  const handlePriceClick = () => {
    const newPrice = prompt(`Enter new price for ${item.name}:`, item.price.toString());
    if (newPrice !== null) {
      const parsed = parseFloat(newPrice);
      if (!isNaN(parsed) && parsed >= 0) {
        onUpdatePrice(item.id, parsed);
      }
    }
  };

  return (
    <div className="flex items-center gap-3 p-3 bg-gray-50 rounded-lg">
      <div className="flex-1 min-w-0">
        <h4 className="truncate mb-1">{item.name}</h4>
        <button
          onClick={handlePriceClick}
          className="text-gray-600 hover:text-blue-600 hover:underline cursor-pointer"
          title="Click to override price"
        >
          ${item.price.toFixed(2)}
        </button>
      </div>

      <div className="flex items-center gap-2">
        <button
          onClick={() => onUpdateQuantity(item.id, item.quantity - 1)}
          className="w-8 h-8 rounded-md bg-white border border-gray-300 flex items-center justify-center hover:bg-gray-100"
        >
          <Minus className="w-4 h-4" />
        </button>
        <span className="w-8 text-center">{item.quantity}</span>
        <button
          onClick={() => onUpdateQuantity(item.id, item.quantity + 1)}
          className="w-8 h-8 rounded-md bg-white border border-gray-300 flex items-center justify-center hover:bg-gray-100"
        >
          <Plus className="w-4 h-4" />
        </button>
      </div>

      <button
        onClick={() => onRemove(item.id)}
        className="w-8 h-8 rounded-md bg-red-50 border border-red-200 flex items-center justify-center hover:bg-red-100"
      >
        <Trash2 className="w-4 h-4 text-red-600" />
      </button>

      <div className="w-20 text-right">
        ${(item.price * item.quantity).toFixed(2)}
      </div>
    </div>
  );
}
