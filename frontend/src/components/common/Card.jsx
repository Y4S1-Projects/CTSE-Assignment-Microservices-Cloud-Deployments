export default function Card({ children, className = "" }) {
	return <div className={`rounded-2xl border border-brand-100 bg-white p-6 shadow-soft ${className}`}>{children}</div>;
}
