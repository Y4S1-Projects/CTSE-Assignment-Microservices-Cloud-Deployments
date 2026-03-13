"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import Button from "@/components/common/Button";
import { logoutUser } from "@/lib/authService";
import { getCurrentUser, isAdminUser, isAuthenticated } from "@/lib/storage";

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

		syncAuthState();
		window.addEventListener("storage", syncAuthState);
		window.addEventListener("focus", syncAuthState);

		return () => {
			window.removeEventListener("storage", syncAuthState);
			window.removeEventListener("focus", syncAuthState);
		};
	}, []);

	const navItems = [{ href: "/", label: "Home" }];

	if (authState.authenticated) {
		navItems.push({ href: authState.admin ? "/admin" : "/customer", label: authState.admin ? "Admin" : "Order Food" });
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
			<div className='mx-auto flex w-full max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8'>
				<Link href='/' className='text-xl font-bold text-brand-700'>
					GreenBite Food Ordering
				</Link>
				<div className='flex items-center gap-2'>
					{authState.ready && authState.authenticated ?
						<p className='hidden text-sm text-slate-600 md:block'>
							Signed in as {authState.user?.username || (authState.admin ? "admin" : "customer")}
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
