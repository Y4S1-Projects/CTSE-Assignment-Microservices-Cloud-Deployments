export default function Button({
	children,
	type = "button",
	variant = "primary",
	size = "md",
	className = "",
	...rest
}) {
	const variants = {
		primary: "bg-brand-600 text-white hover:bg-brand-700",
		secondary: "bg-white text-brand-700 border border-brand-200 hover:bg-brand-100",
		ghost: "bg-transparent text-brand-700 hover:bg-brand-100",
	};

	const sizes = {
		sm: "px-3 py-1.5 text-sm",
		md: "px-4 py-2.5",
		lg: "px-6 py-3 text-lg",
	};

	return (
		<button
			type={type}
			className={`rounded-xl font-medium transition ${variants[variant]} ${sizes[size]} ${className}`}
			{...rest}>
			{children}
		</button>
	);
}
