import { clearAuthSession, getAuthToken, getCurrentUser, getRefreshToken, saveAuthSession } from "@/lib/storage";

let cachedGatewayBase = null;
let gatewayBasePromise = null;

function normalizeBaseUrl(value) {
	return typeof value === "string" ? value.replace(/\/$/, "") : "";
}

function coerceToGatewayBase(value) {
	const normalized = normalizeBaseUrl(value);
	if (!normalized) return "";

	// In local dev, the API must go through the gateway (8080) because it owns JWT validation
	// and applies consistent routing/CORS. If someone configured a service URL (8081-8084),
	// automatically rewrite it to the gateway.
	try {
		const url = new URL(normalized);
		const isLocalhost = url.hostname === "localhost" || url.hostname === "127.0.0.1";
		const servicePorts = new Set(["8081", "8082", "8083", "8084"]);
		if (isLocalhost && servicePorts.has(url.port)) {
			url.port = "8080";
		}
		// Drop any accidental path (we expect a base like http://host:port)
		url.pathname = "";
		url.search = "";
		url.hash = "";
		return url.toString().replace(/\/$/, "");
	} catch {
		return normalized;
	}
}

async function resolveGatewayBase() {
	if (cachedGatewayBase) {
		return cachedGatewayBase;
	}

	if (typeof window === "undefined") {
		cachedGatewayBase =
			coerceToGatewayBase(process.env.NEXT_PUBLIC_API_URL || process.env.NEXT_PUBLIC_API_BASE_URL) ||
			"http://localhost:8080";
		return cachedGatewayBase;
	}

	if (!gatewayBasePromise) {
		gatewayBasePromise = fetch("/api/runtime-config", {
			cache: "no-store",
		})
			.then(async (response) => {
				if (!response.ok) {
					return {};
				}

				return response.json();
			})
			.catch(() => ({}));
	}

	const config = await gatewayBasePromise;
	cachedGatewayBase =
		coerceToGatewayBase(config?.apiBaseUrl) ||
		coerceToGatewayBase(process.env.NEXT_PUBLIC_API_URL) ||
		coerceToGatewayBase(process.env.NEXT_PUBLIC_API_BASE_URL) ||
		"http://localhost:8080";
	return cachedGatewayBase;
}

export async function apiRequest(path, options = {}) {
	const gatewayBase = await resolveGatewayBase();
	return performRequest(gatewayBase, path, options, true);
}

async function performRequest(gatewayBase, path, options = {}, canRetryAuth = true) {
	const token = getAuthToken();
	const headers = {
		"Content-Type": "application/json",
		...(token ? { Authorization: `Bearer ${token}` } : {}),
		...(options.headers || {}),
	};

	let response;
	try {
		response = await fetch(`${gatewayBase}${path}`, {
			...options,
			headers,
			cache: "no-store",
		});
	} catch {
		throw new Error("Failed to fetch");
	}

	let payload = null;
	try {
		const text = await response.text();
		payload = text ? JSON.parse(text) : null;
	} catch {
		payload = null;
	}

	const isAuthLifecycleCall = ["/auth/login", "/auth/register", "/auth/refresh"].includes(path);
	if (response.status === 401 && canRetryAuth && !isAuthLifecycleCall) {
		const refreshed = await tryRefreshAccessToken(gatewayBase);
		if (refreshed) {
			return performRequest(gatewayBase, path, options, false);
		}
	}

	if (!response.ok) {
		const message = payload?.message || payload?.error || "Request failed";
		const error = new Error(message);
		error.status = response.status;
		error.payload = payload;
		error.path = path;

		throw error;
	}

	return payload ?? {};
}

async function tryRefreshAccessToken(gatewayBase) {
	const refreshToken = getRefreshToken();
	if (!refreshToken) {
		return false;
	}

	try {
		const response = await fetch(`${gatewayBase}/auth/refresh`, {
			method: "POST",
			headers: {
				"Content-Type": "application/json",
			},
			body: JSON.stringify({ refreshToken }),
			cache: "no-store",
		});

		if (!response.ok) {
			clearAuthSession();
			return false;
		}

		const data = await response.json();
		const nextToken = data?.token || data?.accessToken;
		if (!nextToken) {
			clearAuthSession();
			return false;
		}

		const currentUser = getCurrentUser();
		saveAuthSession({
			token: nextToken,
			refreshToken: data?.refreshToken || refreshToken,
			user: {
				userId: data?.userId || currentUser?.userId || currentUser?.id,
				id: data?.userId || currentUser?.id || currentUser?.userId,
				email: data?.email || currentUser?.email,
				role: data?.role || currentUser?.role,
			},
		});

		return true;
	} catch {
		clearAuthSession();
		return false;
	}
}
