import Link from "next/link";
import Image from "next/image";
import Button from "@/components/common/Button";
import Card from "@/components/common/Card";

export default function HeroSection() {
	return (
		<section className='grid gap-8 lg:grid-cols-2 lg:items-center'>
			<div className='space-y-5'>
				<p className='inline-flex rounded-full bg-brand-100 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-brand-800'>
					Frontend Service
				</p>
				<h1 className='text-4xl font-bold leading-tight text-slate-900 sm:text-5xl'>
					Modern auth portal for customer and admin experiences.
				</h1>
				<p className='max-w-xl text-slate-600'>
					Built with Next.js and Tailwind CSS. Use the gateway or direct auth service with clear, reusable UI components
					and role-based navigation.
				</p>
				<div className='flex flex-wrap items-center gap-3'>
					<Link href='/auth/register'>
						<Button>Create Account</Button>
					</Link>
					<Link href='/auth/login'>
						<Button variant='secondary'>Sign In</Button>
					</Link>
				</div>
			</div>
			<Card className='space-y-4'>
				<Image
					src='/images/hero-illustration.svg'
					alt='Modern auth platform'
					width={520}
					height={320}
					className='h-auto w-full rounded-xl border border-brand-100'
					priority
				/>
				<h2 className='text-xl font-semibold text-slate-900'>Quick Navigation</h2>
				<ul className='space-y-2 text-sm text-slate-700'>
					<li>Customer area for personal profile and account actions</li>
					<li>Admin area for user management actions</li>
					<li>Integrated auth functions: register, login, validate, refresh, logout</li>
				</ul>
				<p className='rounded-xl bg-brand-50 p-3 text-sm text-brand-800'>
					Default integration target is API Gateway on port 8080.
				</p>
			</Card>
		</section>
	);
}
