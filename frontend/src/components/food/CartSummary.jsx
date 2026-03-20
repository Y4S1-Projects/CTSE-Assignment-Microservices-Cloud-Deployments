import Button from "@/components/common/Button";
import Card from "@/components/common/Card";

function formatPrice(value) {
	const numeric = Number(value || 0);
	return `$${numeric.toFixed(2)}`;
}

export default function CartSummary({ items, onChangeQty, onRemove, onCheckout, loading }) {
	const subtotal = items.reduce((sum, item) => sum + Number(item.price || 0) * item.quantity, 0);

	return (
		<Card className='space-y-4'>
			<h2 className='text-xl font-semibold text-slate-900'>Cart</h2>
			{items.length === 0 ?
				<p className='text-sm text-slate-500'>No items in cart yet.</p>
			:	<div className='space-y-3'>
					{items.map((item) => (
						<div key={item.id} className='rounded-xl border border-brand-100 p-3'>
							<div className='flex items-center justify-between gap-3'>
								<div>
									<p className='font-medium text-slate-900'>{item.name}</p>
									<p className='text-sm text-slate-600'>{formatPrice(item.price)} each</p>
								</div>
								<Button variant='ghost' onClick={() => onRemove(item.id)}>
									Remove
								</Button>
							</div>
							<div className='mt-3 flex items-center gap-2'>
								<Button variant='secondary' onClick={() => onChangeQty(item.id, -1)}>
									-
								</Button>
								<span className='w-10 text-center font-semibold text-slate-900'>{item.quantity}</span>
								<Button variant='secondary' onClick={() => onChangeQty(item.id, 1)}>
									+
								</Button>
							</div>
						</div>
					))}
				</div>
			}
			<div className='rounded-xl bg-brand-50 p-3'>
				<p className='text-sm text-slate-700'>Subtotal</p>
				<p className='text-xl font-bold text-brand-700'>{formatPrice(subtotal)}</p>
			</div>
			<Button className='w-full' disabled={items.length === 0 || loading} onClick={onCheckout}>
				{loading ? "Processing order..." : "Place Order"}
			</Button>
		</Card>
	);
}
