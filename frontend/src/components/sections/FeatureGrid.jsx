"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { getMenuItems } from "@/lib/foodService";
import Card from "@/components/common/Card";
import Button from "@/components/common/Button";
import { isAuthenticated } from "@/lib/storage";

export default function FeatureGrid() {
	const [items, setItems] = useState([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState("");
	const [selectedCategory, setSelectedCategory] = useState("All");
	const isAuth = isAuthenticated();

	useEffect(() => {
		async function loadItems() {
			try {
				setLoading(true);
				const menuItems = await getMenuItems();
				setItems(Array.isArray(menuItems) ? menuItems : []);
				setError("");
			} catch (err) {
				console.error("Failed to load menu items:", err);
				setError("Unable to load menu items");
				setItems([]);
			} finally {
				setLoading(false);
			}
		}
		loadItems();
	}, []);

	// Get unique categories
	const categories = ["All", ...new Set(items.map((item) => item.category || "Other"))];

	// Filter items by category
	const filteredItems = selectedCategory === "All" ? items : items.filter((item) => item.category === selectedCategory);

	return (
		<section className='space-y-8'>
			{/* Category Filter */}
			<div className='space-y-4'>
				<h2 className='text-2xl font-bold text-slate-900'>What are you craving?</h2>
				<div className='flex gap-2 overflow-x-auto pb-2'>
					{categories.map((category) => (
						<button
							key={category}
							onClick={() => setSelectedCategory(category)}
							className={`px-4 py-2 rounded-full whitespace-nowrap font-medium transition ${
								selectedCategory === category ? "bg-brand-600 text-white" : (
									"bg-slate-100 text-slate-700 hover:bg-slate-200"
								)
							}`}>
							{category}
						</button>
					))}
				</div>
			</div>

			{/* Items Grid */}
			{loading ?
				<div className='text-center py-12'>
					<p className='text-slate-600'>Loading delicious options...</p>
				</div>
			: error ?
				<div className='bg-red-50 border border-red-200 rounded-lg p-4 text-red-700'>
					<p>{error}</p>
				</div>
			: filteredItems.length === 0 ?
				<div className='text-center py-12'>
					<p className='text-slate-600'>No items available in this category</p>
				</div>
			:	<div className='grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4'>
					{filteredItems.map((item) => (
						<Link key={item.id} href={isAuth ? `/customer?item=${item.id}` : "/auth/login"} className='group'>
							<Card className='h-full overflow-hidden hover:shadow-lg transition cursor-pointer space-y-3'>
								{/* Item Image */}
								<div className='relative w-full h-40 bg-linear-to-br from-slate-100 to-slate-200 rounded-lg overflow-hidden flex items-center justify-center group-hover:scale-105 transition transform'>
									{item.imageUrl ?
										<img src={item.imageUrl} alt={item.name} className='w-full h-full object-cover' />
									:	<div className='text-3xl'>Food</div>}
								</div>

								{/* Item Info */}
								<div className='space-y-2 p-0'>
									<div>
										<h3 className='font-semibold text-slate-900 line-clamp-2'>{item.name}</h3>
										<p className='text-xs text-slate-500'>{item.category || "Food"}</p>
									</div>

									{/* Description */}
									<p className='text-sm text-slate-600 line-clamp-2'>{item.description || "Delicious food item"}</p>

									{/* Price & Stock */}
									<div className='flex items-center justify-between pt-2'>
										<span className='font-bold text-brand-600'>${Number(item.price || 0).toFixed(2)}</span>
										{item.stockCount > 0 ?
											<span className='text-xs bg-green-100 text-green-700 px-2 py-1 rounded'>Available</span>
										:	<span className='text-xs bg-red-100 text-red-700 px-2 py-1 rounded'>Out of Stock</span>}
									</div>
								</div>
							</Card>
						</Link>
					))}
				</div>
			}

			{/* CTA for unauthenticated users */}
			{!isAuth && items.length > 0 && (
				<div className='mt-12 bg-linear-to-r from-brand-600 to-brand-700 rounded-2xl p-8 text-center text-white space-y-4'>
					<h3 className='text-2xl font-bold'>Ready to Order?</h3>
					<p className='max-w-xl mx-auto'>
						Create your account to start ordering delicious meals and get fast delivery to your door.
					</p>
					<div className='flex flex-wrap gap-3 justify-center'>
						<Link href='/auth/register'>
							<Button size='lg' className='bg-white text-brand-600 hover:bg-slate-100'>
								Create Account
							</Button>
						</Link>
						<Link href='/auth/login'>
							<Button size='lg' variant='secondary'>
								Sign In
							</Button>
						</Link>
					</div>
				</div>
			)}
		</section>
	);
}
