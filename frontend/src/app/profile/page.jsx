"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Card from "@/components/common/Card";
import Button from "@/components/common/Button";
import Input from "@/components/common/Input";
import { getMyProfile, updateMyProfile } from "@/lib/authService";
import { getAuthToken, getCurrentUser, isAdminUser } from "@/lib/storage";

function roleBadgeStyle(role) {
	return (role || "").toUpperCase() === "ADMIN" ? "bg-brand-100 text-brand-800" : "bg-slate-100 text-slate-700";
}

export default function ProfilePage() {
	const router = useRouter();
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState("");
	const [success, setSuccess] = useState("");
	const [profile, setProfile] = useState(null);
	const [profileForm, setProfileForm] = useState({ fullName: "", email: "" });
	const [savingProfile, setSavingProfile] = useState(false);

	const [addresses, setAddresses] = useState([]);
	const [editingAddressId, setEditingAddressId] = useState(null);
	const [savingAddress, setSavingAddress] = useState(false);
	const [addressForm, setAddressForm] = useState({
		label: "",
		entryMode: "manual",
		addressLine1: "",
		addressLine2: "",
		city: "",
		state: "",
		postalCode: "",
		country: "",
		formattedAddress: "",
		latitude: "",
		longitude: "",
		googlePlaceId: "",
		isDefault: false,
	});

	useEffect(() => {
		if (!getAuthToken()) {
			router.replace("/auth/login");
			return;
		}

		async function loadProfile() {
			setLoading(true);
			setError("");
			try {
				const serverProfile = await getMyProfile();
				setProfile(serverProfile);
				setProfileForm({
					fullName: serverProfile?.fullName || "",
					email: serverProfile?.email || "",
				});
				const apiAddresses = Array.isArray(serverProfile?.addresses) ? serverProfile.addresses : [];
				setAddresses(apiAddresses);
			} catch (loadError) {
				const fallback = getCurrentUser();
				if (fallback) {
					const fallbackProfile = {
						id: fallback.userId,
						email: fallback.email,
						role: fallback.role,
						fullName: fallback.email,
						active: true,
					};
					setProfile(fallbackProfile);
					setProfileForm({
						fullName: fallbackProfile.fullName || "",
						email: fallbackProfile.email || "",
					});
				} else {
					setError(loadError.message || "Failed to load profile");
				}
			} finally {
				setLoading(false);
			}
		}

		loadProfile();
	}, [router]);

	const dashboardPath = useMemo(() => (isAdminUser() ? "/admin" : "/customer"), []);

	function toAddressPayload(list) {
		return list.map((address) => ({
			id: address.id,
			label: address.label,
			addressLine1: address.addressLine1 || address.street,
			addressLine2: address.addressLine2,
			street: address.addressLine1 || address.street,
			city: address.city,
			state: address.state,
			postalCode: address.postalCode,
			country: address.country,
			formattedAddress: address.formattedAddress,
			latitude: address.latitude,
			longitude: address.longitude,
			googlePlaceId: address.googlePlaceId,
			locationSource: address.locationSource,
			isDefault: Boolean(address.isDefault),
		}));
	}

	async function persistProfileAddresses(nextAddresses, successMessage) {
		const updated = await updateMyProfile({
			fullName: profileForm.fullName,
			email: profileForm.email,
			addresses: toAddressPayload(nextAddresses),
		});
		setProfile(updated);
		setAddresses(Array.isArray(updated?.addresses) ? updated.addresses : []);
		setSuccess(successMessage);
	}

	async function handleProfileSave(event) {
		event.preventDefault();
		setError("");
		setSuccess("");
		setSavingProfile(true);
		try {
			const updated = await updateMyProfile({
				fullName: profileForm.fullName,
				email: profileForm.email,
				addresses: toAddressPayload(addresses),
			});
			setProfile(updated);
			setAddresses(Array.isArray(updated?.addresses) ? updated.addresses : []);
			setSuccess("Profile updated successfully.");
		} catch (saveError) {
			setError(saveError.message || "Failed to update profile");
		} finally {
			setSavingProfile(false);
		}
	}

	function resetAddressForm() {
		setEditingAddressId(null);
		setAddressForm({
			label: "",
			entryMode: "manual",
			addressLine1: "",
			addressLine2: "",
			city: "",
			state: "",
			postalCode: "",
			country: "",
			formattedAddress: "",
			latitude: "",
			longitude: "",
			googlePlaceId: "",
			isDefault: false,
		});
	}

	function startEditAddress(address) {
		setEditingAddressId(address.id);
		setAddressForm({
			label: address.label || "",
			entryMode: (address.locationSource || "MANUAL").toLowerCase() === "map" ? "map" : "manual",
			addressLine1: address.addressLine1 || address.street || "",
			addressLine2: address.addressLine2 || "",
			city: address.city || "",
			state: address.state || "",
			postalCode: address.postalCode || "",
			country: address.country || "",
			formattedAddress: address.formattedAddress || "",
			latitude: address.latitude ?? "",
			longitude: address.longitude ?? "",
			googlePlaceId: address.googlePlaceId || "",
			isDefault: Boolean(address.isDefault),
		});
	}

	async function handleSaveAddress(event) {
		event.preventDefault();
		setError("");
		setSuccess("");
		setSavingAddress(true);

		const isMapEntry = addressForm.entryMode === "map";
		const payload = {
			id: editingAddressId || undefined,
			label: addressForm.label,
			addressLine1: addressForm.addressLine1,
			addressLine2: addressForm.addressLine2,
			street: addressForm.addressLine1,
			city: addressForm.city,
			state: addressForm.state,
			postalCode: addressForm.postalCode,
			country: addressForm.country,
			isDefault: addressForm.isDefault,
			locationSource: isMapEntry ? "MAP" : "MANUAL",
			formattedAddress: isMapEntry ? addressForm.formattedAddress : undefined,
			googlePlaceId: isMapEntry ? addressForm.googlePlaceId : undefined,
			latitude: isMapEntry && addressForm.latitude !== "" ? Number(addressForm.latitude) : undefined,
			longitude: isMapEntry && addressForm.longitude !== "" ? Number(addressForm.longitude) : undefined,
		};

		try {
			let next;
			if (editingAddressId) {
				next = addresses.map((address) => (address.id === editingAddressId ? { ...address, ...payload } : address));
			} else {
				if (addresses.length >= 3) {
					throw new Error("You can save up to 3 addresses only.");
				}
				next = [...addresses, { ...payload }];
			}

			if (payload.isDefault) {
				next = next.map((address, index) => {
					const isTarget = editingAddressId ? address.id === editingAddressId : index === next.length - 1;
					return { ...address, isDefault: isTarget };
				});
			}

			if (next.length > 0 && !next.some((address) => address.isDefault)) {
				next[0] = { ...next[0], isDefault: true };
			}

			await persistProfileAddresses(
				next,
				editingAddressId ? "Address updated successfully." : "Address added successfully.",
			);
			resetAddressForm();
		} catch (saveError) {
			setError(saveError.message || "Failed to save address");
		} finally {
			setSavingAddress(false);
		}
	}

	async function handleDeleteAddress(id) {
		setError("");
		setSuccess("");
		try {
			const next = addresses.filter((address) => address.id !== id);
			if (next.length > 0 && !next.some((address) => address.isDefault)) {
				next[0] = { ...next[0], isDefault: true };
			}
			await persistProfileAddresses(next, "Address removed successfully.");
			if (editingAddressId === id) {
				resetAddressForm();
			}
		} catch (deleteError) {
			setError(deleteError.message || "Failed to delete address");
		}
	}

	if (loading) {
		return (
			<Card className='max-w-2xl mx-auto mt-10'>
				<p className='text-sm text-slate-600'>Loading your profile...</p>
			</Card>
		);
	}

	if (error) {
		return (
			<Card className='max-w-2xl mx-auto mt-10 space-y-4'>
				<p className='text-sm text-red-600'>{error}</p>
				<Button onClick={() => router.push(dashboardPath)}>Back to dashboard</Button>
			</Card>
		);
	}

	return (
		<div className='max-w-3xl mx-auto space-y-6'>
			<Card className='relative overflow-hidden'>
				<div className='absolute inset-x-0 top-0 h-20 bg-brand-600/10' />
				<div className='relative flex flex-wrap items-start justify-between gap-4'>
					<div className='space-y-1'>
						<h1 className='text-2xl font-bold text-slate-900'>My Profile</h1>
						<p className='text-sm text-slate-600'>Account details and role information</p>
					</div>
					<span className={`rounded-full px-3 py-1 text-xs font-semibold ${roleBadgeStyle(profile?.role)}`}>
						{(profile?.role || "CUSTOMER").toUpperCase()}
					</span>
				</div>
			</Card>

			<Card className='space-y-4'>
				<h2 className='text-lg font-semibold text-slate-900'>Edit Profile</h2>
				<form onSubmit={handleProfileSave} className='grid gap-4 sm:grid-cols-2'>
					<Input
						label='Full Name'
						value={profileForm.fullName}
						onChange={(event) => setProfileForm((prev) => ({ ...prev, fullName: event.target.value }))}
					/>
					<Input
						label='Email'
						type='email'
						value={profileForm.email}
						onChange={(event) => setProfileForm((prev) => ({ ...prev, email: event.target.value }))}
						required
					/>
					<div className='rounded-xl border border-brand-100 bg-slate-50 px-3.5 py-2.5'>
						<p className='text-sm font-medium text-slate-700'>Role</p>
						<p className='text-sm text-slate-600'>{profile?.role || "-"} (read-only)</p>
					</div>
					<div className='rounded-xl border border-brand-100 bg-slate-50 px-3.5 py-2.5'>
						<p className='text-sm font-medium text-slate-700'>Status</p>
						<p className='text-sm text-slate-600'>{profile?.active ? "Active" : "Inactive"}</p>
					</div>
					<div className='sm:col-span-2'>
						<Button type='submit' disabled={savingProfile}>
							{savingProfile ? "Saving..." : "Save Profile"}
						</Button>
					</div>
				</form>
			</Card>

			<Card className='space-y-4'>
				<h2 className='text-lg font-semibold text-slate-900'>Addresses</h2>
				<form onSubmit={handleSaveAddress} className='space-y-4'>
					<div className='grid gap-4 sm:grid-cols-2'>
						<Input
							label='Address Label (Home, Work, etc.)'
							value={addressForm.label}
							onChange={(event) => setAddressForm((prev) => ({ ...prev, label: event.target.value }))}
						/>
						<label className='block space-y-1.5'>
							<span className='text-sm font-medium text-slate-700'>Entry Mode</span>
							<select
								value={addressForm.entryMode}
								onChange={(event) => setAddressForm((prev) => ({ ...prev, entryMode: event.target.value }))}
								className='w-full rounded-xl border border-brand-200 bg-white px-3.5 py-2.5 text-slate-900 outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-200'>
								<option value='manual'>Manual Address</option>
								<option value='map'>Map Selection (Google Maps ready)</option>
							</select>
						</label>
						<Input
							label='Address Line 1'
							value={addressForm.addressLine1}
							onChange={(event) => setAddressForm((prev) => ({ ...prev, addressLine1: event.target.value }))}
						/>
						<Input
							label='Address Line 2'
							value={addressForm.addressLine2}
							onChange={(event) => setAddressForm((prev) => ({ ...prev, addressLine2: event.target.value }))}
						/>
						<Input
							label='City'
							value={addressForm.city}
							onChange={(event) => setAddressForm((prev) => ({ ...prev, city: event.target.value }))}
						/>
						<Input
							label='State / Province'
							value={addressForm.state}
							onChange={(event) => setAddressForm((prev) => ({ ...prev, state: event.target.value }))}
						/>
						<Input
							label='Postal Code'
							value={addressForm.postalCode}
							onChange={(event) => setAddressForm((prev) => ({ ...prev, postalCode: event.target.value }))}
						/>
						<Input
							label='Country'
							value={addressForm.country}
							onChange={(event) => setAddressForm((prev) => ({ ...prev, country: event.target.value }))}
						/>
						{addressForm.entryMode === "map" ?
							<>
								<Input
									label='Formatted Address (from map)'
									value={addressForm.formattedAddress}
									onChange={(event) => setAddressForm((prev) => ({ ...prev, formattedAddress: event.target.value }))}
								/>
								<Input
									label='Google place details'
									value={addressForm.googlePlaceId}
									onChange={(event) => setAddressForm((prev) => ({ ...prev, googlePlaceId: event.target.value }))}
								/>
								<Input
									label='Latitude'
									type='number'
									step='any'
									value={addressForm.latitude}
									onChange={(event) => setAddressForm((prev) => ({ ...prev, latitude: event.target.value }))}
								/>
								<Input
									label='Longitude'
									type='number'
									step='any'
									value={addressForm.longitude}
									onChange={(event) => setAddressForm((prev) => ({ ...prev, longitude: event.target.value }))}
								/>
							</>
						:	null}
					</div>
					<label className='flex items-center gap-2 text-sm text-slate-700'>
						<input
							type='checkbox'
							checked={addressForm.isDefault}
							onChange={(event) => setAddressForm((prev) => ({ ...prev, isDefault: event.target.checked }))}
						/>
						Set as default address
					</label>
					<div className='flex flex-wrap gap-2'>
						<Button type='submit' disabled={savingAddress}>
							{savingAddress ?
								"Saving..."
							: editingAddressId ?
								"Update Address"
							:	"Add Address"}
						</Button>
						{editingAddressId ?
							<Button type='button' variant='secondary' onClick={resetAddressForm}>
								Cancel Edit
							</Button>
						:	null}
					</div>
				</form>

				<div className='space-y-3'>
					{addresses.length === 0 ?
						<p className='text-sm text-slate-500'>No addresses added yet.</p>
					:	addresses.map((address) => (
							<div key={address.id} className='p-4 border rounded-xl border-brand-100'>
								<p className='font-medium text-slate-900'>
									{address.label || "Address"} {address.isDefault ? "(Default)" : ""}
								</p>
								<p className='text-sm text-slate-600'>
									{address.formattedAddress ||
										[
											address.addressLine1 || address.street,
											address.addressLine2,
											address.city,
											address.state,
											address.postalCode,
											address.country,
										]
											.filter(Boolean)
											.join(", ")}
								</p>
								<div className='flex gap-2 mt-2'>
									<Button type='button' variant='secondary' onClick={() => startEditAddress(address)}>
										Edit
									</Button>
									<Button type='button' variant='ghost' onClick={() => handleDeleteAddress(address.id)}>
										Delete
									</Button>
								</div>
							</div>
						))
					}
				</div>
			</Card>

			{error ?
				<p className='text-sm text-red-600'>{error}</p>
			:	null}
			{success ?
				<p className='text-sm text-green-700'>{success}</p>
			:	null}

			<div className='flex flex-wrap gap-3'>
				<Link href={dashboardPath}>
					<Button>Back to Dashboard</Button>
				</Link>
			</div>
		</div>
	);
}
