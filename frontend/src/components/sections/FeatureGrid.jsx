import Card from "@/components/common/Card";

const features = [
	{
		title: "Menu & Cart",
		description: "Browse food catalog, filter by category, and build your order in real time.",
	},
	{
		title: "Checkout & Payments",
		description: "Create orders from cart items and trigger payment processing in one flow.",
	},
	{
		title: "Auth + Admin",
		description: "Integrated login/register/logout plus admin controls for users and orders.",
	},
];

export default function FeatureGrid() {
	return (
		<section className='mt-10 grid gap-4 md:grid-cols-3'>
			{features.map((feature) => (
				<Card key={feature.title} className='space-y-2'>
					<h3 className='text-lg font-semibold text-brand-800'>{feature.title}</h3>
					<p className='text-sm text-slate-600'>{feature.description}</p>
				</Card>
			))}
		</section>
	);
}
