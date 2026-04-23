"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Button from "@/components/common/Button";
import Card from "@/components/common/Card";
import Input from "@/components/common/Input";
import { getAllUsers, updateUserDetails, updateUserStatus } from "@/lib/authService";
import { getAuthToken, isAdminUser } from "@/lib/storage";

export default function AdminUsersPage() {
	const router = useRouter();
	const [users, setUsers] = useState([]);
	const [error, setError] = useState("");
	const [success, setSuccess] = useState("");
	const [authorized, setAuthorized] = useState(false);
	const [editingUserId, setEditingUserId] = useState(null);
	const [userEditForm, setUserEditForm] = useState({
		fullName: "",
		email: "",
		role: "CUSTOMER",
		active: true,
	});
	const [savingUser, setSavingUser] = useState(false);

	async function loadUsers() {
		setError("");
		try {
			const usersData = await getAllUsers();
			setUsers(Array.isArray(usersData) ? usersData : []);
		} catch (loadError) {
			setError(loadError.message || "Failed to load users");
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
		loadUsers();
	}, [router]);

	async function handleToggleUser(user) {
		setError("");
		setSuccess("");
		try {
			await updateUserStatus(user.id, !user.active);
			setSuccess(`Updated status for ${user.email}`);
			await loadUsers();
		} catch (err) {
			setError(err.message || "Failed to update user status");
		}
	}

	function startEditUser(user) {
		setEditingUserId(user.id);
		setUserEditForm({
			fullName: user.fullName || "",
			email: user.email || "",
			role: user.role || "CUSTOMER",
			active: Boolean(user.active),
		});
	}

	function cancelEditUser() {
		setEditingUserId(null);
		setUserEditForm({
			fullName: "",
			email: "",
			role: "CUSTOMER",
			active: true,
		});
	}

	async function handleSaveUser(event) {
		event.preventDefault();
		if (!editingUserId) return;

		setError("");
		setSuccess("");
		setSavingUser(true);
		try {
			await updateUserDetails(editingUserId, {
				fullName: userEditForm.fullName,
				email: userEditForm.email,
				role: userEditForm.role,
				active: userEditForm.active,
			});
			setSuccess("User updated successfully.");
			cancelEditUser();
			await loadUsers();
		} catch (err) {
			setError(err.message || "Failed to update user");
		} finally {
			setSavingUser(false);
		}
	}

	if (!authorized) {
		return (
			<Card className='max-w-xl mx-auto mt-10'>
				<p className='text-sm text-slate-600'>Checking admin access...</p>
			</Card>
		);
	}

	return (
		<div className='space-y-6'>
			{/* Header */}
			<Card className='space-y-2'>
				<h1 className='text-2xl font-bold text-slate-900'>User Management</h1>
				<p className='text-sm text-slate-600'>Manage user accounts, roles, and access permissions</p>
				{error ?
					<p className='text-sm text-red-600'>{error}</p>
				:	null}
				{success ?
					<p className='text-sm text-green-700'>{success}</p>
				:	null}
			</Card>

			{/* Users Section */}
			<Card className='space-y-4'>
				<div className='flex items-center justify-between'>
					<h2 className='text-lg font-semibold text-slate-900'>All Users</h2>
					<Button variant='secondary' onClick={loadUsers}>
						Refresh
					</Button>
				</div>

				{/* Edit Form */}
				{editingUserId ?
					<form onSubmit={handleSaveUser} className='grid gap-3 p-4 border rounded-xl border-brand-100 sm:grid-cols-2'>
						<Input
							label='Full Name'
							value={userEditForm.fullName}
							onChange={(event) =>
								setUserEditForm((prev) => ({
									...prev,
									fullName: event.target.value,
								}))
							}
						/>
						<Input
							label='Email'
							type='email'
							value={userEditForm.email}
							onChange={(event) =>
								setUserEditForm((prev) => ({
									...prev,
									email: event.target.value,
								}))
							}
							required
						/>
						<label className='block space-y-1.5'>
							<span className='text-sm font-medium text-slate-700'>Role</span>
							<select
								value={userEditForm.role}
								onChange={(event) =>
									setUserEditForm((prev) => ({
										...prev,
										role: event.target.value,
									}))
								}
								className='w-full rounded-xl border border-brand-200 bg-white px-3.5 py-2.5 text-slate-900 outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-200'>
								<option value='CUSTOMER'>CUSTOMER</option>
								<option value='ADMIN'>ADMIN</option>
							</select>
						</label>
						<label className='flex items-center gap-2 text-sm text-slate-700 sm:pt-8'>
							<input
								type='checkbox'
								checked={userEditForm.active}
								onChange={(event) =>
									setUserEditForm((prev) => ({
										...prev,
										active: event.target.checked,
									}))
								}
							/>
							Active user
						</label>
						<div className='flex gap-2 sm:col-span-2'>
							<Button type='submit' disabled={savingUser}>
								{savingUser ? "Saving..." : "Save User"}
							</Button>
							<Button type='button' variant='secondary' onClick={cancelEditUser}>
								Cancel
							</Button>
						</div>
					</form>
				:	null}

				{/* Users Table */}
				{users.length === 0 ?
					<p className='text-sm text-slate-500'>No users found.</p>
				:	<div className='overflow-x-auto'>
						<table className='min-w-full text-sm text-left'>
							<thead>
								<tr className='border-b border-brand-100 text-brand-800'>
									<th className='py-2 pr-4'>Full Name</th>
									<th className='py-2 pr-4'>Email</th>
									<th className='py-2 pr-4'>Role</th>
									<th className='py-2 pr-4'>Active</th>
									<th className='py-2 pr-4'>Actions</th>
								</tr>
							</thead>
							<tbody>
								{users.map((user) => (
									<tr key={user.id || user.email} className='border-b border-brand-50 text-slate-700'>
										<td className='py-2 pr-4'>{user.fullName || "-"}</td>
										<td className='py-2 pr-4'>{user.email}</td>
										<td className='py-2 pr-4'>
											<span
												className={`px-2 py-1 rounded text-xs font-semibold ${
													user.role === "ADMIN" ? "bg-purple-100 text-purple-700" : "bg-blue-100 text-blue-700"
												}`}>
												{user.role}
											</span>
										</td>
										<td className='py-2 pr-4'>
											<span className={user.active ? "text-green-600" : "text-red-600"}>
												{user.active ? "✓ Yes" : "✗ No"}
											</span>
										</td>
										<td className='flex gap-2 py-2 pr-4'>
											<Button variant='secondary' onClick={() => startEditUser(user)}>
												Edit
											</Button>
											<Button variant='secondary' onClick={() => handleToggleUser(user)}>
												{user.active ? "Deactivate" : "Activate"}
											</Button>
										</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				}
			</Card>
		</div>
	);
}
