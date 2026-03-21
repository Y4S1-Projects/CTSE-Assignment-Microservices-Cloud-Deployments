import { apiRequest } from "@/lib/apiClient";
import { clearAuthSession, getAuthToken, getRefreshToken, saveAuthSession } from "@/lib/storage";

function ensureSession() {
	const token = getAuthToken();
	if (!token) {
		const error = new Error("Unauthorized: active session required");
		error.status = 401;
		throw error;
	}
	return token;
}

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
	const sessionToken = token || ensureSession();
	return apiRequest("/auth/validate", {
		method: "POST",
		headers: sessionToken ? { Authorization: `Bearer ${sessionToken}` } : {},
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
	ensureSession();
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
	ensureSession();
	return apiRequest("/auth/users/me", {
		method: "GET",
	});
}

export async function updateMyProfile(payload) {
	ensureSession();
	return apiRequest("/auth/users/profile", {
		method: "PUT",
		body: JSON.stringify(payload),
	});
}

export async function getAllUsers() {
	ensureSession();
	return apiRequest("/auth/admin/users", {
		method: "GET",
	});
}

export async function updateUserStatus(id, active) {
	ensureSession();
	return apiRequest(`/auth/admin/users/${id}/status`, {
		method: "PATCH",
		body: JSON.stringify({ active }),
	});
}

export async function updateUserDetails(id, payload) {
	ensureSession();
	return apiRequest(`/auth/admin/users/${id}`, {
		method: "PUT",
		body: JSON.stringify(payload),
	});
}
