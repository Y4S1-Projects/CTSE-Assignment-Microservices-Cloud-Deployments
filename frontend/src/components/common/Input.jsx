export default function Input({ label, error, className = "", ...props }) {
	return (
		<label className='block space-y-1.5'>
			<span className='text-sm font-medium text-slate-700'>{label}</span>
			<input
				className={`w-full rounded-xl border border-brand-200 bg-white px-3.5 py-2.5 text-slate-900 outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-200 ${className}`}
				{...props}
			/>
			{error ?
				<span className='text-xs text-red-600'>{error}</span>
			:	null}
		</label>
	);
}
