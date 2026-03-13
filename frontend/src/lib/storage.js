const TOKEN_KEY = "frontend_access_token";
const REFRESH_KEY = "frontend_refresh_token";
const USER_KEY = "frontend_user";
const CART_KEY = "frontend_cart";
const ORDER_HISTORY_KEY = "frontend_order_history";

export function saveAuthSession(session) {
	if (typeof window === "undefined") return;
	if (session?.token) localStorage.setItem(TOKEN_KEY, session.token);
	if (session?.refreshToken) localStorage.setItem(REFRESH_KEY, session.refreshToken);
	if (session?.user) localStorage.setItem(USER_KEY, JSON.stringify(session.user));
}

export function clearAuthSession() {
	if (typeof window === "undefined") return;
	localStorage.removeItem(TOKEN_KEY);
	localStorage.removeItem(REFRESH_KEY);
	localStorage.removeItem(USER_KEY);
}

export function getAuthToken() {
	if (typeof window === "undefined") return null;
	return localStorage.getItem(TOKEN_KEY);
}

export function getRefreshToken() {
	if (typeof window === "undefined") return null;
	return localStorage.getItem(REFRESH_KEY);
}

export function getCurrentUser() {
	if (typeof window === "undefined") return null;
	const raw = localStorage.getItem(USER_KEY);
	return raw ? JSON.parse(raw) : null;
}

export function isAuthenticated() {
	return Boolean(getAuthToken());
}

export function isAdminUser() {
	const user = getCurrentUser();
	return (user?.role || "").toUpperCase() === "ADMIN";
}

export function getCart() {
	if (typeof window === "undefined") return [];
	const raw = localStorage.getItem(CART_KEY);
	return raw ? JSON.parse(raw) : [];
}

export function saveCart(items) {
	if (typeof window === "undefined") return;
	localStorage.setItem(CART_KEY, JSON.stringify(items || []));
}

export function clearCart() {
	if (typeof window === "undefined") return;
	localStorage.removeItem(CART_KEY);
}

export function getOrderHistory() {
	if (typeof window === "undefined") return [];
	const raw = localStorage.getItem(ORDER_HISTORY_KEY);
	return raw ? JSON.parse(raw) : [];
}

export function pushOrderHistory(order) {
	if (typeof window === "undefined") return;
	const current = getOrderHistory();
	const next = [order, ...current].slice(0, 20);
	localStorage.setItem(ORDER_HISTORY_KEY, JSON.stringify(next));
}
