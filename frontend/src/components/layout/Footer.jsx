export default function Footer() {
	return (
		<footer className='border-t border-brand-100 bg-white'>
			<div className='mx-auto flex w-full max-w-7xl flex-col gap-2 px-4 py-6 text-sm text-slate-600 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8'>
				<p>Frontend service for CTSE microservices deployment.</p>
				<p className='text-brand-700'>Auth via Gateway :8080 | Direct Auth :8081</p>
			</div>
		</footer>
	);
}
