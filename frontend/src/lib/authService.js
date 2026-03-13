import { apiRequest } from "@/lib/apiClient";
import { clearAuthSession, getRefreshToken, saveAuthSession } from "@/lib/storage";

export async function registerUser(formData) {
	return apiRequest("/auth/register", {
		method: "POST",
		body: JSON.stringify(formData),
	});
}

export async function loginUser(credentials) {
	const data = await apiRequest("/auth/login", {
		method: "POST",
		body: JSON.stringify(credentials),
	});

	saveAuthSession({
		token: data?.token,
		refreshToken: data?.refreshToken,
		user: data?.user || { username: credentials?.username },
	});

	return data;
}

export async function validateToken(token) {
	return apiRequest("/auth/validate", {
		method: "POST",
		headers: token ? { Authorization: `Bearer ${token}` } : {},
	});
}

export async function refreshToken() {
	const refresh = getRefreshToken();
	return apiRequest("/auth/refresh", {
		method: "POST",
		body: JSON.stringify({ refreshToken: refresh }),
	});
}

export async function logoutUser() {
	const refresh = getRefreshToken();
	try {
		await apiRequest("/auth/logout", {
			method: "POST",
			body: JSON.stringify({ refreshToken: refresh }),
		});
	} finally {
		clearAuthSession();
	}
}

export async function getMyProfile() {
	return apiRequest("/users/me", {
		method: "GET",
	});
}

export async function getAllUsers() {
	return apiRequest("/admin/users", {
		method: "GET",
	});
}
