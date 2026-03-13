import { getAuthToken } from "@/lib/storage";

const gatewayBase = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export async function apiRequest(path, options = {}) {
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
