import "./globals.css";
import AppShell from "@/components/layout/AppShell";

export const metadata = {
	title: "Frontend Service",
	description: "Next.js frontend for auth, customer, and admin flows",
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
