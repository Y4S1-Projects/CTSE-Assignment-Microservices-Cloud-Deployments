import Header from "@/components/layout/Header";
import Footer from "@/components/layout/Footer";
import AlertCenter from "@/components/common/AlertCenter";

export default function AppShell({ children }) {
	return (
		<div className='gradient-bg flex min-h-screen flex-col'>
			<AlertCenter />
			<Header />
			<main className='mx-auto w-full max-w-7xl flex-1 px-4 py-8 sm:px-6 lg:px-8'>{children}</main>
			<Footer />
		</div>
	);
}
