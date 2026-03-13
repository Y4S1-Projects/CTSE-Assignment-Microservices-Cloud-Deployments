"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import Button from "@/components/common/Button";
import Card from "@/components/common/Card";
import { isAdminUser, isAuthenticated } from "@/lib/storage";

export default function HeroSection() {
	const [authState, setAuthState] = useState({ ready: false, authenticated: false, admin: false });

	useEffect(() => {
		setAuthState({
			ready: true,
			authenticated: isAuthenticated(),
			admin: isAdminUser(),
		});
	}, []);

	return (
		<section className='grid gap-8 lg:grid-cols-2 lg:items-center'>
			<div className='space-y-5'>
				<p className='inline-flex rounded-full bg-brand-100 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-brand-800'>
					Food Ordering Platform
				</p>
				<h1 className='text-4xl font-bold leading-tight text-slate-900 sm:text-5xl'>
					Order fresh meals with secure auth, cart, checkout, and order tracking.
				</h1>
				<p className='max-w-xl text-slate-600'>
					Browse menu items, add to cart, place orders, process payments, and manage users or order status from the
					admin dashboard.
				</p>
				<div className='flex flex-wrap items-center gap-3'>
					{authState.ready && authState.admin ?
						<Link href='/admin'>
							<Button>Open Admin Panel</Button>
						</Link>
					: authState.ready && authState.authenticated ?
						<Link href='/customer'>
							<Button>Continue Ordering</Button>
						</Link>
					:	<>
							<Link href='/auth/register'>
								<Button>Create Account</Button>
							</Link>
							<Link href='/auth/login'>
								<Button variant='secondary'>Sign In</Button>
							</Link>
						</>
					}
				</div>
			</div>
			<Card className='space-y-4'>
				<Image
					src='/images/hero-illustration.svg'
					alt='Modern auth platform'
					width={520}
					height={320}
					className='h-auto w-full rounded-xl border border-brand-100'
					priority
				/>
				<h2 className='text-xl font-semibold text-slate-900'>Quick Navigation</h2>
				<ul className='space-y-2 text-sm text-slate-700'>
					<li>Customer side: menu discovery, cart management, checkout</li>
					<li>Order handling: order creation and payment processing</li>
					<li>Admin side: users management, menu availability, order status</li>
				</ul>
				<p className='rounded-xl bg-brand-50 p-3 text-sm text-brand-800'>
					Auth is integrated into the ordering flow and routed via API Gateway on port 8080.
				</p>
			</Card>
		</section>
	);
}
