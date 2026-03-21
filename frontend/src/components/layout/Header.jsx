"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { validateToken, logoutUser } from "@/lib/authService";
import Button from "@/components/common/Button";
import {
	clearAuthSession,
	getAuthToken,
	getCurrentUser,
	getRefreshToken,
	isAdminUser,
	isAuthenticated,
	saveAuthSession,
} from "@/lib/storage";

export default function Header() {
	const pathname = usePathname();
	const [authState, setAuthState] = useState({ ready: false, authenticated: false, admin: false, user: null });

	useEffect(() => {
		function syncAuthState() {
			setAuthState({
				ready: true,
				authenticated: isAuthenticated(),
				admin: isAdminUser(),
				user: getCurrentUser(),
			});
		}

		async function hydrateFromBackendSession() {
			const token = getAuthToken();
			if (!token) return;

			const existingUser = getCurrentUser();
			if (existingUser?.email && existingUser?.role) return;

			try {
				const validation = await validateToken(token);
				if (validation?.valid) {
					saveAuthSession({
						token,
						refreshToken: getRefreshToken(),
						user: {
							userId: validation.userId,
							id: validation.userId,
							email: validation.email,
							role: validation.role,
						},
					});
				}
			} catch (error) {
				if (error?.status === 401) {
					clearAuthSession();
				}
			}

			syncAuthState();
		}

		syncAuthState();
		hydrateFromBackendSession();
		window.addEventListener("storage", syncAuthState);
		window.addEventListener("focus", syncAuthState);
		window.addEventListener("auth-session-changed", syncAuthState);

		return () => {
			window.removeEventListener("storage", syncAuthState);
			window.removeEventListener("focus", syncAuthState);
			window.removeEventListener("auth-session-changed", syncAuthState);
		};
	}, []);

	const navItems = [{ href: "/", label: "Home" }];

	if (authState.authenticated) {
		navItems.push({ href: authState.admin ? "/admin" : "/customer", label: authState.admin ? "Admin" : "Order Food" });
		navItems.push({ href: "/profile", label: "Profile" });
	} else {
		navItems.push({ href: "/auth/login", label: "Login" });
		navItems.push({ href: "/auth/register", label: "Register" });
	}

	async function handleLogout() {
		await logoutUser();
		setAuthState({ ready: true, authenticated: false, admin: false, user: null });
		window.location.href = "/auth/login";
	}

	return (
		<header className='sticky top-0 z-50 border-b border-brand-100 bg-white/95 backdrop-blur'>
			<div className='flex items-center justify-between w-full px-4 py-4 mx-auto max-w-7xl sm:px-6 lg:px-8'>
				<Link href='/' className='text-xl font-bold text-brand-700'>
					GreenBite Food Ordering
				</Link>
				<div className='flex items-center gap-2'>
					{authState.ready && authState.authenticated ?
						<p className='hidden text-sm text-slate-600 md:block'>
							Signed in as {authState.user?.email || (authState.admin ? "admin@local.test" : "customer")}
						</p>
					:	null}
					<nav className='flex items-center gap-1 sm:gap-2'>
						{navItems.map((item) => {
							const isActive = pathname === item.href;
							return (
								<Link
									key={item.href}
									href={item.href}
									className={`rounded-lg px-3 py-2 text-sm font-medium transition ${
										isActive ? "bg-brand-600 text-white" : "text-slate-700 hover:bg-brand-100 hover:text-brand-700"
									}`}>
									{item.label}
								</Link>
							);
						})}
					</nav>
					{authState.ready && authState.authenticated ?
						<Button variant='ghost' className='hidden sm:inline-flex' onClick={handleLogout}>
							Logout
						</Button>
					:	null}
				</div>
			</div>
		</header>
	);
}
