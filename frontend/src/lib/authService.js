import { apiRequest } from "@/lib/apiClient";
import { clearAuthSession, getRefreshToken, saveAuthSession } from "@/lib/storage";

export async function registerUser(formData) {
	const payload = {
		email: formData.email,
		password: formData.password,
		fullName: formData.fullName,
	};

	return apiRequest("/auth/register", {
		method: "POST",
		body: JSON.stringify(payload),
	});
}

export async function loginUser(credentials) {
	const payload = {
		email: credentials.email,
		password: credentials.password,
	};

	const data = await apiRequest("/auth/login", {
		method: "POST",
		body: JSON.stringify(payload),
	});

	const token = data?.token || data?.accessToken;
	if (!token) {
		throw new Error("Login succeeded but no access token was returned");
	}
	const user = {
		userId: data?.userId,
		id: data?.userId,
		email: data?.email || credentials?.email,
		role: data?.role || "CUSTOMER",
	};

	saveAuthSession({
		token,
		refreshToken: data?.refreshToken,
		user,
	});

	return { ...data, user, token };
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
	return apiRequest("/auth/users/me", {
		method: "GET",
	});
}

export async function updateMyProfile(payload) {
	return apiRequest("/auth/users/profile", {
		method: "PUT",
		body: JSON.stringify(payload),
	});
}

export async function getAllUsers() {
	return apiRequest("/admin/users", {
		method: "GET",
	});
}

export async function updateUserStatus(id, active) {
	return apiRequest(`/admin/users/${id}/status`, {
		method: "PATCH",
		body: JSON.stringify({ active }),
	});
}
