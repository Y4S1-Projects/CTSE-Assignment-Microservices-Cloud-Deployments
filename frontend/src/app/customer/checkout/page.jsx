"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import dynamic from "next/dynamic";
import { loadStripe } from "@stripe/stripe-js";
import { Elements } from "@stripe/react-stripe-js";
import Card from "@/components/common/Card";
import Button from "@/components/common/Button";
import { checkout, createOrder, createStripeIntent } from "@/lib/foodService";
import { notifyAlert } from "@/lib/alerts";
import { clearCart, getAuthToken, getCart, getCurrentUser } from "@/lib/storage";

// Dynamic import to avoid hydration issues with Stripe
const StripeCheckout = dynamic(() => import("@/components/StripeCheckout"), {
	ssr: false,
});

const stripePromise = loadStripe(
	"pk_test_51TDiNzRL1F3MeUCkfwlcHcpbzslBdXs3UwCyvUyHEGCDJXZk81CfTxFRWKZTpbPT6YipeDen5QadaOupTdaCpkj1002q1GoINw",
);

function formatPrice(value) {
	const numeric = Number(value || 0);
	return `$${numeric.toFixed(2)}`;
}

export default function CheckoutPage() {
	const router = useRouter();
	const [cart, setCart] = useState([]);
	const [paymentMethod, setPaymentMethod] = useState("CARD");
	const [cardNumber, setCardNumber] = useState("");
	const [cardName, setCardName] = useState("");
	const [expiry, setExpiry] = useState("");
	const [cvv, setCvv] = useState("");
	const [loading, setLoading] = useState(false);
	const [error, setError] = useState("");

	// Stripe mode state
	const [stripeMode, setStripeMode] = useState(false);
	const [stripeClientSecret, setStripeClientSecret] = useState(null);
	const [stripePaymentIntentId, setStripePaymentIntentId] = useState(null);
	const [stripeLoading, setStripeLoading] = useState(false);
	const [stripeOrderId, setStripeOrderId] = useState(null);

	const subtotal = cart.reduce((sum, item) => sum + Number(item.price || 0) * item.quantity, 0);

	useEffect(() => {
		if (!getAuthToken()) {
			router.replace("/auth/login");
			return;
		}
		const stored = getCart();
		if (!stored || stored.length === 0) {
			router.replace("/customer");
			return;
		}
		setCart(stored);
	}, [router]);

	async function handlePay() {
		setError("");

		// Basic card validation
		if (paymentMethod === "CARD") {
			if (!cardName.trim()) return setError("Please enter the cardholder name.");
			if (cardNumber.replace(/\s/g, "").length < 16) return setError("Please enter a valid 16-digit card number.");
			if (!expiry.match(/^\d{2}\/\d{2}$/)) return setError("Please enter expiry in MM/YY format.");
			if (cvv.length < 3) return setError("Please enter a valid CVV.");
		}

		setLoading(true);
		const userId = getCurrentUser()?.id || "guest";

		try {
			// 1) Create the order in order-service
			const created = await createOrder(cart);

			// 2) Process payment for each line item
			for (const orderItem of created?.items || []) {
				await checkout({
					itemId: orderItem.itemId || orderItem.catalogItemId,
					orderId: created.id,
					userId,
					quantity: orderItem.quantity,
					amount: orderItem.lineTotal,
					paymentMethod,
				});
			}

			// 3) Clear cart and go to success page
			clearCart();
			notifyAlert({
				variant: "success",
				title: "Order placed",
				message: `Your order ${created.id} was processed successfully.`,
			});
			router.push(`/customer/payment-success?orderId=${created.id}&total=${subtotal.toFixed(2)}`);
		} catch (err) {
			notifyAlert({
				variant: "error",
				title: "Checkout failed",
				message: err.message || "Payment failed. Please try again.",
			});
			setError(err.message || "Payment failed. Please try again.");
			setLoading(false);
		}
	}

	async function handlePayWithStripe() {
		setStripeLoading(true);
		setError("");
		const userId = getCurrentUser()?.id || "guest";

		try {
			// 1) Create the order in order-service first
			const created = await createOrder(cart);
			setStripeOrderId(created.id);

			// 2) Create Stripe PaymentIntent for the full order amount (in cents)
			const amountInCents = Math.round(subtotal * 100);

			const intentData = await createStripeIntent({
				orderId: created.id,
				userId,
				itemId: created.items?.[0]?.itemId || "",
				quantity: cart.length,
				amount: amountInCents,
				currency: "usd",
				description: `Order ${created.id} - ${cart.length} items`,
			});

			if (!intentData || !intentData.clientSecret) {
				throw new Error("Failed to create Stripe PaymentIntent");
			}

			// 3) Store the intent details and show the modal
			setStripeClientSecret(intentData.clientSecret);
			setStripePaymentIntentId(intentData.paymentIntentId);
			setStripeMode(true);
			setStripeLoading(false);
		} catch (err) {
			notifyAlert({
				variant: "error",
				title: "Stripe checkout failed",
				message: err.message || "Failed to initiate Stripe payment. Please try again.",
			});
			setError(err.message || "Failed to initiate Stripe payment. Please try again.");
			setStripeLoading(false);
		}
	}

	async function handleStripeSuccess() {
		// Payment confirmed by Stripe -- now mark items as COMPLETED in our backend
		try {
			if (!stripeOrderId) {
				setError("Missing Stripe order ID. Please retry payment.");
				return;
			}

			const orderItems = cart.map((item) => ({
				itemId: item.id,
				catalogItemId: item.itemId || item.id,
				quantity: item.quantity,
				lineTotal: item.price * item.quantity,
			}));

			for (const orderItem of orderItems) {
				await checkout({
					itemId: orderItem.catalogItemId,
					orderId: stripeOrderId,
					userId: getCurrentUser()?.id || "guest",
					quantity: orderItem.quantity,
					amount: orderItem.lineTotal,
					paymentMethod: "STRIPE",
				});
			}

			clearCart();
			setStripeMode(false);
			setStripeClientSecret(null);
			setStripePaymentIntentId(null);
			setStripeOrderId(null);
			notifyAlert({
				variant: "success",
				title: "Payment confirmed",
				message: `Order ${stripeOrderId} has been placed successfully.`,
			});
			router.push(`/customer/payment-success?orderId=${stripeOrderId}&total=${subtotal.toFixed(2)}`);
		} catch (err) {
			notifyAlert({
				variant: "error",
				title: "Payment finalization failed",
				message: "Payment succeeded but failed to finalize order. Please contact support.",
			});
			setError("Payment succeeded but failed to finalize order. Please contact support.");
			console.error(err);
		}
	}

	function formatCardNumber(value) {
		return value
			.replace(/\D/g, "")
			.slice(0, 16)
			.replace(/(.{4})/g, "$1 ")
			.trim();
	}

	return (
		<div className='max-w-2xl mx-auto space-y-6 py-6'>
			<h1 className='text-2xl font-bold text-slate-900'>Checkout</h1>

			{/* Order Summary */}
			<Card className='space-y-3'>
				<h2 className='text-lg font-semibold text-slate-800'>Order Summary</h2>
				{cart.map((item) => (
					<div key={item.id} className='flex justify-between text-sm text-slate-700'>
						<span>
							{item.name} × {item.quantity}
						</span>
						<span>{formatPrice(Number(item.price) * item.quantity)}</span>
					</div>
				))}
				<div className='border-t border-brand-100 pt-3 flex justify-between font-bold text-slate-900'>
					<span>Total</span>
					<span>{formatPrice(subtotal)}</span>
				</div>
			</Card>

			{/* Payment Method */}
			<Card className='space-y-4'>
				<h2 className='text-lg font-semibold text-slate-800'>Payment Method</h2>
				<div className='flex gap-3'>
					{["CARD", "CASH", "ONLINE"].map((method) => (
						<button
							key={method}
							type='button'
							onClick={() => setPaymentMethod(method)}
							className={`flex-1 rounded-xl border-2 py-2 text-sm font-semibold transition ${
								paymentMethod === method ?
									"border-brand-600 bg-brand-50 text-brand-700"
								:	"border-brand-100 text-slate-600 hover:border-brand-300"
							}`}>
							{method === "CARD" ?
								"💳 Card"
							: method === "CASH" ?
								"💵 Cash"
							:	"🌐 Online"}
						</button>
					))}
				</div>

				{/* Card Details Form */}
				{paymentMethod === "CARD" && (
					<div className='space-y-3 pt-2'>
						<div>
							<label className='block text-xs font-semibold text-slate-500 mb-1'>Cardholder Name</label>
							<input
								type='text'
								placeholder='Vilan Siriwardana'
								value={cardName}
								onChange={(e) => setCardName(e.target.value)}
								className='w-full rounded-xl border border-brand-200 px-4 py-2 text-sm text-slate-800 focus:border-brand-500 focus:outline-none'
							/>
						</div>
						<div>
							<label className='block text-xs font-semibold text-slate-500 mb-1'>Card Number</label>
							<input
								type='text'
								placeholder='1234 5678 9012 3456'
								value={cardNumber}
								onChange={(e) => setCardNumber(formatCardNumber(e.target.value))}
								maxLength={19}
								className='w-full rounded-xl border border-brand-200 px-4 py-2 text-sm text-slate-800 focus:border-brand-500 focus:outline-none font-mono'
							/>
						</div>
						<div className='grid grid-cols-2 gap-3'>
							<div>
								<label className='block text-xs font-semibold text-slate-500 mb-1'>Expiry (MM/YY)</label>
								<input
									type='text'
									placeholder='12/27'
									value={expiry}
									onChange={(e) => {
										let v = e.target.value.replace(/\D/g, "").slice(0, 4);
										if (v.length >= 3) v = v.slice(0, 2) + "/" + v.slice(2);
										setExpiry(v);
									}}
									maxLength={5}
									className='w-full rounded-xl border border-brand-200 px-4 py-2 text-sm text-slate-800 focus:border-brand-500 focus:outline-none'
								/>
							</div>
							<div>
								<label className='block text-xs font-semibold text-slate-500 mb-1'>CVV</label>
								<input
									type='password'
									placeholder='•••'
									value={cvv}
									onChange={(e) => setCvv(e.target.value.replace(/\D/g, "").slice(0, 4))}
									maxLength={4}
									className='w-full rounded-xl border border-brand-200 px-4 py-2 text-sm text-slate-800 focus:border-brand-500 focus:outline-none'
								/>
							</div>
						</div>
					</div>
				)}

				{paymentMethod === "ONLINE" && (
					<p className='text-sm text-slate-500 bg-brand-50 rounded-xl px-4 py-3'>
						You will be redirected to complete payment via your preferred online method.
					</p>
				)}

				{paymentMethod === "CASH" && (
					<p className='text-sm text-slate-500 bg-brand-50 rounded-xl px-4 py-3'>
						Pay with cash on delivery. Your order will be confirmed immediately.
					</p>
				)}
			</Card>

			{/* Error */}
			{error && <p className='text-sm text-red-600 bg-red-50 rounded-xl px-4 py-3'>{error}</p>}

			{/* Actions */}
			<div className='flex gap-3'>
				<Button
					variant='secondary'
					className='flex-1'
					onClick={() => router.push("/customer")}
					disabled={loading || stripeLoading}>
					Back to Cart
				</Button>
				<Button className='flex-1' onClick={handlePay} disabled={loading || stripeLoading || cart.length === 0}>
					{loading ? "Processing..." : `Pay ${formatPrice(subtotal)}`}
				</Button>
				<Button
					className='flex-1 bg-purple-600 hover:bg-purple-700'
					onClick={handlePayWithStripe}
					disabled={loading || stripeLoading || cart.length === 0}>
					{stripeLoading ? "Loading..." : `💳 Pay with Stripe`}
				</Button>
			</div>

			{/* Stripe Modal Overlay */}
			{stripeMode && (
				<div className='fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50'>
					<div className='bg-white rounded-xl p-6 max-w-sm w-full mx-4'>
						<Elements stripe={stripePromise}>
							<StripeCheckout
								clientSecret={stripeClientSecret}
								amount={subtotal}
								orderId={stripeOrderId}
								onSuccess={handleStripeSuccess}
								onCancel={() => {
									setStripeMode(false);
									setStripeClientSecret(null);
									setStripePaymentIntentId(null);
								}}
							/>
						</Elements>
					</div>
				</div>
			)}
		</div>
	);
}
