"use client";

import { useEffect, useState } from "react";
import Button from "@/components/common/Button";
import Card from "@/components/common/Card";
import { getMyProfile, logoutUser } from "@/lib/authService";

export default function CustomerPage() {
	const [profile, setProfile] = useState(null);
	const [error, setError] = useState("");

	useEffect(() => {
		async function loadProfile() {
			try {
				const data = await getMyProfile();
				setProfile(data);
			} catch (loadError) {
				setError(loadError.message);
			}
		}

		loadProfile();
	}, []);

	async function handleLogout() {
		await logoutUser();
		window.location.href = "/auth/login";
	}

	return (
		<div className='space-y-6'>
			<Card>
				<h1 className='text-2xl font-bold text-slate-900'>Customer Dashboard</h1>
				<p className='mt-2 text-sm text-slate-600'>
					This area is for customer-side features, profile access, and personal settings.
				</p>
			</Card>

			<Card className='space-y-3'>
				<h2 className='text-lg font-semibold text-brand-800'>My Profile</h2>
				{error ?
					<p className='text-sm text-red-600'>{error}</p>
				:	null}
				{profile ?
					<div className='grid gap-2 text-sm text-slate-700 sm:grid-cols-2'>
						<p>
							<span className='font-medium'>Username:</span> {profile.username || "N/A"}
						</p>
						<p>
							<span className='font-medium'>Email:</span> {profile.email || "N/A"}
						</p>
						<p>
							<span className='font-medium'>First Name:</span> {profile.firstName || "N/A"}
						</p>
						<p>
							<span className='font-medium'>Last Name:</span> {profile.lastName || "N/A"}
						</p>
					</div>
				:	<p className='text-sm text-slate-500'>Loading profile...</p>}
			</Card>

			<Button onClick={handleLogout}>Logout</Button>
		</div>
	);
}
