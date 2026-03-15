"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Button from "@/components/common/Button";
import Card from "@/components/common/Card";
import Input from "@/components/common/Input";
import { getAllUsers, logoutUser, updateUserStatus } from "@/lib/authService";
import {
	createCatalogItem,
	deleteCatalogItem,
	getAllOrders,
	getCatalogDashboard,
	getMenuItems,
	updateCatalogStock,
	updateOrderStatus,
} from "@/lib/foodService";
import { getAuthToken, isAdminUser } from "@/lib/storage";

function formatPrice(value) {
	return `$${Number(value || 0).toFixed(2)}`;
}

const emptyItemForm = {
	name: "",
	description: "",
	price: "",
	category: "",
	stockCount: "",
	imageUrl: "",
	itemId: "",
};

export default function AdminPage() {
	const router = useRouter();
	const [users, setUsers] = useState([]);
	const [menuItems, setMenuItems] = useState([]);
	const [orders, setOrders] = useState([]);
	const [dashboard, setDashboard] = useState(null);
	const [orderId, setOrderId] = useState("");
	const [orderStatus, setOrderStatus] = useState("PREPARING");
	const [error, setError] = useState("");
	const [success, setSuccess] = useState("");
	const [authorized, setAuthorized] = useState(false);
	const [activeTab, setActiveTab] = useState("dashboard");
	// New item form
	const [itemForm, setItemForm] = useState(emptyItemForm);
	const [savingItem, setSavingItem] = useState(false);
	// Inline stock edit
	const [editingStock, setEditingStock] = useState({});   // { [id]: newStockValue }

	async function loadData() {
		setError("");
		try {
			const [usersData, menuData, ordersData, dashData] = await Promise.all([
				getAllUsers().catch(() => []),
				getMenuItems(),
				getAllOrders().catch(() => []),
				getCatalogDashboard().catch(() => null),
			]);
			setUsers(Array.isArray(usersData) ? usersData : []);
			setMenuItems(Array.isArray(menuData) ? menuData : []);
			setOrders(Array.isArray(ordersData) ? ordersData : []);
			setDashboard(dashData);
		} catch (loadError) {
			setError(loadError.message || "Failed to load admin data");
		}
	}

	useEffect(() => {
		if (!getAuthToken()) {
			router.replace("/auth/login");
			return;
		}
		if (!isAdminUser()) {
			router.replace("/customer");
			return;
		}
		setAuthorized(true);
		loadData();
	}, [router]);

	async function handleToggleUser(user) {
		setError("");
		setSuccess("");
		try {
			await updateUserStatus(user.id, !user.active);
			setSuccess(`Updated status for ${user.username}`);
			await loadData();
		} catch (err) {
			setError(err.message || "Failed to update user status");
		}
	}

	async function handleOrderStatus(event) {
		event.preventDefault();
		setError("");
		setSuccess("");
		try {
			await updateOrderStatus(orderId, orderStatus);
			setSuccess(`Order ${orderId} updated to ${orderStatus}`);
			setOrderId("");
		} catch (err) {
			setError(err.message || "Failed to update order status");
		}
	}

	async function handleAddItem(event) {
		event.preventDefault();
		setSavingItem(true);
		setError("");
		setSuccess("");
		try {
			await createCatalogItem({
				...itemForm,
				price: parseFloat(itemForm.price),
				stockCount: parseInt(itemForm.stockCount, 10),
			});
			setSuccess(`Item "${itemForm.name}" added to catalog.`);
			setItemForm(emptyItemForm);
			await loadData();
		} catch (err) {
			setError(err.message || "Failed to add item");
		} finally {
			setSavingItem(false);
		}
	}

	async function handleSaveStock(item) {
		setError("");
		setSuccess("");
		const newStock = parseInt(editingStock[item.id], 10);
		if (isNaN(newStock) || newStock < 0) {
			setError("Stock count must be a non-negative number");
			return;
		}
		try {
			await updateCatalogStock(item.id, { stockCount: newStock });
			setSuccess(`Stock for "${item.name}" updated to ${newStock}`);
			setEditingStock((prev) => {
				const next = { ...prev };
				delete next[item.id];
				return next;
			});
			await loadData();
		} catch (err) {
			setError(err.message || "Failed to update stock");
		}
	}

	async function handleDeleteItem(item) {
		if (!confirm(`Delete "${item.name}"? This cannot be undone.`)) return;
		setError("");
		setSuccess("");
		try {
			await deleteCatalogItem(item.id);
			setSuccess(`"${item.name}" deleted.`);
			await loadData();
		} catch (err) {
			setError(err.message || "Failed to delete item");
		}
	}

	async function handleLogout() {
		await logoutUser();
		window.location.href = "/auth/login";
	}

	if (!authorized) {
		return (
			<Card className='mx-auto mt-10 max-w-xl'>
				<p className='text-sm text-slate-600'>Checking admin access...</p>
			</Card>
		);
	}

	const tabs = [
		{ id: "dashboard", label: "Dashboard" },
		{ id: "orders", label: "Orders" },
		{ id: "users", label: "Users" },
	];

	return (
		<div className='space-y-6'>
			{/* ── header ── */}
			<Card className='space-y-2'>
				<div className='flex flex-wrap items-center justify-between gap-3'>
					<div>
						<h1 className='text-2xl font-bold text-slate-900'>Admin Control Centre</h1>
						<p className='text-sm text-slate-600'>Catalog, inventory, orders &amp; user management</p>
					</div>
					<Button onClick={handleLogout}>Logout</Button>
				</div>
				{error ? <p className='text-sm text-red-600'>{error}</p> : null}
				{success ? <p className='text-sm text-green-700'>{success}</p> : null}
			</Card>

			{/* ── nav tabs ── */}
			<div className='flex flex-wrap gap-2'>
				{tabs.map((tab) => (
					<button
						key={tab.id}
						type='button'
						onClick={() => setActiveTab(tab.id)}
						className={`rounded-full px-4 py-1.5 text-sm font-semibold transition ${
							activeTab === tab.id ? "bg-brand-600 text-white" : "bg-brand-100 text-brand-800 hover:bg-brand-200"
						}`}>
						{tab.label}
					</button>
				))}
			</div>

			{/* ── Dashboard tab ── */}
			{activeTab === "dashboard" && (
				<div className='space-y-4'>
					{dashboard ? (
						<>
							<div className='grid grid-cols-2 gap-4 sm:grid-cols-4'>
								{[
									{ label: "Total Items",    value: dashboard.totalItems,    color: "bg-blue-50 text-blue-700" },
									{ label: "In Stock",       value: dashboard.availableItems,  color: "bg-green-50 text-green-700" },
									{ label: "Out of Stock",   value: dashboard.outOfStockItems, color: "bg-red-50 text-red-700" },
									{ label: "Low Stock (≤5)", value: dashboard.lowStockItems,   color: "bg-yellow-50 text-yellow-700" },
								].map((stat) => (
									<Card key={stat.label} className={`text-center ${stat.color}`}>
										<p className='text-3xl font-bold'>{stat.value}</p>
										<p className='mt-1 text-xs font-medium'>{stat.label}</p>
									</Card>
								))}
							</div>

							<Card className='space-y-3'>
								<h2 className='text-lg font-semibold text-slate-900'>Category Breakdown</h2>
								<div className='overflow-x-auto'>
									<table className='min-w-full text-left text-sm'>
										<thead>
											<tr className='border-b border-brand-100 text-brand-800'>
												<th className='py-2 pr-6'>Category</th>
												<th className='py-2 pr-6'>Items</th>
												<th className='py-2 pr-6'>Total Stock</th>
											</tr>
										</thead>
										<tbody>
											{(dashboard.categoryStats || []).map((cs) => (
												<tr key={cs.category} className='border-b border-brand-50 text-slate-700'>
													<td className='py-2 pr-6 font-medium'>{cs.category}</td>
													<td className='py-2 pr-6'>{cs.count}</td>
													<td className='py-2 pr-6'>{cs.totalStock}</td>
												</tr>
											))}
										</tbody>
									</table>
								</div>
							</Card>
						</>
					) : (
						<Card><p className='text-sm text-slate-500'>Loading dashboard…</p></Card>
					)}
				</div>
			)}

			{/* ── Catalog shortcut card ── */}
			<Card className='flex items-center justify-between gap-4'>
				<div>
					<h2 className='text-base font-semibold text-slate-900'>Catalog &amp; Inventory</h2>
					<p className='text-sm text-slate-500'>Manage items, stock levels and pricing</p>
				</div>
				<Link
					href='/admin/catalog'
					className='rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700 transition whitespace-nowrap'>
					Go to Catalog →
				</Link>
			</Card>

			{/* ── Orders tab ── */}
			{activeTab === "orders" && (
				<div className='space-y-4'>
					<Card className='space-y-4'>
						<div className='flex items-center justify-between'>
							<h2 className='text-lg font-semibold text-slate-900'>All Orders</h2>
							<Button variant='secondary' onClick={loadData}>Refresh</Button>
						</div>
						{orders.length === 0 ? (
							<p className='text-sm text-slate-500'>No orders yet.</p>
						) : (
							<div className='overflow-x-auto'>
								<table className='min-w-full text-left text-sm'>
									<thead>
										<tr className='border-b border-brand-100 text-brand-800'>
											<th className='py-2 pr-4'>Reference</th>
											<th className='py-2 pr-4'>Item ID</th>
											<th className='py-2 pr-4'>Item Name</th>
											<th className='py-2 pr-4'>Qty</th>
											<th className='py-2 pr-4'>Amount</th>
											<th className='py-2 pr-4'>Method</th>
											<th className='py-2 pr-4'>Status</th>
											<th className='py-2 pr-4'>Checkout</th>
											<th className='py-2 pr-4'>Date</th>
										</tr>
									</thead>
									<tbody>
										{orders.map((order) => (
											<tr key={order.id} className='border-b border-brand-50 text-slate-700'>
												<td className='py-2 pr-4 font-mono text-xs'>{order.reference || order.id?.slice(0, 8)}</td>
												<td className='py-2 pr-4 font-mono text-xs'>{order.itemId}</td>
												<td className='py-2 pr-4'>{order.itemName || "—"}</td>
												<td className='py-2 pr-4'>{order.quantity}</td>
												<td className='py-2 pr-4'>{formatPrice(order.amount)}</td>
												<td className='py-2 pr-4'>{order.paymentMethod || "—"}</td>
												<td className='py-2 pr-4'>
													<span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
														order.status === "COMPLETED" ? "bg-green-100 text-green-700" :
														order.status === "FAILED" ? "bg-red-100 text-red-700" :
														"bg-yellow-100 text-yellow-700"
													}`}>
														{order.status}
													</span>
												</td>
												<td className='py-2 pr-4'>
													<span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
														order.isSuccessCheckout ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-600"
													}`}>
														{order.isSuccessCheckout ? "✓ Confirmed" : "Pending"}
													</span>
												</td>
												<td className='py-2 pr-4 text-xs text-slate-500'>
													{order.createdAt ? new Date(order.createdAt).toLocaleDateString() : "—"}
												</td>
											</tr>
										))}
									</tbody>
								</table>
							</div>
						)}
					</Card>

					<Card className='space-y-4'>
						<h2 className='text-lg font-semibold text-brand-800'>Update Order Status</h2>
						<form className='grid gap-3 sm:grid-cols-3' onSubmit={handleOrderStatus}>
							<Input label='Order ID' value={orderId} onChange={(event) => setOrderId(event.target.value)} required />
							<label className='block space-y-1.5'>
								<span className='text-sm font-medium text-slate-700'>Status</span>
								<select
									value={orderStatus}
									onChange={(event) => setOrderStatus(event.target.value)}
									className='w-full rounded-xl border border-brand-200 bg-white px-3.5 py-2.5 text-slate-900 outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-200'>
									<option value='PREPARING'>PREPARING</option>
									<option value='READY'>READY</option>
									<option value='DELIVERED'>DELIVERED</option>
									<option value='CANCELLED'>CANCELLED</option>
								</select>
							</label>
							<div className='flex items-end'>
								<Button type='submit' className='w-full'>Update Status</Button>
							</div>
						</form>
					</Card>
				</div>
			)}

			{/* ── Users tab ── */}
			{activeTab === "users" && (
				<Card className='space-y-4'>
					<h2 className='text-lg font-semibold text-brand-800'>Users</h2>
					<div className='overflow-x-auto'>
						<table className='min-w-full text-left text-sm'>
							<thead>
								<tr className='border-b border-brand-100 text-brand-800'>
									<th className='py-2 pr-4'>Username</th>
									<th className='py-2 pr-4'>Role</th>
									<th className='py-2 pr-4'>Active</th>
									<th className='py-2 pr-4'>Action</th>
								</tr>
							</thead>
							<tbody>
								{users.map((user) => (
									<tr key={user.id || user.username} className='border-b border-brand-50 text-slate-700'>
										<td className='py-2 pr-4'>{user.username}</td>
										<td className='py-2 pr-4'>{user.role}</td>
										<td className='py-2 pr-4'>{user.active ? "Yes" : "No"}</td>
										<td className='py-2 pr-4'>
											<Button variant='secondary' onClick={() => handleToggleUser(user)}>
												{user.active ? "Deactivate" : "Activate"}
											</Button>
										</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				</Card>
			)}
		</div>
	);
}

