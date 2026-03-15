"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Button from "@/components/common/Button";
import Card from "@/components/common/Card";
import Input from "@/components/common/Input";
import {
	createCatalogItem,
	deleteCatalogItem,
	getCatalogDashboard,
	getMenuItems,
	updateCatalogItem,
	updateCatalogStock,
} from "@/lib/foodService";
import { getAuthToken, isAdminUser } from "@/lib/storage";

function formatPrice(value) {
	return `$${Number(value || 0).toFixed(2)}`;
}

const emptyForm = {
	name: "",
	description: "",
	price: "",
	category: "",
	stockCount: "",
	imageUrl: "",
	itemId: "",
};

export default function AdminCatalogPage() {
	const router = useRouter();
	const [authorized, setAuthorized] = useState(false);
	const [activeTab, setActiveTab] = useState("inventory");

	const [items, setItems] = useState([]);
	const [dashboard, setDashboard] = useState(null);
	const [editingStock, setEditingStock] = useState({});
	const [editingItem, setEditingItem] = useState(null); // item being edited inline
	const [editForm, setEditForm] = useState({});

	const [itemForm, setItemForm] = useState(emptyForm);
	const [saving, setSaving] = useState(false);
	const [error, setError] = useState("");
	const [success, setSuccess] = useState("");

	async function loadData() {
		setError("");
		try {
			const [menuData, dashData] = await Promise.all([
				getMenuItems(),
				getCatalogDashboard().catch(() => null),
			]);
			setItems(Array.isArray(menuData) ? menuData : []);
			setDashboard(dashData);
		} catch (e) {
			setError(e.message || "Failed to load catalog data");
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

	// ── Add Item ──────────────────────────────────────────────────────────────

	async function handleAddItem(e) {
		e.preventDefault();
		setSaving(true);
		setError("");
		setSuccess("");
		try {
			await createCatalogItem({
				...itemForm,
				price: parseFloat(itemForm.price),
				stockCount: parseInt(itemForm.stockCount, 10),
			});
			setSuccess(`"${itemForm.name}" added to catalog.`);
			setItemForm(emptyForm);
			await loadData();
		} catch (e) {
			setError(e.message || "Failed to add item");
		} finally {
			setSaving(false);
		}
	}

	// ── Stock edit ────────────────────────────────────────────────────────────

	async function handleSaveStock(item) {
		const newStock = parseInt(editingStock[item.id], 10);
		if (isNaN(newStock) || newStock < 0) {
			setError("Stock must be a non-negative number");
			return;
		}
		setError("");
		setSuccess("");
		try {
			await updateCatalogStock(item.id, { stockCount: newStock });
			setSuccess(`Stock for "${item.name}" updated to ${newStock}`);
			setEditingStock((prev) => { const n = { ...prev }; delete n[item.id]; return n; });
			await loadData();
		} catch (e) {
			setError(e.message || "Failed to update stock");
		}
	}

	// ── Inline item edit ─────────────────────────────────────────────────────

	function startEditItem(item) {
		setEditingItem(item.id);
		setEditForm({
			name: item.name || "",
			description: item.description || "",
			price: item.price || "",
			category: item.category || "",
			imageUrl: item.imageUrl || "",
		});
	}

	async function handleSaveItem(item) {
		setError("");
		setSuccess("");
		try {
			await updateCatalogItem(item.id, {
				...editForm,
				price: parseFloat(editForm.price),
			});
			setSuccess(`"${editForm.name}" updated.`);
			setEditingItem(null);
			await loadData();
		} catch (e) {
			setError(e.message || "Failed to update item");
		}
	}

	// ── Delete ────────────────────────────────────────────────────────────────

	async function handleDelete(item) {
		if (!confirm(`Delete "${item.name}"? This cannot be undone.`)) return;
		setError("");
		setSuccess("");
		try {
			await deleteCatalogItem(item.id);
			setSuccess(`"${item.name}" deleted.`);
			await loadData();
		} catch (e) {
			setError(e.message || "Failed to delete item");
		}
	}

	if (!authorized) {
		return (
			<Card className='mx-auto mt-10 max-w-xl'>
				<p className='text-sm text-slate-600'>Checking admin access…</p>
			</Card>
		);
	}

	const tabs = [
		{ id: "inventory", label: "Inventory" },
		{ id: "add-item", label: "Add Item" },
	];

	return (
		<div className='space-y-6'>
			{/* ── Header ── */}
			<Card className='space-y-2'>
				<div className='flex flex-wrap items-center justify-between gap-3'>
					<div>
						<h1 className='text-2xl font-bold text-slate-900'>Catalog &amp; Stock Management</h1>
						<p className='text-sm text-slate-600'>Admin only — manage items, categories and stock levels</p>
					</div>
					<div className='flex gap-2'>
						<Link
							href='/admin'
							className='rounded-xl border border-brand-200 px-4 py-2 text-sm font-medium text-brand-700 hover:bg-brand-50 transition'>
							← Back to Admin
						</Link>
					</div>
				</div>
				{error ? <p className='text-sm text-red-600'>{error}</p> : null}
				{success ? <p className='text-sm text-green-700'>{success}</p> : null}
			</Card>

			{/* ── Dashboard stat cards ── */}
			{dashboard && (
				<div className='grid grid-cols-2 gap-4 sm:grid-cols-4'>
					{[
						{ label: "Total Items",    value: dashboard.totalItems,     color: "bg-blue-50 text-blue-700" },
						{ label: "In Stock",       value: dashboard.availableItems,  color: "bg-green-50 text-green-700" },
						{ label: "Out of Stock",   value: dashboard.outOfStockItems, color: "bg-red-50 text-red-700" },
						{ label: "Low Stock (≤5)", value: dashboard.lowStockItems,   color: "bg-yellow-50 text-yellow-700" },
					].map((s) => (
						<Card key={s.label} className={`text-center ${s.color}`}>
							<p className='text-3xl font-bold'>{s.value}</p>
							<p className='mt-1 text-xs font-medium'>{s.label}</p>
						</Card>
					))}
				</div>
			)}

			{/* ── Tabs ── */}
			<div className='flex gap-2'>
				{tabs.map((t) => (
					<button
						key={t.id}
						type='button'
						onClick={() => setActiveTab(t.id)}
						className={`rounded-full px-4 py-1.5 text-sm font-semibold transition ${
							activeTab === t.id ? "bg-brand-600 text-white" : "bg-brand-100 text-brand-800 hover:bg-brand-200"
						}`}>
						{t.label}
					</button>
				))}
				<button
					type='button'
					onClick={loadData}
					className='ml-auto rounded-full bg-slate-100 px-4 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-200 transition'>
					Refresh
				</button>
			</div>

			{/* ── Inventory tab ── */}
			{activeTab === "inventory" && (
				<Card className='space-y-4'>
					<h2 className='text-lg font-semibold text-slate-900'>All Catalog Items</h2>
					{items.length === 0 ? (
						<p className='text-sm text-slate-500'>No items yet. Add some using the "Add Item" tab.</p>
					) : (
						<div className='overflow-x-auto'>
							<table className='min-w-full text-left text-sm'>
								<thead>
									<tr className='border-b border-brand-100 text-brand-800'>
										<th className='py-2 pr-3'>Item ID</th>
										<th className='py-2 pr-3'>Name</th>
										<th className='py-2 pr-3'>Category</th>
										<th className='py-2 pr-3'>Price</th>
										<th className='py-2 pr-3'>Stock</th>
										<th className='py-2 pr-3'>Status</th>
										<th className='py-2 pr-3'>Actions</th>
									</tr>
								</thead>
								<tbody>
									{items.map((item) => (
										<tr key={item.id} className='border-b border-brand-50'>
											{editingItem === item.id ? (
												/* ── inline edit row ── */
												<>
													<td className='py-2 pr-3 font-mono text-xs text-slate-500'>{item.itemId || "—"}</td>
													<td className='py-2 pr-3'>
														<input className='w-32 rounded border px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-brand-400'
															value={editForm.name} onChange={(e) => setEditForm((f) => ({ ...f, name: e.target.value }))} />
													</td>
													<td className='py-2 pr-3'>
														<input className='w-24 rounded border px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-brand-400'
															value={editForm.category} onChange={(e) => setEditForm((f) => ({ ...f, category: e.target.value }))} />
													</td>
													<td className='py-2 pr-3'>
														<input type='number' step='0.01' min='0' className='w-20 rounded border px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-brand-400'
															value={editForm.price} onChange={(e) => setEditForm((f) => ({ ...f, price: e.target.value }))} />
													</td>
													<td className='py-2 pr-3'>
														<input type='number' min='0' className='w-16 rounded border px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-brand-400'
															value={editingStock[item.id] !== undefined ? editingStock[item.id] : item.stockCount}
															onChange={(e) => setEditingStock((p) => ({ ...p, [item.id]: e.target.value }))} />
													</td>
													<td className='py-2 pr-3'></td>
													<td className='py-2 pr-3 flex gap-1'>
														<button type='button' onClick={() => handleSaveItem(item)}
															className='rounded bg-brand-600 px-2 py-1 text-xs text-white hover:bg-brand-700'>Save</button>
														{editingStock[item.id] !== undefined && (
															<button type='button' onClick={() => handleSaveStock(item)}
																className='rounded bg-green-600 px-2 py-1 text-xs text-white hover:bg-green-700'>Stock</button>
														)}
														<button type='button' onClick={() => setEditingItem(null)}
															className='rounded bg-slate-200 px-2 py-1 text-xs text-slate-700 hover:bg-slate-300'>Cancel</button>
													</td>
												</>
											) : (
												/* ── view row ── */
												<>
													<td className='py-2 pr-3 font-mono text-xs text-slate-500'>{item.itemId || "—"}</td>
													<td className='py-2 pr-3 font-medium text-slate-900'>{item.name}</td>
													<td className='py-2 pr-3 text-slate-600'>{item.category}</td>
													<td className='py-2 pr-3'>{formatPrice(item.price)}</td>
													<td className='py-2 pr-3'>
														<div className='flex items-center gap-2'>
															<input type='number' min='0'
																value={editingStock[item.id] !== undefined ? editingStock[item.id] : item.stockCount}
																onChange={(e) => setEditingStock((p) => ({ ...p, [item.id]: e.target.value }))}
																className='w-16 rounded border border-slate-300 px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-brand-400' />
															{editingStock[item.id] !== undefined && (
																<button type='button' onClick={() => handleSaveStock(item)}
																	className='rounded bg-brand-600 px-2 py-1 text-xs text-white hover:bg-brand-700'>Save</button>
															)}
														</div>
													</td>
													<td className='py-2 pr-3'>
														<span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
															item.stockCount === 0 ? "bg-red-100 text-red-700" :
															item.stockCount <= 5 ? "bg-yellow-100 text-yellow-700" :
															"bg-green-100 text-green-700"
														}`}>
															{item.stockCount === 0 ? "Out of stock" : item.stockCount <= 5 ? "Low" : "In stock"}
														</span>
													</td>
													<td className='py-2 pr-3'>
														<div className='flex gap-1'>
															<button type='button' onClick={() => startEditItem(item)}
																className='rounded bg-brand-100 px-2 py-1 text-xs text-brand-700 hover:bg-brand-200'>Edit</button>
															<button type='button' onClick={() => handleDelete(item)}
																className='rounded bg-red-100 px-2 py-1 text-xs text-red-700 hover:bg-red-200'>Delete</button>
														</div>
													</td>
												</>
											)}
										</tr>
									))}
								</tbody>
							</table>
						</div>
					)}
				</Card>
			)}

			{/* ── Add Item tab ── */}
			{activeTab === "add-item" && (
				<Card className='space-y-4'>
					<h2 className='text-lg font-semibold text-slate-900'>Add New Item to Catalog</h2>
					<form className='grid gap-4 sm:grid-cols-2' onSubmit={handleAddItem}>
						<Input label='Item Name *' value={itemForm.name}
							onChange={(e) => setItemForm((f) => ({ ...f, name: e.target.value }))} required />
						<Input label='Category *' placeholder='e.g. Main, Drinks, Desserts' value={itemForm.category}
							onChange={(e) => setItemForm((f) => ({ ...f, category: e.target.value }))} required />
						<Input label='Price ($) *' type='number' step='0.01' min='0' value={itemForm.price}
							onChange={(e) => setItemForm((f) => ({ ...f, price: e.target.value }))} required />
						<Input label='Initial Stock *' type='number' min='0' value={itemForm.stockCount}
							onChange={(e) => setItemForm((f) => ({ ...f, stockCount: e.target.value }))} required />
						<Input label='Description' value={itemForm.description}
							onChange={(e) => setItemForm((f) => ({ ...f, description: e.target.value }))} />
						<Input label='Image URL' type='url' placeholder='https://...' value={itemForm.imageUrl}
							onChange={(e) => setItemForm((f) => ({ ...f, imageUrl: e.target.value }))} />
						<Input label='Custom Item ID (optional)' placeholder='e.g. ITEM-0010' value={itemForm.itemId}
							onChange={(e) => setItemForm((f) => ({ ...f, itemId: e.target.value }))} />
						<div className='flex items-end sm:col-span-2'>
							<Button type='submit' disabled={saving} className='w-full sm:w-auto'>
								{saving ? "Adding…" : "Add to Catalog"}
							</Button>
						</div>
					</form>
				</Card>
			)}
		</div>
	);
}
