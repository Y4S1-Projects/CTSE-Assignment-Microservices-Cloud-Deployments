export default function Footer() {
	return (
		<footer className='border-t border-brand-100 bg-white'>
			<div className='mx-auto flex w-full max-w-7xl flex-col gap-2 px-4 py-6 text-sm text-slate-600 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:px-8'>
				<p>GreenBite food ordering frontend powered by microservices.</p>
				<p className='text-brand-700'>Gateway :8080 | Auth :8081 | Catalog :8082 | Orders :8083 | Payments :8084</p>
			</div>
		</footer>
	);
}
