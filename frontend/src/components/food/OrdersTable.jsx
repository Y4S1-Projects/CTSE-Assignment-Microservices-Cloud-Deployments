import Card from "@/components/common/Card";

function formatPrice(value) {
	const numeric = Number(value || 0);
	return `$${numeric.toFixed(2)}`;
}

export default function OrdersTable({ orders, title = "Orders" }) {
	return (
		<Card className='space-y-4'>
			<h2 className='text-xl font-semibold text-slate-900'>{title}</h2>
			{orders.length === 0 ?
				<p className='text-sm text-slate-500'>No orders yet.</p>
			:	<div className='overflow-x-auto'>
					<table className='min-w-full text-left text-sm'>
						<thead>
							<tr className='border-b border-brand-100 text-brand-800'>
								<th className='py-2 pr-4'>Order ID</th>
								<th className='py-2 pr-4'>Status</th>
								<th className='py-2 pr-4'>Total</th>
							</tr>
						</thead>
						<tbody>
							{orders.map((order) => (
								<tr
									key={order.id || `${order.userId}-${order.totalAmount}`}
									className='border-b border-brand-50 text-slate-700'>
									<td className='py-2 pr-4'>{order.id || "N/A"}</td>
									<td className='py-2 pr-4'>{order.status || "PENDING"}</td>
									<td className='py-2 pr-4'>{formatPrice(order.totalAmount)}</td>
								</tr>
							))}
						</tbody>
					</table>
				</div>
			}
		</Card>
	);
}
