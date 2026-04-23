"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Card from "@/components/common/Card";
import Button from "@/components/common/Button";
import { getPaymentByOrderId } from "@/lib/foodService";

const PAYMENT_SUCCESS_STORAGE_KEY = "frontend_payment_success";

function formatPrice(value) {
	const numeric = Number(value || 0);
	return `$${numeric.toFixed(2)}`;
}

export default function PaymentSuccessContent() {
	const router = useRouter();
	const params = useSearchParams();
	const [paymentContext, setPaymentContext] = useState({
		orderId: params.get("orderId") || "",
		total: params.get("total") || "",
	});
	const [payment, setPayment] = useState(null);

	useEffect(() => {
		if (typeof window !== "undefined") {
			const stored = sessionStorage.getItem(PAYMENT_SUCCESS_STORAGE_KEY);
			if (stored) {
				try {
					const parsed = JSON.parse(stored);
					if (parsed?.orderId || parsed?.total) {
						setPaymentContext({
							orderId: parsed?.orderId || "",
							total: parsed?.total || "",
						});
					}
				} catch {
					// Ignore malformed cached data and fall back to query params.
				}
				sessionStorage.removeItem(PAYMENT_SUCCESS_STORAGE_KEY);
			}
			sessionStorage.removeItem("menuItems");
		}

		if (paymentContext.orderId) {
			getPaymentByOrderId(paymentContext.orderId)
				.then(setPayment)
				.catch(() => null);
		}
	}, [paymentContext.orderId, params]);

	const total = paymentContext.total;

	return (
		<div className='max-w-lg py-12 mx-auto space-y-6 text-center'>
			<div className='flex justify-center'>
				<div className='flex items-center justify-center w-20 h-20 bg-green-100 rounded-full'>
					<svg className='w-10 h-10 text-green-600' fill='none' viewBox='0 0 24 24' stroke='currentColor'>
						<path strokeLinecap='round' strokeLinejoin='round' strokeWidth={2} d='M5 13l4 4L19 7' />
					</svg>
				</div>
			</div>

			<div>
				<h1 className='text-3xl font-bold text-slate-900'>Payment Successful!</h1>
				<p className='mt-2 text-slate-500'>Your order has been placed and payment confirmed.</p>
			</div>

			<Card className='space-y-3 text-left'>
				<h2 className='font-semibold text-slate-800'>Payment Details</h2>
				{total && (
					<div className='flex justify-between text-sm'>
						<span className='text-slate-500'>Amount Paid</span>
						<span className='font-bold text-green-700'>{formatPrice(total)}</span>
					</div>
				)}
				{payment?.paymentMethod && (
					<div className='flex justify-between text-sm'>
						<span className='text-slate-500'>Payment Method</span>
						<span className='text-slate-800'>{payment.paymentMethod}</span>
					</div>
				)}
				{payment?.status && (
					<div className='flex justify-between text-sm'>
						<span className='text-slate-500'>Status</span>
						<span className='inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-semibold text-green-800'>
							{payment.status}
						</span>
					</div>
				)}
				<p className='pt-1 text-xs text-slate-400'>Stock has been updated in real-time in the catalog.</p>
			</Card>

			<div className='flex gap-3'>
				<Button variant='secondary' className='flex-1' onClick={() => router.push("/customer")}>
					Back to Menu
				</Button>
				<Button className='flex-1' onClick={() => router.push("/customer")}>
					Place Another Order
				</Button>
			</div>
		</div>
	);
}
