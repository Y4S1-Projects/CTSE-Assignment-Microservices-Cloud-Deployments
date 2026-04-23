"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Card from "@/components/common/Card";
import Button from "@/components/common/Button";
import { getOrderById } from "@/lib/foodService";
import { getAuthToken } from "@/lib/storage";

function formatPrice(value) {
	const numeric = Number(value || 0);
	return `$${numeric.toFixed(2)}`;
}

export default function OrderDetailsPage() {
	const router = useRouter();
	const params = useParams();
	const id = params?.id;

	const [order, setOrder] = useState(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState("");

	useEffect(() => {
		if (!getAuthToken()) {
			router.replace("/auth/login");
			return;
		}
		if (!id) return;

		async function load() {
			setLoading(true);
			setError("");
			try {
				const data = await getOrderById(id);
				setOrder(data);
			} catch (e) {
				setError(e.message || "Failed to load order");
			} finally {
				setLoading(false);
			}
		}

		load();
	}, [id, router]);

	const totalItems = useMemo(
		() => (Array.isArray(order?.items) ? order.items.reduce((sum, it) => sum + Number(it.quantity || 0), 0) : 0),
		[order],
	);

	return (
		<div className='space-y-4'>
			<div className='flex flex-wrap items-center justify-between gap-3'>
				<h1 className='text-2xl font-bold text-slate-900'>Order Details</h1>
				<Button type='button' onClick={() => router.push("/customer")}>
					Back to dashboard
				</Button>
			</div>

			{loading ?
				<Card>
					<p className='text-sm text-slate-600'>Loading order...</p>
				</Card>
			:	null}

			{error ?
				<Card>
					<p className='text-sm text-red-600'>{error}</p>
				</Card>
			:	null}

			{order ?
				<Card className='space-y-4'>
					<div className='grid gap-2 sm:grid-cols-2'>
						<div>
							<p className='text-xs font-semibold text-slate-500'>Status</p>
							<p className='text-sm text-slate-800'>{order.status}</p>
						</div>
						<div>
							<p className='text-xs font-semibold text-slate-500'>Total amount</p>
							<p className='text-sm text-slate-800'>{formatPrice(order.totalAmount)}</p>
						</div>
						<div>
							<p className='text-xs font-semibold text-slate-500'>Total items</p>
							<p className='text-sm text-slate-800'>{totalItems}</p>
						</div>
					</div>

					<div className='overflow-x-auto'>
						<table className='min-w-full text-left text-sm'>
							<thead>
								<tr className='border-b border-brand-100 text-brand-800'>
									<th className='py-2 pr-4'>Item</th>
									<th className='py-2 pr-4'>Unit</th>
									<th className='py-2 pr-4'>Qty</th>
									<th className='py-2 pr-4'>Line total</th>
								</tr>
							</thead>
							<tbody>
								{(order.items || []).map((it) => (
									<tr key={it.id || `${it.itemId}-${it.quantity}`} className='border-b border-brand-50 text-slate-700'>
										<td className='py-2 pr-4'>
											<p className='font-medium text-slate-900'>{it.itemName || "Item"}</p>
										</td>
										<td className='py-2 pr-4'>{formatPrice(it.unitPrice)}</td>
										<td className='py-2 pr-4'>{it.quantity}</td>
										<td className='py-2 pr-4'>{formatPrice(it.lineTotal)}</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				</Card>
			:	null}
		</div>
	);
}
