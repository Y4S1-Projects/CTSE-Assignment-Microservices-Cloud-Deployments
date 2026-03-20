"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Button from "@/components/common/Button";
import Card from "@/components/common/Card";
import MenuItemCard from "@/components/food/MenuItemCard";
import CartSummary from "@/components/food/CartSummary";
import OrdersTable from "@/components/food/OrdersTable";
import { getMyProfile, logoutUser, validateToken } from "@/lib/authService";
import { checkout, createOrder, getMenuItems, getMyOrders } from "@/lib/foodService";
import {
	clearCart,
	getAuthToken,
	getCart,
	getCurrentUser,
	isAdminUser,
	saveCart,
} from "@/lib/storage";

function formatPrice(value) {
	const numeric = Number(value || 0);
	return `$${numeric.toFixed(2)}`;
}

export default function CustomerPage() {
	const router = useRouter();
	const [profile, setProfile] = useState(null);
	const [menuItems, setMenuItems] = useState([]);
	const [orders, setOrders] = useState([]);
	const [cart, setCart] = useState([]);
	const [category, setCategory] = useState("ALL");
	const [placingOrder, setPlacingOrder] = useState(false);
	const [error, setError] = useState("");
	const [success, setSuccess] = useState("");
	const [authorized, setAuthorized] = useState(false);

	useEffect(() => {
		setCart(getCart());
	}, []);

	useEffect(() => {
		if (!getAuthToken()) {
			router.replace("/auth/login");
			return;
		}

		if (isAdminUser()) {
			router.replace("/admin");
			return;
		}

		setAuthorized(true);

		async function loadInitial() {
			try {
				// If token is stale/invalid, clear it and force re-login.
				const token = getAuthToken();
				if (token) {
					await validateToken(token);
				}
				const [profileData, menuData, orderData] = await Promise.all([
					getMyProfile().catch(() => null),
					getMenuItems(),
					getMyOrders().catch(() => []),
				]);
				// Fall back to locally stored user if profile API is unavailable
				setProfile(profileData || getCurrentUser());
				setMenuItems(menuData);
				if (orderData.length > 0) {
					setOrders(orderData);
				}
			} catch (loadError) {
				const msg = loadError.message || "Failed to load customer data";
				setError(msg);
				if (msg.toLowerCase().includes("invalid or expired jwt token") || msg.toLowerCase().includes("missing or invalid authorization")) {
					router.replace("/auth/login");
				}
			}
		}

		loadInitial();
	}, [router]);

	const categories = useMemo(() => {
		const set = new Set(menuItems.map((item) => item.category).filter(Boolean));
		return ["ALL", ...Array.from(set)];
	}, [menuItems]);

	const filteredItems = useMemo(() => {
		if (category === "ALL") return menuItems;
		return menuItems.filter((item) => (item.category || "").toLowerCase() === category.toLowerCase());
	}, [menuItems, category]);

	function persistCart(nextCart) {
		setCart(nextCart);
		saveCart(nextCart);
	}

	function addToCart(item) {
		setSuccess("");
		setError("");
		if (item.stockCount === 0 || item.available === false) {
			setError(`"${item.name}" is out of stock.`);
			return;
		}
		const existing = cart.find((cartItem) => cartItem.id === item.id);
		if (existing) {
			persistCart(
				cart.map((cartItem) => (cartItem.id === item.id ? { ...cartItem, quantity: cartItem.quantity + 1 } : cartItem)),
			);
			return;
		}
		persistCart([...cart, { ...item, quantity: 1 }]);
	}

	function changeQuantity(itemId, delta) {
		const next = cart
			.map((item) => (item.id === itemId ? { ...item, quantity: item.quantity + delta } : item))
			.filter((item) => item.quantity > 0);
		persistCart(next);
	}

	function removeFromCart(itemId) {
		persistCart(cart.filter((item) => item.id !== itemId));
	}

	async function handleCheckout() {
		if (cart.length === 0) return;
		setPlacingOrder(true);
		setError("");
		setSuccess("");

		const userId = profile?.id || "guest";

		try {
			// 1) Create an order in order-service (validates items with catalog-service)
			const created = await createOrder(cart);

			// 2) Pay each line item (payment-service decrements stock and marks order as PAID)
			for (const orderItem of created?.items || []) {
				await checkout({
					itemId: orderItem.itemId || orderItem.catalogItemId,
					orderId: created.id,
					userId,
					quantity: orderItem.quantity,
					amount: orderItem.lineTotal,
					paymentMethod: "CARD",
				});
			}

			// Refresh server-side order list
			const refreshedOrders = await getMyOrders().catch(() => []);
			if (refreshedOrders.length > 0) {
				setOrders(refreshedOrders);
			} else {
				setOrders([created, ...orders]);
			}
			clearCart();
			setCart([]);
			// Refresh catalog items to show updated stock counts
			const refreshed = await getMenuItems();
			setMenuItems(refreshed);
			setSuccess(`Checkout successful! Order ${created?.id || ""} paid. Stock updated in real-time.`);
		} catch (checkoutError) {
			setError(checkoutError.message || "Failed to complete checkout");
		} finally {
			setPlacingOrder(false);
		}
	}

	async function handleLogout() {
		await logoutUser();
		window.location.href = "/auth/login";
	}

	if (!authorized) {
		return (
			<Card className='mx-auto mt-10 max-w-xl'>
				<p className='text-sm text-slate-600'>Checking customer access...</p>
			</Card>
		);
	}

	return (
		<div className='space-y-6'>
			<Card className='space-y-3'>
				<div className='flex flex-wrap items-center justify-between gap-3'>
					<div>
						<h1 className='text-2xl font-bold text-slate-900'>Food Ordering Dashboard</h1>
						<p className='text-sm text-slate-600'>Welcome {profile?.fullName || profile?.username || "Customer"}</p>
					</div>
					<Button onClick={handleLogout}>Logout</Button>
				</div>
				{error ?
					<p className='text-sm text-red-600'>{error}</p>
				:	null}
				{success ?
					<p className='text-sm text-green-700'>{success}</p>
				:	null}
			</Card>

			<section className='grid gap-6 xl:grid-cols-[2fr_1fr]'>
				<div className='space-y-4'>
					<Card className='space-y-4'>
						<div className='flex flex-wrap items-center justify-between gap-3'>
							<h2 className='text-xl font-semibold text-slate-900'>Menu</h2>
							<div className='flex flex-wrap gap-2'>
								{categories.map((itemCategory) => (
									<button
										key={itemCategory}
										type='button'
										onClick={() => setCategory(itemCategory)}
										className={`rounded-full px-3 py-1 text-xs font-semibold transition ${
											category === itemCategory ? "bg-brand-600 text-white" : (
												"bg-brand-100 text-brand-800 hover:bg-brand-200"
											)
										}`}>
										{itemCategory}
									</button>
								))}
							</div>
						</div>
						{filteredItems.length === 0 ?
							<p className='text-sm text-slate-500'>No menu items available.</p>
						:	<div className='grid gap-4 sm:grid-cols-2'>
								{filteredItems.map((item) => (
									<MenuItemCard key={item.id} item={item} onAdd={addToCart} />
								))}
							</div>
						}
					</Card>
					<OrdersTable orders={orders} title='My Orders' />
				</div>

				<div className='space-y-4'>
					<CartSummary
						items={cart}
						onChangeQty={changeQuantity}
						onRemove={removeFromCart}
						onCheckout={handleCheckout}
						loading={placingOrder}
					/>
					<Card>
						<h3 className='text-lg font-semibold text-slate-900'>Payment Summary</h3>
						<p className='mt-2 text-sm text-slate-600'>
							Total payable: {formatPrice(cart.reduce((sum, item) => sum + Number(item.price || 0) * item.quantity, 0))}
						</p>
						<p className='mt-1 text-xs text-slate-500'>
							Checkout deducts real-time stock from the catalog inventory.
						</p>
					</Card>
				</div>
			</section>
		</div>
	);
}
