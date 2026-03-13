const TOKEN_KEY = "frontend_access_token";
const REFRESH_KEY = "frontend_refresh_token";
const USER_KEY = "frontend_user";

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
