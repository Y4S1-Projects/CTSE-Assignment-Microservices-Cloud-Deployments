import { Suspense } from "react";
import PaymentSuccessContent from "./payment-success-content";

function PaymentSuccessFallback() {
	return (
		<div className='max-w-lg mx-auto py-12 space-y-6 text-center'>
			<div className='flex justify-center'>
				<div className='h-20 w-20 rounded-full bg-green-100 flex items-center justify-center animate-pulse'>
					<svg className='h-10 w-10 text-green-600' fill='none' viewBox='0 0 24 24' stroke='currentColor'>
						<path strokeLinecap='round' strokeLinejoin='round' strokeWidth={2} d='M5 13l4 4L19 7' />
					</svg>
				</div>
			</div>
			<div>
				<h1 className='text-3xl font-bold text-slate-900'>Loading payment details...</h1>
				<p className='mt-2 text-slate-500'>Preparing your payment confirmation screen.</p>
			</div>
		</div>
	);
}

export default function PaymentSuccessPage() {
	return (
		<Suspense fallback={<PaymentSuccessFallback />}>
			<PaymentSuccessContent />
		</Suspense>
	);
}
