"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const navItems = [
	{ href: "/", label: "Home" },
	{ href: "/auth/login", label: "Login" },
	{ href: "/auth/register", label: "Register" },
	{ href: "/customer", label: "Customer" },
	{ href: "/admin", label: "Admin" },
];

export default function Header() {
	const pathname = usePathname();

	return (
		<header className='sticky top-0 z-50 border-b border-brand-100 bg-white/95 backdrop-blur'>
			<div className='mx-auto flex w-full max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8'>
				<Link href='/' className='text-xl font-bold text-brand-700'>
					Frontend Service
				</Link>
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
			</div>
		</header>
	);
}
