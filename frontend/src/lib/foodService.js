import { apiRequest } from "@/lib/apiClient";

const fallbackMenu = [
	{
		id: "item-1",
		name: "Green Garden Salad",
		description: "Fresh lettuce, cucumber, avocado, and light herb dressing",
		price: 8.5,
		availability: "AVAILABLE",
		category: "Salads",
		imageUrl: "https://images.unsplash.com/photo-1546793665-c74683f339c1?w=800",
	},
	{
		id: "item-2",
		name: "Chicken Rice Bowl",
		description: "Grilled chicken, jasmine rice, and sautéed vegetables",
		price: 12.0,
		availability: "AVAILABLE",
		category: "Main",
		imageUrl: "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=800",
	},
	{
		id: "item-3",
		name: "Pasta Primavera",
		description: "Penne pasta with seasonal veggies and basil sauce",
		price: 11.25,
		availability: "AVAILABLE",
		category: "Pasta",
		imageUrl: "https://images.unsplash.com/photo-1473093295043-cdd812d0e601?w=800",
	},
	{
		id: "item-4",
		name: "Matcha Smoothie",
		description: "Green tea smoothie with banana and almond milk",
		price: 6.75,
		availability: "AVAILABLE",
		category: "Drinks",
		imageUrl: "https://images.unsplash.com/photo-1505252585461-04db1eb84625?w=800",
	},
];

export async function getMenuItems() {
	const data = await apiRequest("/catalog/items", { method: "GET" });
	if (Array.isArray(data) && data.length > 0) return data;
	return fallbackMenu;
}

export async function getMenuItemById(id) {
	return apiRequest(`/catalog/items/${id}`, { method: "GET" });
}

export async function getMenuByCategory(category) {
	const data = await apiRequest(`/catalog/items/category/${encodeURIComponent(category)}`, {
		method: "GET",
	});
	if (Array.isArray(data) && data.length > 0) return data;
	const all = await getMenuItems();
	return all.filter((item) => (item.category || "").toLowerCase() === category.toLowerCase());
}

export async function updateMenuAvailability(id, availability) {
	return apiRequest(`/catalog/items/${id}/availability?availability=${encodeURIComponent(availability)}`, {
		method: "PATCH",
	});
}

export async function createOrder(items) {
	const payload = {
		items: items.map((item) => ({
			itemId: item.id,
			quantity: item.quantity,
		})),
	};
	return apiRequest("/orders", {
		method: "POST",
		body: JSON.stringify(payload),
	});
}

export async function getMyOrders() {
	const data = await apiRequest("/orders/my", { method: "GET" });
	return Array.isArray(data) ? data : [];
}

export async function updateOrderStatus(id, status) {
	return apiRequest(`/orders/${id}/status?status=${encodeURIComponent(status)}`, {
		method: "PATCH",
	});
}

export async function processPayment(orderId, amount) {
	return apiRequest("/payments/charge", {
		method: "POST",
		body: JSON.stringify({ orderId, amount }),
	});
}

export async function getPaymentByOrderId(orderId) {
	return apiRequest(`/payments/${orderId}`, { method: "GET" });
}
