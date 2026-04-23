import { apiRequest } from "@/lib/apiClient";
import { clearAuthSession, getAuthToken, getRefreshToken, saveAuthSession } from "@/lib/storage";
import { notifyAlert } from "@/lib/alerts";

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

	try {
		const data = await apiRequest("/auth/register", {
			method: "POST",
			body: JSON.stringify(payload),
		});
		return data;
	} catch (error) {
		throw error;
	}
}

export async function loginUser(credentials) {
	const payload = {
		email: credentials.email,
		password: credentials.password,
	};

	try {
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
	} catch (error) {
		throw error;
	}
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
	try {
		return await apiRequest("/auth/users/me", {
			method: "GET",
		});
	} catch (error) {
		notifyAlert({
			variant: "error",
			title: "Profile unavailable",
			message: error?.message || "We could not load your profile.",
		});
		throw error;
	}
}

export async function updateMyProfile(payload) {
	ensureSession();
	try {
		const data = await apiRequest("/auth/users/profile", {
			method: "PUT",
			body: JSON.stringify(payload),
		});
		notifyAlert({
			variant: "success",
			title: "Profile updated",
			message: "Your profile changes were saved.",
		});
		return data;
	} catch (error) {
		notifyAlert({
			variant: "error",
			title: "Profile update failed",
			message: error?.message || "We could not save your profile.",
		});
		throw error;
	}
}

export async function getAllUsers() {
	ensureSession();
	try {
		return await apiRequest("/auth/admin/users", {
			method: "GET",
		});
	} catch (error) {
		notifyAlert({
			variant: "error",
			title: "Users unavailable",
			message: error?.message || "We could not load the user list.",
		});
		throw error;
	}
}

export async function updateUserStatus(id, active) {
	ensureSession();
	try {
		const data = await apiRequest(`/auth/admin/users/${id}/status`, {
			method: "PATCH",
			body: JSON.stringify({ active }),
		});
		notifyAlert({
			variant: "success",
			title: "User status updated",
			message: active ? "The account was activated." : "The account was deactivated.",
		});
		return data;
	} catch (error) {
		notifyAlert({
			variant: "error",
			title: "User status update failed",
			message: error?.message || "We could not change the user status.",
		});
		throw error;
	}
}

export async function updateUserDetails(id, payload) {
	ensureSession();
	try {
		const data = await apiRequest(`/auth/admin/users/${id}`, {
			method: "PUT",
			body: JSON.stringify(payload),
		});
		notifyAlert({
			variant: "success",
			title: "User updated",
			message: `${payload?.email || "The user"} was saved successfully.`,
		});
		return data;
	} catch (error) {
		notifyAlert({
			variant: "error",
			title: "User update failed",
			message: error?.message || "We could not save the user details.",
		});
		throw error;
	}
}
