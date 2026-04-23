"use client";

import { useState } from "react";
import { CardElement, useElements, useStripe } from "@stripe/react-stripe-js";
import Button from "@/components/common/Button";
import Card from "@/components/common/Card";
import { notifyAlert } from "@/lib/alerts";

function formatPrice(value) {
	const numeric = Number(value || 0);
	return `$${numeric.toFixed(2)}`;
}

export default function StripeCheckout({ clientSecret, amount, orderId, onSuccess, onCancel }) {
	const stripe = useStripe();
	const elements = useElements();
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState(null);

	async function handleConfirmPayment() {
		if (!stripe || !elements) {
			setError("Stripe not loaded yet. Please try again.");
			notifyAlert({
				variant: "error",
				title: "Stripe unavailable",
				message: "Stripe is not loaded yet. Please try again.",
			});
			return;
		}

		setLoading(true);
		setError(null);

		try {
			// Confirm the payment with the card element
			const { error: confirmError, paymentIntent } = await stripe.confirmCardPayment(clientSecret, {
				payment_method: {
					card: elements.getElement(CardElement),
					billing_details: {
						name: "Customer",
					},
				},
			});

			if (confirmError) {
				notifyAlert({
					variant: "error",
					title: "Card payment failed",
					message: confirmError.message || "Payment failed. Please try again.",
				});
				setError(confirmError.message || "Payment failed. Please try again.");
				setLoading(false);
			} else if (paymentIntent && paymentIntent.status === "succeeded") {
				// Payment successful
				setTimeout(() => onSuccess(), 500);
			} else if (paymentIntent) {
				// Unexpected status
				notifyAlert({
					variant: "warning",
					title: "Unexpected payment status",
					message: `Stripe returned ${paymentIntent.status}. Please try again.`,
				});
				setError(`Unexpected payment status: ${paymentIntent.status}`);
				setLoading(false);
			}
		} catch (err) {
			notifyAlert({
				variant: "error",
				title: "Stripe payment error",
				message: err.message || "An error occurred. Please try again.",
			});
			setError(err.message || "An error occurred. Please try again.");
			setLoading(false);
		}
	}

	const cardElementOptions = {
		style: {
			base: {
				fontSize: "16px",
				color: "#1e293b",
				"::placeholder": {
					color: "#cbd5e1",
				},
				fontFamily: "ui-sans-serif, system-ui, -apple-system",
			},
			invalid: {
				color: "#dc2626",
			},
		},
		hidePostalCode: false,
	};

	return (
		<Card className='space-y-4 max-w-sm mx-auto'>
			<h2 className='text-xl font-semibold text-slate-900'>Enter Card Details</h2>

			{/* Card Input */}
			<div className='rounded-lg border border-slate-200 p-3 bg-white'>
				<CardElement options={cardElementOptions} />
			</div>

			{/* Error Message */}
			{error && (
				<div className='rounded-lg bg-red-50 p-3 border border-red-200'>
					<p className='text-sm text-red-700'>{error}</p>
				</div>
			)}

			{/* Amount Display */}
			<div className='rounded-lg bg-slate-50 p-3'>
				<p className='text-xs text-slate-600 mb-1'>Amount to pay</p>
				<p className='text-xl font-bold text-slate-900'>{formatPrice(amount)}</p>
			</div>

			{/* Action Buttons */}
			<div className='flex gap-3'>
				<Button variant='secondary' className='flex-1' disabled={loading} onClick={onCancel}>
					Cancel
				</Button>
				<Button className='flex-1' disabled={loading || !stripe || !elements} onClick={handleConfirmPayment}>
					{loading ? "Processing..." : `Pay ${formatPrice(amount)}`}
				</Button>
			</div>
		</Card>
	);
}
