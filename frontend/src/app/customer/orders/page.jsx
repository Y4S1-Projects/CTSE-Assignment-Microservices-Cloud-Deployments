"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Card from "@/components/common/Card";
import Button from "@/components/common/Button";
import OrdersTable from "@/components/food/OrdersTable";
import { apiRequest } from "@/lib/apiClient";
import { validateToken } from "@/lib/authService";
import { getAuthToken, getCurrentUser, getOrderHistory } from "@/lib/storage";

function mergeOrders(primaryOrders, secondaryOrders) {
	const merged = [];
	const seenKeys = new Set();

	for (const order of [...(primaryOrders || []), ...(secondaryOrders || [])]) {
		if (!order) continue;
		const key =
			order.id ||
			`${order.userId || ""}-${order.totalAmount || ""}-${Array.isArray(order.items) ? order.items.length : order.items || ""}`;
		if (seenKeys.has(key)) continue;
		seenKeys.add(key);
		merged.push(order);
	}

	return merged;
}

function getLocalOrdersForUser(userId) {
	const localOrders = getOrderHistory();
	if (!userId) return localOrders;
	return localOrders.filter((order) => !order?.userId || order.userId === userId);
}

export default function CustomerOrdersPage() {
	const router = useRouter();
	const [orders, setOrders] = useState([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState("");

	useEffect(() => {
		if (!getAuthToken()) {
			router.replace("/auth/login");
			return;
		}

		async function loadOrders() {
			setLoading(true);
			setError("");

			const currentUser = getCurrentUser();
			const currentUserId = currentUser?.id || currentUser?.userId;

			try {
				const token = getAuthToken();
				if (token) {
					await validateToken(token);
				}

				const remoteOrders = await apiRequest("/orders/my", { method: "GET" });
				setOrders(mergeOrders(Array.isArray(remoteOrders) ? remoteOrders : [], getLocalOrdersForUser(currentUserId)));
			} catch (loadError) {
				const localOrders = getLocalOrdersForUser(currentUserId);
				setOrders(localOrders);
				setError(loadError.message || "Failed to load your orders.");
			} finally {
				setLoading(false);
			}
		}

		loadOrders();
	}, [router]);

	return (
		<div className='space-y-6'>
			<Card className='space-y-4'>
				<div className='flex flex-wrap items-center justify-between gap-3'>
					<div>
						<h1 className='text-2xl font-bold text-slate-900'>My Orders</h1>
						<p className='text-sm text-slate-600'>Review your recent orders and open any order for details.</p>
					</div>
					<div className='flex flex-wrap gap-2'>
						<Link href='/customer'>
							<Button variant='secondary'>Back to dashboard</Button>
						</Link>
						<Link href='/customer/checkout'>
							<Button>Place new order</Button>
						</Link>
					</div>
				</div>
				{error ?
					<p className='text-sm text-amber-700'>{error}</p>
				:	null}
			</Card>

			{loading ?
				<Card>
					<p className='text-sm text-slate-600'>Loading your orders...</p>
				</Card>
			:	null}

			<OrdersTable orders={orders} title='My Orders' />
		</div>
	);
}
