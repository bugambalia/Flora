// Frontend service layer to communicate with the Rust/Cloudflare Worker backend

const API_URL = '/api/dispatch';

export interface BackendResponse<T = any> {
    status: 'success' | 'error';
    data?: T;
    message?: string;
}

async function apiCall<T>(action: string, payload: any = {}): Promise<BackendResponse<T>> {
    try {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                type_action: action,
                ...payload,
            }),
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        return await response.json();
    } catch (error) {
        console.error(`Error calling ${action}:`, error);
        return {
            status: 'error',
            message: error instanceof Error ? error.message : 'Unknown error',
        };
    }
}

export const posService = {
    login: (credentials: any) => apiCall('LOGIN', credentials),
    getProducts: () => apiCall('GET_PRODUCTS'),
    getCustomers: () => apiCall('GET_CUSTOMERS'),
    createSale: (saleData: any) => apiCall('CREATE_SALE', { sale: saleData }),
    getInventory: () => apiCall('GET_INVENTORY'),
    // Add more handlers as they are implemented in the backend
};
