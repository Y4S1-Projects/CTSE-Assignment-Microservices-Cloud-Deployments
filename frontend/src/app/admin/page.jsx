import { redirect } from "next/navigation";

export default function AdminPage() {
	// Admin dashboard removed - header provides navigation to all admin pages
	// Redirect to catalog management as the default admin view
	redirect("/admin/catalog");
}
