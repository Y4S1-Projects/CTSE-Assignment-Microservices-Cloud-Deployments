"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Button from "@/components/common/Button";
import Card from "@/components/common/Card";
import Input from "@/components/common/Input";
import { getAllOrders, updateOrderStatus } from "@/lib/foodService";
import { getAuthToken, isAdminUser } from "@/lib/storage";

function formatPrice(value) {
	return `$${Number(value || 0).toFixed(2)}`;
}

export default function AdminOrdersPage() {
	const router = useRouter();
	const [orders, setOrders] = useState([]);
	const [error, setError] = useState("");
	const [success, setSuccess] = useState("");
	const [authorized, setAuthorized] = useState(false);
	const [orderId, setOrderId] = useState("");
	const [orderStatus, setOrderStatus] = useState("PREPARING");

	async function loadOrders() {
		setError("");
		try {
			const ordersData = await getAllOrders().catch(() => []);
			setOrders(Array.isArray(ordersData) ? ordersData : []);
		} catch (loadError) {
			setError(loadError.message || "Failed to load orders");
		}
	}

	useEffect(() => {
		if (!getAuthToken()) {
			router.replace("/auth/login");
			return;
		}
		if (!isAdminUser()) {
			router.replace("/customer");
			return;
		}
		setAuthorized(true);
		loadOrders();
	}, [router]);

	async function handleOrderStatus(event) {
		event.preventDefault();
		setError("");
		setSuccess("");
		try {
			await updateOrderStatus(orderId, orderStatus);
			setSuccess(`Order ${orderId} updated to ${orderStatus}`);
			setOrderId("");
			await loadOrders();
		} catch (err) {
			setError(err.message || "Failed to update order status");
		}
	}

	if (!authorized) {
		return (
			<Card className='max-w-xl mx-auto mt-10'>
				<p className='text-sm text-slate-600'>Checking admin access...</p>
			</Card>
		);
	}

	return (
		<div className='space-y-6'>
			{/* Header */}
			<Card className='space-y-2'>
				<h1 className='text-2xl font-bold text-slate-900'>Order Management</h1>
				<p className='text-sm text-slate-600'>View and manage all customer orders, track status and delivery</p>
				{error ?
					<p className='text-sm text-red-600'>{error}</p>
				:	null}
				{success ?
					<p className='text-sm text-green-700'>{success}</p>
				:	null}
			</Card>

			{/* Orders Table */}
			<Card className='space-y-4'>
				<div className='flex items-center justify-between'>
					<h2 className='text-lg font-semibold text-slate-900'>All Orders</h2>
					<Button variant='secondary' onClick={loadOrders}>
						Refresh
					</Button>
				</div>

				{orders.length === 0 ?
					<p className='text-sm text-slate-500'>No orders yet.</p>
				:	<div className='overflow-x-auto'>
						<table className='min-w-full text-sm text-left'>
							<thead>
								<tr className='border-b border-brand-100 text-brand-800'>
									<th className='py-2 pr-4'>Reference</th>
									<th className='py-2 pr-4'>Item ID</th>
									<th className='py-2 pr-4'>Item Name</th>
									<th className='py-2 pr-4'>Qty</th>
									<th className='py-2 pr-4'>Amount</th>
									<th className='py-2 pr-4'>Method</th>
									<th className='py-2 pr-4'>Status</th>
									<th className='py-2 pr-4'>Checkout</th>
									<th className='py-2 pr-4'>Date</th>
								</tr>
							</thead>
							<tbody>
								{orders.map((order) => (
									<tr key={order.id} className='border-b border-brand-50 text-slate-700 hover:bg-brand-50'>
										<td className='py-2 pr-4 font-mono text-xs'>{order.reference || order.id?.slice(0, 8)}</td>
										<td className='py-2 pr-4 font-mono text-xs'>{order.itemId}</td>
										<td className='py-2 pr-4'>{order.itemName || "—"}</td>
										<td className='py-2 pr-4'>{order.quantity}</td>
										<td className='py-2 pr-4 font-semibold'>{formatPrice(order.amount)}</td>
										<td className='py-2 pr-4'>{order.paymentMethod || "—"}</td>
										<td className='py-2 pr-4'>
											<span
												className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
													order.status === "COMPLETED" ? "bg-green-100 text-green-700"
													: order.status === "FAILED" ? "bg-red-100 text-red-700"
													: order.status === "CANCELLED" ? "bg-slate-100 text-slate-700"
													: "bg-yellow-100 text-yellow-700"
												}`}>
												{order.status}
											</span>
										</td>
										<td className='py-2 pr-4'>
											<span
												className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
													order.isSuccessCheckout ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-600"
												}`}>
												{order.isSuccessCheckout ? "✓ Confirmed" : "Pending"}
											</span>
										</td>
										<td className='py-2 pr-4 text-xs text-slate-500'>
											{order.createdAt ? new Date(order.createdAt).toLocaleDateString() : "—"}
										</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				}
			</Card>

			{/* Update Order Status */}
			<Card className='space-y-4'>
				<h2 className='text-lg font-semibold text-slate-900'>Update Order Status</h2>
				<form className='grid gap-3 sm:grid-cols-3' onSubmit={handleOrderStatus}>
					<Input
						label='Order ID'
						value={orderId}
						onChange={(event) => setOrderId(event.target.value)}
						placeholder='Enter order ID...'
						required
					/>
					<label className='block space-y-1.5'>
						<span className='text-sm font-medium text-slate-700'>New Status</span>
						<select
							value={orderStatus}
							onChange={(event) => setOrderStatus(event.target.value)}
							className='w-full rounded-xl border border-brand-200 bg-white px-3.5 py-2.5 text-slate-900 outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-200'>
							<option value='PREPARING'>PREPARING</option>
							<option value='READY'>READY</option>
							<option value='DELIVERED'>DELIVERED</option>
							<option value='CANCELLED'>CANCELLED</option>
						</select>
					</label>
					<div className='flex items-end'>
						<Button type='submit' className='w-full'>
							Update Status
						</Button>
					</div>
				</form>
			</Card>
		</div>
	);
}
