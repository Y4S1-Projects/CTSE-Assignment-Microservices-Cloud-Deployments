"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
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
	const router = useRouter();
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

	// Customer navigation items
	const customerNavItems = [
		{ href: "/customer", label: "Menus & Items" },
		{ href: "/customer/orders", label: "My Orders" },
		{ href: "/customer/checkout", label: "Cart" },
	];

	// Admin navigation items
	const adminNavItems = [
		{ href: "/admin/catalog", label: "Menus & Items" },
		{ href: "/admin/users", label: "Users" },
		{ href: "/admin/orders", label: "Orders" },
		{ href: "/admin/payments", label: "Payments" },
	];

	const activeNavItems =
		authState.authenticated ?
			authState.admin ?
				adminNavItems
			:	customerNavItems
		:	[];

	async function handleLogout() {
		await logoutUser();
		setAuthState({ ready: true, authenticated: false, admin: false, user: null });
		router.push("/auth/login");
	}

	return (
		<header className='sticky top-0 z-50 border-b border-brand-100 bg-white/95 backdrop-blur'>
			<div className='w-full px-4 py-4 mx-auto max-w-7xl sm:px-6 lg:px-8'>
				<div className='flex items-center justify-between gap-4'>
					{/* Logo */}
					<Link href='/' className='text-xl font-bold text-brand-700 whitespace-nowrap'>
						🍃 GreenBite
					</Link>

					{/* Center Navigation - Role-based */}
					{authState.ready && authState.authenticated && (
						<nav className='flex-1 flex items-center justify-center gap-1 sm:gap-3'>
							{activeNavItems.map((item) => {
								const isActive = pathname === item.href || pathname.startsWith(item.href + "/");
								return (
									<Link
										key={item.href}
										href={item.href}
										className={`rounded-lg px-3 py-2 text-sm font-medium transition whitespace-nowrap ${
											isActive ? "bg-brand-600 text-white" : "text-slate-700 hover:bg-brand-100 hover:text-brand-700"
										}`}>
										{item.label}
									</Link>
								);
							})}
						</nav>
					)}

					{/* Right Section - Auth Status & Buttons */}
					<div className='flex items-center gap-3 ml-auto'>
						{authState.ready ?
							authState.authenticated ?
								<>
									{/* Signed in as (hidden on mobile) */}
									<div className='hidden sm:flex items-center gap-2'>
										<span className='text-xs text-slate-500'>Signed in as:</span>
										<span className='text-sm font-medium text-slate-700'>
											{authState.user?.email || (authState.admin ? "Admin" : "Customer")}
										</span>
										<span
											className={`px-2 py-1 rounded text-xs font-semibold ${
												authState.admin ? "bg-purple-100 text-purple-700" : "bg-blue-100 text-blue-700"
											}`}>
											{authState.admin ? "Admin" : "Customer"}
										</span>
									</div>

									{/* Logout Button */}
									<Button variant='secondary' onClick={handleLogout} className='whitespace-nowrap'>
										Logout
									</Button>
								</>
							:	<>
									{/* Login and Register Buttons */}
									<Link href='/auth/login'>
										<Button variant='ghost' className='whitespace-nowrap'>
											Login
										</Button>
									</Link>
									<Link href='/auth/register'>
										<Button variant='primary' className='whitespace-nowrap hidden sm:inline-flex'>
											Sign Up
										</Button>
									</Link>
								</>

						:	null}
					</div>
				</div>

				{/* Mobile view - show role badge below on small screens */}
				{authState.ready && authState.authenticated && (
					<div className='sm:hidden mt-3 flex items-center justify-between text-xs'>
						<span className='text-slate-600'>{authState.user?.email || (authState.admin ? "Admin" : "Customer")}</span>
						<span
							className={`px-2 py-1 rounded font-semibold ${
								authState.admin ? "bg-purple-100 text-purple-700" : "bg-blue-100 text-blue-700"
							}`}>
							{authState.admin ? "Admin" : "Customer"}
						</span>
					</div>
				)}
			</div>
		</header>
	);
}
