import { getAuthToken } from "@/lib/storage";

let cachedGatewayBase = null;
let gatewayBasePromise = null;

function normalizeBaseUrl(value) {
	return typeof value === "string" ? value.replace(/\/$/, "") : "";
}

async function resolveGatewayBase() {
	if (cachedGatewayBase) {
		return cachedGatewayBase;
	}

	if (typeof window === "undefined") {
		cachedGatewayBase = normalizeBaseUrl(process.env.NEXT_PUBLIC_API_URL || process.env.NEXT_PUBLIC_API_BASE_URL) || "http://localhost:8080";
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
		normalizeBaseUrl(config?.apiBaseUrl) ||
		normalizeBaseUrl(process.env.NEXT_PUBLIC_API_URL) ||
		normalizeBaseUrl(process.env.NEXT_PUBLIC_API_BASE_URL) ||
		"http://localhost:8080";
	return cachedGatewayBase;
}

export async function apiRequest(path, options = {}) {
	const gatewayBase = await resolveGatewayBase();
	const token = getAuthToken();
	const headers = {
		"Content-Type": "application/json",
		...(token ? { Authorization: `Bearer ${token}` } : {}),
		...(options.headers || {}),
	};

	const response = await fetch(`${gatewayBase}${path}`, {
		...options,
		headers,
		cache: "no-store",
	});

	let payload = null;
	try {
		const text = await response.text();
		payload = text ? JSON.parse(text) : null;
	} catch {
		payload = null;
	}

	if (!response.ok) {
		const message = payload?.message || payload?.error || "Request failed";
		throw new Error(message);
	}

	return payload ?? {};
}
