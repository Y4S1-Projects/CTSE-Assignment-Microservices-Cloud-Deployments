const TOKEN_KEY = "frontend_access_token";
const REFRESH_KEY = "frontend_refresh_token";
const USER_KEY = "frontend_user";
const CART_KEY = "frontend_cart";
const ORDER_HISTORY_KEY = "frontend_order_history";
const AUTH_SESSION_EVENT = "auth-session-changed";

function getSessionStore() {
	if (typeof window === "undefined") return null;
	return window.sessionStorage;
}

function getLocalStore() {
	if (typeof window === "undefined") return null;
	return window.localStorage;
}

function notifyAuthSessionChanged() {
	if (typeof window === "undefined") return;
	window.dispatchEvent(new Event(AUTH_SESSION_EVENT));
}

export function saveAuthSession(session) {
	const sessionStore = getSessionStore();
	if (!sessionStore) return;
	if (session?.token) sessionStore.setItem(TOKEN_KEY, session.token);
	if (session?.refreshToken) sessionStore.setItem(REFRESH_KEY, session.refreshToken);
	if (session?.user) sessionStore.setItem(USER_KEY, JSON.stringify(session.user));
	notifyAuthSessionChanged();
}

export function clearAuthSession() {
	const sessionStore = getSessionStore();
	const localStore = getLocalStore();
	if (!sessionStore && !localStore) return;
	sessionStore?.removeItem(TOKEN_KEY);
	sessionStore?.removeItem(REFRESH_KEY);
	sessionStore?.removeItem(USER_KEY);
	// Cleanup previous storage strategy as part of migration.
	localStore?.removeItem(TOKEN_KEY);
	localStore?.removeItem(REFRESH_KEY);
	localStore?.removeItem(USER_KEY);
	notifyAuthSessionChanged();
}

export function getAuthToken() {
	const sessionStore = getSessionStore();
	const localStore = getLocalStore();
	if (!sessionStore && !localStore) return null;
	return sessionStore?.getItem(TOKEN_KEY) || localStore?.getItem(TOKEN_KEY);
}

export function getRefreshToken() {
	const sessionStore = getSessionStore();
	const localStore = getLocalStore();
	if (!sessionStore && !localStore) return null;
	return sessionStore?.getItem(REFRESH_KEY) || localStore?.getItem(REFRESH_KEY);
}

export function getCurrentUser() {
	const sessionStore = getSessionStore();
	const localStore = getLocalStore();
	if (!sessionStore && !localStore) return null;
	const raw = sessionStore?.getItem(USER_KEY) || localStore?.getItem(USER_KEY);
	if (!raw) return null;
	try {
		return JSON.parse(raw);
	} catch {
		sessionStore?.removeItem(USER_KEY);
		localStore?.removeItem(USER_KEY);
		return null;
	}
}

export function isAuthenticated() {
	return Boolean(getAuthToken());
}

export function isAdminUser() {
	const user = getCurrentUser();
	if (!user) return false;
	const role = user.role;
	// Support numeric role (1 = admin) and string variants
	if (role === 1 || role === "1") return true;
	const roleStr = String(role || "").toUpperCase();
	return roleStr === "ADMIN" || roleStr === "ROLE_ADMIN";
}

export function getCart() {
	const localStore = getLocalStore();
	if (!localStore) return [];
	const raw = localStore.getItem(CART_KEY);
	return raw ? JSON.parse(raw) : [];
}

export function saveCart(items) {
	const localStore = getLocalStore();
	if (!localStore) return;
	localStore.setItem(CART_KEY, JSON.stringify(items || []));
}

export function clearCart() {
	const localStore = getLocalStore();
	if (!localStore) return;
	localStore.removeItem(CART_KEY);
}

export function getOrderHistory() {
	const localStore = getLocalStore();
	if (!localStore) return [];
	const raw = localStore.getItem(ORDER_HISTORY_KEY);
	return raw ? JSON.parse(raw) : [];
}

export function pushOrderHistory(order) {
	const localStore = getLocalStore();
	if (!localStore) return;
	const current = getOrderHistory();
	const next = [order, ...current].slice(0, 20);
	localStore.setItem(ORDER_HISTORY_KEY, JSON.stringify(next));
}
