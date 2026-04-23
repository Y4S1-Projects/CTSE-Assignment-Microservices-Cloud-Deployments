"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Button from "@/components/common/Button";
import { isAuthenticated } from "@/lib/storage";

export default function HeroSection() {
	const router = useRouter();
	const [searchQuery, setSearchQuery] = useState("");
	const isAuth = isAuthenticated();

	const handleSearch = (e) => {
		e.preventDefault();
		if (searchQuery.trim()) {
			// Search functionality can be implemented later
			console.log("Search for:", searchQuery);
		}
	};

	return (
		<section className='relative bg-linear-to-br from-brand-50 to-brand-100 rounded-2xl overflow-hidden'>
			<div className='max-w-6xl mx-auto px-6 py-16 md:py-24'>
				<div className='grid md:grid-cols-2 gap-12 items-center'>
					{/* Left Content */}
					<div className='space-y-6'>
						<div className='space-y-3'>
							<h1 className='text-5xl md:text-6xl font-bold text-slate-900 leading-tight'>Order Fresh Food Online</h1>
							<p className='text-xl text-slate-600'>
								Fast delivery. Fresh ingredients. Your favorite meals, right to your door.
							</p>
						</div>

						{/* Search Bar */}
						<form onSubmit={handleSearch} className='flex gap-2'>
							<input
								type='text'
								placeholder='Search for restaurants, cuisines...'
								value={searchQuery}
								onChange={(e) => setSearchQuery(e.target.value)}
								className='flex-1 px-4 py-3 rounded-lg border border-slate-300 focus:outline-none focus:ring-2 focus:ring-brand-600 focus:border-transparent'
							/>
							<Button type='submit'>Search</Button>
						</form>

						{/* CTA Buttons */}
						<div className='flex flex-wrap gap-3 pt-4'>
							{isAuth ?
								<Link href='/customer'>
									<Button size='lg' className='px-8'>
										Start Ordering
									</Button>
								</Link>
							:	<>
									<Link href='/auth/register'>
										<Button size='lg' className='px-8'>
											Sign Up
										</Button>
									</Link>
									<Link href='/auth/login'>
										<Button variant='secondary' size='lg' className='px-8'>
											Sign In
										</Button>
									</Link>
								</>
							}
						</div>

						{/* Trust Signals */}
						<div className='flex gap-8 text-sm text-slate-700 pt-4'>
							<div>
								<p className='font-semibold text-slate-900'>100%</p>
								<p>Fresh Ingredients</p>
							</div>
							<div>
								<p className='font-semibold text-slate-900'>30 min</p>
								<p>Fast Delivery</p>
							</div>
							<div>
								<p className='font-semibold text-slate-900'>24/7</p>
								<p>Available</p>
							</div>
						</div>
					</div>

					{/* Right Side - Hero Image */}
					<div className='hidden md:flex items-center justify-center'>
						<div className='relative w-full aspect-square'>
							<div className='absolute inset-0 bg-linear-to-br from-brand-400 to-brand-600 rounded-3xl opacity-20'></div>
							<div className='relative w-full h-full flex items-center justify-center'>
								<div className='text-center space-y-4'>
									<div className='text-6xl'>🥗</div>
									<p className='text-sm text-slate-600'>Premium Quality</p>
								</div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</section>
	);
}
