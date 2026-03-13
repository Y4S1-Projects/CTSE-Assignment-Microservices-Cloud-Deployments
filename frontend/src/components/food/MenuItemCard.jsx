import Button from "@/components/common/Button";
import Card from "@/components/common/Card";

function formatPrice(value) {
	const numeric = Number(value || 0);
	return `$${numeric.toFixed(2)}`;
}

export default function MenuItemCard({ item, onAdd }) {
	const available = (item.availability || "AVAILABLE").toUpperCase() === "AVAILABLE";

	return (
		<Card className='flex h-full flex-col overflow-hidden p-0'>
			<div className='h-40 w-full bg-brand-100'>
				{item.imageUrl ?
					<img src={item.imageUrl} alt={item.name} className='h-full w-full object-cover' />
				:	<div className='flex h-full items-center justify-center text-sm text-brand-800'>Food Item</div>}
			</div>
			<div className='flex flex-1 flex-col space-y-2 p-4'>
				<div className='flex items-start justify-between gap-2'>
					<h3 className='text-lg font-semibold text-slate-900'>{item.name || "Menu Item"}</h3>
					<span className='rounded-full bg-brand-100 px-2.5 py-1 text-xs font-semibold text-brand-800'>
						{item.category || "General"}
					</span>
				</div>
				<p className='flex-1 text-sm text-slate-600'>{item.description || "No description available."}</p>
				<div className='flex items-center justify-between'>
					<p className='text-base font-bold text-brand-700'>{formatPrice(item.price)}</p>
					<Button onClick={() => onAdd(item)} disabled={!available}>
						{available ? "Add to Cart" : "Unavailable"}
					</Button>
				</div>
			</div>
		</Card>
	);
}
