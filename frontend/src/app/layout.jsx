import "./globals.css";
import AppShell from "@/components/layout/AppShell";

export const metadata = {
	title: "GreenBite Food Ordering",
	description: "Food ordering frontend with auth, catalog, cart, orders, and payment flows",
};

export default function RootLayout({ children }) {
	return (
		<html lang='en'>
			<body>
				<AppShell>{children}</AppShell>
			</body>
		</html>
	);
}
