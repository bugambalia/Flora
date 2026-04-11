import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import { get, set, del } from 'idb-keyval';

// Custom storage for IndexedDB persistence
const storage = {
    getItem: async (name: string) => (await get(name)) || null,
    setItem: async (name: string, value: any) => await set(name, value),
    removeItem: async (name: string) => await del(name),
};

export interface SaleItem {
    id: string;
    producto_id: number;
    nombre: string;
    cantidad: number;
    precio_aplicado: number;
    precio_original: number;
}

export interface ActiveSale {
    id: string;
    cliente_id: number;
    items: SaleItem[];
    total: number;
}

interface SaleState {
    activeSales: ActiveSale[];
    currentSaleId: string | null;
    addSale: () => void;
    removeSale: (id: string) => void;
    setCurrentSale: (id: string) => void;
    addItemToCurrentSale: (item: Omit<SaleItem, 'id'>) => void;
    updateItemQuantity: (saleId: string, itemId: string, quantity: number) => void;
    updateItemPrice: (saleId: string, itemId: string, price: number) => void;
    clearCurrentSale: () => void;
}

export const useSaleStore = create<SaleState>()(
    persist(
        (set) => ({
            activeSales: [
                { id: '1', cliente_id: 1, items: [], total: 0 } // Consumidor Final default
            ],
            currentSaleId: '1',

            addSale: () => set((state) => {
                if (state.activeSales.length >= 5) return state; // Limit to 5 sales
                const newId = Math.random().toString(36).substr(2, 9);
                return {
                    activeSales: [...state.activeSales, { id: newId, cliente_id: 1, items: [], total: 0 }],
                    currentSaleId: newId
                };
            }),

            removeSale: (id) => set((state) => {
                const remaining = state.activeSales.filter(s => s.id !== id);
                if (remaining.length === 0) {
                    const defaultId = '1';
                    return {
                        activeSales: [{ id: defaultId, cliente_id: 1, items: [], total: 0 }],
                        currentSaleId: defaultId
                    };
                }
                return {
                    activeSales: remaining,
                    currentSaleId: state.currentSaleId === id ? remaining[0].id : state.currentSaleId
                };
            }),

            setCurrentSale: (id) => set({ currentSaleId: id }),

            addItemToCurrentSale: (item) => set((state) => {
                const activeSales = state.activeSales.map(sale => {
                    if (sale.id === state.currentSaleId) {
                        const newItem = { ...item, id: Math.random().toString(36).substr(2, 9) };
                        const updatedItems = [...sale.items, newItem];
                        const updatedTotal = updatedItems.reduce((acc, i) => acc + (i.precio_aplicado * i.cantidad), 0);
                        return { ...sale, items: updatedItems, total: updatedTotal };
                    }
                    return sale;
                });
                return { activeSales };
            }),

            updateItemQuantity: (saleId, itemId, quantity) => set((state) => {
                const activeSales = state.activeSales.map(sale => {
                    if (sale.id === saleId) {
                        const updatedItems = sale.items.map(i => i.id === itemId ? { ...i, cantidad: quantity } : i);
                        const updatedTotal = updatedItems.reduce((acc, i) => acc + (i.precio_aplicado * i.cantidad), 0);
                        return { ...sale, items: updatedItems, total: updatedTotal };
                    }
                    return sale;
                });
                return { activeSales };
            }),

            updateItemPrice: (saleId, itemId, price) => set((state) => {
                const activeSales = state.activeSales.map(sale => {
                    if (sale.id === saleId) {
                        const updatedItems = sale.items.map(i => i.id === itemId ? { ...i, precio_aplicado: price } : i);
                        const updatedTotal = updatedItems.reduce((acc, i) => acc + (i.precio_aplicado * i.cantidad), 0);
                        return { ...sale, items: updatedItems, total: updatedTotal };
                    }
                    return sale;
                });
                return { activeSales };
            }),

            clearCurrentSale: () => set((state) => {
                const activeSales = state.activeSales.map(sale => {
                    if (sale.id === state.currentSaleId) {
                        return { ...sale, items: [], total: 0 };
                    }
                    return sale;
                });
                return { activeSales };
            })
        }),
        {
            name: 'pos-sales-storage',
            storage: createJSONStorage(() => storage),
        }
    )
);
