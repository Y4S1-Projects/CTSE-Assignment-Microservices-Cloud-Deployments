import { apiRequest } from "@/lib/apiClient";

// ---------------------------------------------------------------------------
// Fallback catalog data (used when catalog service is unavailable)
// ---------------------------------------------------------------------------
const fallbackMenu = [
	{
		id: "item-1",
		itemId: "ITEM-0001",
		name: "Green Garden Salad",
		description: "Fresh lettuce, cucumber, avocado, and light herb dressing",
		price: 8.5,
		category: "Salads",
		stockCount: 20,
		available: true,
		imageUrl: "https://images.unsplash.com/photo-1546793665-c74683f339c1?w=800",
	},
	{
		id: "item-2",
		itemId: "ITEM-0002",
		name: "Chicken Rice Bowl",
		description: "Grilled chicken, jasmine rice, and sautéed vegetables",
		price: 12.0,
		category: "Main",
		stockCount: 15,
		available: true,
		imageUrl: "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=800",
	},
	{
		id: "item-3",
		itemId: "ITEM-0003",
		name: "Pasta Primavera",
		description: "Penne pasta with seasonal veggies and basil sauce",
		price: 11.25,
		category: "Pasta",
		stockCount: 10,
		available: true,
		imageUrl: "https://images.unsplash.com/photo-1473093295043-cdd812d0e601?w=800",
	},
	{
		id: "item-4",
		itemId: "ITEM-0004",
		name: "Matcha Smoothie",
		description: "Green tea smoothie with banana and almond milk",
		price: 6.75,
		category: "Drinks",
		stockCount: 5,
		available: true,
		imageUrl: "https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=800",
	},
];

// ---------------------------------------------------------------------------
// Catalog Service
// ---------------------------------------------------------------------------

export async function getMenuItems() {
	try {
		const data = await apiRequest("/catalog/items", { method: "GET" });
		if (Array.isArray(data) && data.length > 0) return data;
	} catch {
		// fall through to fallback
	}
	return fallbackMenu;
}

export async function getMenuItemById(id) {
	return apiRequest(`/catalog/items/${id}`, { method: "GET" });
}

export async function getMenuByCategory(category) {
	try {
		const data = await apiRequest(`/catalog/items/category/${encodeURIComponent(category)}`, { method: "GET" });
		if (Array.isArray(data) && data.length > 0) return data;
	} catch {
		// fall through
	}
	const all = await getMenuItems();
	return all.filter((item) => (item.category || "").toLowerCase() === category.toLowerCase());
}

export async function getCatalogCategories() {
	try {
		return await apiRequest("/catalog/categories", { method: "GET" });
	} catch {
		return [...new Set(fallbackMenu.map((i) => i.category))];
	}
}

/** Admin — create new catalog item */
export async function createCatalogItem(data) {
	return apiRequest("/catalog/items", {
		method: "POST",
		body: JSON.stringify(data),
	});
}

/** Admin — update existing item (full or partial) */
export async function updateCatalogItem(id, data) {
	return apiRequest(`/catalog/items/${id}`, {
		method: "PUT",
		body: JSON.stringify(data),
	});
}

/** Admin — set or adjust stock count.  Pass { stockCount } or { delta } */
export async function updateCatalogStock(id, stockData) {
	return apiRequest(`/catalog/items/${id}/stock`, {
		method: "PATCH",
		body: JSON.stringify(stockData),
	});
}

/** Admin — delete item */
export async function deleteCatalogItem(id) {
	return apiRequest(`/catalog/items/${id}`, { method: "DELETE" });
}

/** Admin — dashboard stats */
export async function getCatalogDashboard() {
	return apiRequest("/catalog/dashboard", { method: "GET" });
}

// ---------------------------------------------------------------------------
// Payment / Checkout Service
// ---------------------------------------------------------------------------

/**
 * Process checkout.
 * Saves payment record + decrements catalog stock in real-time.
 * @param {{ itemId, userId, quantity, amount, paymentMethod }} data
 */
export async function checkout(data) {
	return apiRequest("/payments/checkout", {
		method: "POST",
		body: JSON.stringify(data),
	});
}

/** Admin / all orders list */
export async function getAllOrders() {
	try {
		const data = await apiRequest("/payments/orders", { method: "GET" });
		return Array.isArray(data) ? data : [];
	} catch {
		return [];
	}
}

export async function getPaymentById(id) {
	return apiRequest(`/payments/${id}`, { method: "GET" });
}

// ---------------------------------------------------------------------------
// Legacy / Order Service helpers (kept for backward compat)
// ---------------------------------------------------------------------------

export async function createOrder(items) {
	const payload = { items: items.map((item) => ({ itemId: item.id, quantity: item.quantity })) };
	return apiRequest("/orders", { method: "POST", body: JSON.stringify(payload) });
}

export async function getMyOrders() {
	try {
		const data = await apiRequest("/orders/my", { method: "GET" });
		return Array.isArray(data) ? data : [];
	} catch {
		return [];
	}
}

export async function updateOrderStatus(id, status) {
	return apiRequest(`/orders/${id}/status?status=${encodeURIComponent(status)}`, { method: "PATCH" });
}

export async function processPayment(orderId, amount) {
	return apiRequest("/payments/checkout", {
		method: "POST",
		body: JSON.stringify({ orderId, amount }),
	});
}

export async function updateMenuAvailability(id, availability) {
	return apiRequest(`/catalog/items/${id}/availability?availability=${encodeURIComponent(availability)}`, { method: "PATCH" });
}

export async function getPaymentByOrderId(orderId) {
	return apiRequest(`/payments/order/${orderId}`, { method: "GET" });
}

