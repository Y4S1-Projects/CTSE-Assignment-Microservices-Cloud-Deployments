export default function Button({ children, type = "button", variant = "primary", className = "", ...rest }) {
	const variants = {
		primary: "bg-brand-600 text-white hover:bg-brand-700",
		secondary: "bg-white text-brand-700 border border-brand-200 hover:bg-brand-100",
		ghost: "bg-transparent text-brand-700 hover:bg-brand-100",
	};

	return (
		<button
			type={type}
			className={`rounded-xl px-4 py-2.5 font-medium transition ${variants[variant]} ${className}`}
			{...rest}>
			{children}
		</button>
	);
}
