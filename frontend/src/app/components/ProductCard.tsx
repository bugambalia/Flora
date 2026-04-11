import React from 'react';

export interface Product {
  id: string;
  name: string;
  price: number;
  category: string;
  image: string;
}

interface ProductCardProps {
  product: Product;
  onAddToCart: (product: Product) => void;
}

export function ProductCard({ product, onAddToCart }: ProductCardProps) {
  return (
    <button
      onClick={() => onAddToCart(product)}
      className="bg-white rounded-lg p-4 shadow-sm hover:shadow-md transition-shadow border border-gray-200 text-left w-full"
    >
      <div className="aspect-square bg-gray-100 rounded-lg mb-3 overflow-hidden">
        <img 
          src={product.image} 
          alt={product.name}
          className="w-full h-full object-cover"
        />
      </div>
      <h3 className="mb-1 truncate">{product.name}</h3>
      <p className="text-blue-600">${product.price.toFixed(2)}</p>
    </button>
  );
}
