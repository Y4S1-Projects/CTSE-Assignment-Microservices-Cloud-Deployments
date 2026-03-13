"use client";

import { useEffect, useState } from "react";
import Button from "@/components/common/Button";
import Card from "@/components/common/Card";
import { getAllUsers, logoutUser } from "@/lib/authService";

export default function AdminPage() {
	const [users, setUsers] = useState([]);
	const [error, setError] = useState("");

	useEffect(() => {
		async function loadUsers() {
			try {
				const data = await getAllUsers();
				setUsers(Array.isArray(data) ? data : data?.users || []);
			} catch (loadError) {
				setError(loadError.message);
			}
		}

		loadUsers();
	}, []);

	async function handleLogout() {
		await logoutUser();
		window.location.href = "/auth/login";
	}

	return (
		<div className='space-y-6'>
			<Card>
				<h1 className='text-2xl font-bold text-slate-900'>Admin Dashboard</h1>
				<p className='mt-2 text-sm text-slate-600'>
					This area is separated for admin-side operations such as user listing and management.
				</p>
			</Card>

			<Card className='space-y-4'>
				<h2 className='text-lg font-semibold text-brand-800'>Users</h2>
				{error ?
					<p className='text-sm text-red-600'>{error}</p>
				:	null}
				{users.length === 0 ?
					<p className='text-sm text-slate-500'>No users found or loading...</p>
				:	<div className='overflow-x-auto'>
						<table className='min-w-full text-left text-sm'>
							<thead>
								<tr className='border-b border-brand-100 text-brand-800'>
									<th className='py-2 pr-4'>Username</th>
									<th className='py-2 pr-4'>Email</th>
									<th className='py-2 pr-4'>Role</th>
									<th className='py-2 pr-4'>Status</th>
								</tr>
							</thead>
							<tbody>
								{users.map((user) => (
									<tr key={user.id || user.username} className='border-b border-brand-50 text-slate-700'>
										<td className='py-2 pr-4'>{user.username || "N/A"}</td>
										<td className='py-2 pr-4'>{user.email || "N/A"}</td>
										<td className='py-2 pr-4'>{user.role || "N/A"}</td>
										<td className='py-2 pr-4'>{user.status || "N/A"}</td>
									</tr>
								))}
							</tbody>
						</table>
					</div>
				}
			</Card>

			<Button onClick={handleLogout}>Logout</Button>
		</div>
	);
}
