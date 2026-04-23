export default function Footer() {
	const currentYear = new Date().getFullYear();

	return (
		<footer className='border-t bg-gradient-to-b from-slate-900 to-slate-950 text-slate-100 border-slate-800'>
			<div className='px-4 py-12 mx-auto max-w-7xl sm:px-6 lg:px-8'>
				{/* Main Footer Content */}
				<div className='grid grid-cols-1 gap-8 mb-8 md:grid-cols-2 lg:grid-cols-4'>
					{/* Brand Section */}
					<div className='space-y-4'>
						<h3 className='text-xl font-bold text-white'>🍃 GreenBite</h3>
						<p className='text-sm leading-relaxed text-slate-400'>
							Delivering fresh, delicious meals straight to your door. Your favorite restaurants, just a tap away.
						</p>
						<div className='flex gap-4'>
							<a href='#' className='transition text-slate-400 hover:text-brand-500'>
								<span className='text-lg'>f</span>
							</a>
							<a href='#' className='transition text-slate-400 hover:text-brand-500'>
								<span className='text-lg'>𝕏</span>
							</a>
							<a href='#' className='transition text-slate-400 hover:text-brand-500'>
								<span className='text-lg'>📱</span>
							</a>
						</div>
					</div>

					{/* About Us */}
					<div className='space-y-4'>
						<h4 className='mb-4 font-semibold text-white'>Company</h4>
						<ul className='space-y-2 text-sm'>
							<li>
								<a href='#' className='transition text-slate-400 hover:text-brand-500'>
									About Us
								</a>
							</li>
							<li>
								<a href='#' className='transition text-slate-400 hover:text-brand-500'>
									Careers
								</a>
							</li>
							<li>
								<a href='#' className='transition text-slate-400 hover:text-brand-500'>
									Blog
								</a>
							</li>
							<li>
								<a href='#' className='transition text-slate-400 hover:text-brand-500'>
									Press
								</a>
							</li>
						</ul>
					</div>

					{/* For Users */}
					<div className='space-y-4'>
						<h4 className='mb-4 font-semibold text-white'>For Users</h4>
						<ul className='space-y-2 text-sm'>
							<li>
								<a href='#' className='transition text-slate-400 hover:text-brand-500'>
									Browse Restaurants
								</a>
							</li>
							<li>
								<a href='#' className='transition text-slate-400 hover:text-brand-500'>
									Track Orders
								</a>
							</li>
							<li>
								<a href='#' className='transition text-slate-400 hover:text-brand-500'>
									Promotions
								</a>
							</li>
							<li>
								<a href='#' className='transition text-slate-400 hover:text-brand-500'>
									Help Center
								</a>
							</li>
						</ul>
					</div>

					{/* Contact & Support */}
					<div className='space-y-4'>
						<h4 className='mb-4 font-semibold text-white'>Support</h4>
						<ul className='space-y-2 text-sm'>
							<li>
								<a href='#' className='transition text-slate-400 hover:text-brand-500'>
									Contact Us
								</a>
							</li>
							<li>
								<a href='#' className='transition text-slate-400 hover:text-brand-500'>
									Privacy Policy
								</a>
							</li>
							<li>
								<a href='#' className='transition text-slate-400 hover:text-brand-500'>
									Terms of Service
								</a>
							</li>
							<li>
								<a href='#' className='transition text-slate-400 hover:text-brand-500'>
									FAQ
								</a>
							</li>
						</ul>
					</div>
				</div>

				{/* Divider */}
				<div className='my-8 border-t border-slate-800'></div>

				{/* Bottom Footer */}
				<div className='flex flex-col gap-4 text-sm md:flex-row md:items-center md:justify-between text-slate-400'>
					<p>© {currentYear} GreenBite. All rights reserved.</p>
					<div className='flex gap-6'>
						<a href='#' className='transition hover:text-brand-500'>
							Accessibility
						</a>
						<a href='#' className='transition hover:text-brand-500'>
							Cookie Preferences
						</a>
					</div>
				</div>
			</div>
		</footer>
	);
}
