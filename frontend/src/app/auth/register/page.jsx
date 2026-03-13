"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Button from "@/components/common/Button";
import Card from "@/components/common/Card";
import Input from "@/components/common/Input";
import { registerUser } from "@/lib/authService";

export default function RegisterPage() {
	const router = useRouter();
	const [form, setForm] = useState({
		firstName: "",
		lastName: "",
		username: "",
		email: "",
		password: "",
		phoneNumber: "",
	});
	const [error, setError] = useState("");
	const [success, setSuccess] = useState("");
	const [loading, setLoading] = useState(false);

	async function handleSubmit(event) {
		event.preventDefault();
		setError("");
		setSuccess("");
		setLoading(true);

		try {
			await registerUser(form);
			setSuccess("Registration successful. You can now sign in.");
			setTimeout(() => router.push("/auth/login"), 800);
		} catch (submitError) {
			setError(submitError.message);
		} finally {
			setLoading(false);
		}
	}

	return (
		<div className='mx-auto w-full max-w-2xl'>
			<Card className='space-y-5'>
				<h1 className='text-2xl font-bold text-slate-900'>Create Your Account</h1>
				<p className='text-sm text-slate-600'>Register as a customer account to access the platform.</p>
				<form onSubmit={handleSubmit} className='grid gap-4 sm:grid-cols-2'>
					<Input
						label='First Name'
						value={form.firstName}
						onChange={(event) => setForm((prev) => ({ ...prev, firstName: event.target.value }))}
						required
					/>
					<Input
						label='Last Name'
						value={form.lastName}
						onChange={(event) => setForm((prev) => ({ ...prev, lastName: event.target.value }))}
						required
					/>
					<Input
						label='Username'
						value={form.username}
						onChange={(event) => setForm((prev) => ({ ...prev, username: event.target.value }))}
						required
					/>
					<Input
						label='Email'
						type='email'
						value={form.email}
						onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
						required
					/>
					<Input
						label='Phone Number'
						value={form.phoneNumber}
						onChange={(event) => setForm((prev) => ({ ...prev, phoneNumber: event.target.value }))}
						required
					/>
					<Input
						label='Password'
						type='password'
						value={form.password}
						onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
						required
					/>
					<div className='sm:col-span-2 space-y-2'>
						{error ?
							<p className='text-sm text-red-600'>{error}</p>
						:	null}
						{success ?
							<p className='text-sm text-green-700'>{success}</p>
						:	null}
						<Button type='submit' className='w-full' disabled={loading}>
							{loading ? "Creating account..." : "Create Account"}
						</Button>
					</div>
				</form>
			</Card>
		</div>
	);
}
