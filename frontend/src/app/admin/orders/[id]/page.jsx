"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import Card from "@/components/common/Card";
import Button from "@/components/common/Button";
import { apiRequest } from "@/lib/apiClient";
import { getOrderById } from "@/lib/foodService";
import { getAuthToken, isAdminUser } from "@/lib/storage";

function formatPrice(value) {
	return `$${Number(value || 0).toFixed(2)}`;
}

function formatDate(value) {
	return value ? new Date(value).toLocaleString() : "—";
}

function statusClasses(status) {
	switch ((status || "").toUpperCase()) {
		case "PAID":
		case "COMPLETED":
			return "bg-green-100 text-green-700";
		case "FAILED":
		case "CANCELLED":
			return "bg-red-100 text-red-700";
		default:
			return "bg-yellow-100 text-yellow-700";
	}
}

export default function AdminOrderDetailsPage() {
	const router = useRouter();
	const params = useParams();
	const orderId = params?.id;

	const [order, setOrder] = useState(null);
	const [payments, setPayments] = useState([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState("");
	const [paymentsError, setPaymentsError] = useState("");

	useEffect(() => {
		if (!getAuthToken()) {
			router.replace("/auth/login");
			return;
		}
		if (!isAdminUser()) {
			router.replace("/customer");
			return;
		}
		if (!orderId) return;

		async function load() {
			setLoading(true);
			setError("");
			setPaymentsError("");

			try {
				const orderData = await getOrderById(orderId);
				setOrder(orderData);
			} catch (loadError) {
				setError(loadError.message || "Failed to load order details");
				setLoading(false);
				return;
			}

			try {
				const allPayments = await apiRequest("/payments/orders", { method: "GET" });
				const matchingPayments =
					Array.isArray(allPayments) ? allPayments.filter((payment) => payment.orderId === orderId) : [];
				setPayments(matchingPayments);
			} catch (paymentLoadError) {
				setPayments([]);
				setPaymentsError(paymentLoadError.message || "Failed to load payment records");
			}

			setLoading(false);
		}

		load();
	}, [orderId, router]);

	const totalItems = useMemo(
		() => (Array.isArray(order?.items) ? order.items.reduce((sum, item) => sum + Number(item.quantity || 0), 0) : 0),
		[order],
	);

	const paymentTotal = useMemo(
		() => payments.reduce((sum, payment) => sum + Number(payment.amount || 0), 0),
		[payments],
	);

	return (
		<div className='space-y-6'>
			<Card className='space-y-4'>
				<div className='flex flex-wrap items-center justify-between gap-3'>
					<div>
						<h1 className='text-2xl font-bold text-slate-900'>Order Details</h1>
						<p className='text-sm text-slate-600'>
							Full order contents and matching payment records for this checkout.
						</p>
					</div>
					<div className='flex flex-wrap gap-2'>
						<Link href='/admin/orders'>
							<Button variant='secondary'>Back to orders</Button>
						</Link>
						<Link href='/admin'>
							<Button variant='secondary'>Back to admin</Button>
						</Link>
					</div>
				</div>
				{error ?
					<p className='text-sm text-red-600'>{error}</p>
				:	null}
				{paymentsError ?
					<p className='text-sm text-amber-700'>{paymentsError}</p>
				:	null}
			</Card>

			{loading ?
				<Card>
					<p className='text-sm text-slate-600'>Loading order details...</p>
				</Card>
			:	null}

			{order ?
				<>
					<Card className='space-y-4'>
						<div className='grid gap-3 sm:grid-cols-2 lg:grid-cols-4'>
							<div>
								<p className='text-xs font-semibold text-slate-500'>Status</p>
								<span
									className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${statusClasses(order.status)}`}>
									{order.status || "—"}
								</span>
							</div>
							<div>
								<p className='text-xs font-semibold text-slate-500'>Created</p>
								<p className='text-sm text-slate-900'>{formatDate(order.createdAt)}</p>
							</div>
							<div>
								<p className='text-xs font-semibold text-slate-500'>Updated</p>
								<p className='text-sm text-slate-900'>{formatDate(order.updatedAt)}</p>
							</div>
							<div>
								<p className='text-xs font-semibold text-slate-500'>Items</p>
								<p className='text-sm text-slate-900'>{totalItems}</p>
							</div>
							<div>
								<p className='text-xs font-semibold text-slate-500'>Order total</p>
								<p className='text-sm font-semibold text-slate-900'>{formatPrice(order.totalAmount)}</p>
							</div>
							<div>
								<p className='text-xs font-semibold text-slate-500'>Payment records</p>
								<p className='text-sm text-slate-900'>{payments.length}</p>
							</div>
						</div>
					</Card>

					<Card className='space-y-4'>
						<h2 className='text-lg font-semibold text-slate-900'>Ordered Items</h2>
						<div className='overflow-x-auto'>
							<table className='min-w-full text-left text-sm'>
								<thead>
									<tr className='border-b border-brand-100 text-brand-800'>
										<th className='py-2 pr-4'>Item</th>
										<th className='py-2 pr-4'>Qty</th>
										<th className='py-2 pr-4'>Unit Price</th>
										<th className='py-2 pr-4'>Line Total</th>
									</tr>
								</thead>
								<tbody>
									{(order.items || []).map((item) => (
										<tr
											key={item.id || `${item.itemId}-${item.quantity}`}
											className='border-b border-brand-50 text-slate-700'>
											<td className='py-2 pr-4 font-medium text-slate-900'>{item.itemName || "Item"}</td>
											<td className='py-2 pr-4'>{item.quantity}</td>
											<td className='py-2 pr-4'>{formatPrice(item.unitPrice)}</td>
											<td className='py-2 pr-4'>{formatPrice(item.lineTotal)}</td>
										</tr>
									))}
								</tbody>
							</table>
						</div>
					</Card>

					<Card className='space-y-4'>
						<div className='flex flex-wrap items-center justify-between gap-2'>
							<h2 className='text-lg font-semibold text-slate-900'>Payment Records</h2>
							<p className='text-sm text-slate-600'>Total captured: {formatPrice(paymentTotal)}</p>
						</div>
						{payments.length === 0 ?
							<p className='text-sm text-slate-500'>No payment records found for this order.</p>
						:	<div className='overflow-x-auto'>
								<table className='min-w-full text-left text-sm'>
									<thead>
										<tr className='border-b border-brand-100 text-brand-800'>
											<th className='py-2 pr-4'>Item</th>
											<th className='py-2 pr-4'>Qty</th>
											<th className='py-2 pr-4'>Amount</th>
											<th className='py-2 pr-4'>Method</th>
											<th className='py-2 pr-4'>Status</th>
											<th className='py-2 pr-4'>Created</th>
										</tr>
									</thead>
									<tbody>
										{payments.map((payment) => (
											<tr key={payment.id} className='border-b border-brand-50 text-slate-700'>
												<td className='py-2 pr-4'>{payment.itemName || "Item"}</td>
												<td className='py-2 pr-4'>{payment.quantity}</td>
												<td className='py-2 pr-4 font-semibold'>{formatPrice(payment.amount)}</td>
												<td className='py-2 pr-4'>{payment.paymentMethod || "—"}</td>
												<td className='py-2 pr-4'>
													<span
														className={`rounded-full px-2 py-0.5 text-xs font-semibold ${statusClasses(payment.status)}`}>
														{payment.status || "—"}
													</span>
												</td>
												<td className='py-2 pr-4 text-xs text-slate-500'>{formatDate(payment.createdAt)}</td>
											</tr>
										))}
									</tbody>
								</table>
							</div>
						}
					</Card>
				</>
			:	null}
		</div>
	);
}
