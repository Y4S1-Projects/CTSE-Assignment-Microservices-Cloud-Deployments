import Card from "@/components/common/Card";

const features = [
	{
		title: "Reusable Components",
		description: "Centralized buttons, inputs, cards, and layout shell for consistency.",
	},
	{
		title: "Role Separation",
		description: "Dedicated routes for customer and admin side experiences.",
	},
	{
		title: "Auth Service Integration",
		description: "Prepared API functions for auth endpoints with token utilities.",
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
