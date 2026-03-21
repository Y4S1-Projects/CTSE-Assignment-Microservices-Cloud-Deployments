"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Button from "@/components/common/Button";
import Card from "@/components/common/Card";
import Input from "@/components/common/Input";
import { loginUser } from "@/lib/authService";

export default function LoginPage() {
	const router = useRouter();
	const [form, setForm] = useState({ email: "", password: "" });
	const [error, setError] = useState("");
	const [loading, setLoading] = useState(false);

	async function handleSubmit(event) {
		event.preventDefault();
		setError("");
		setLoading(true);

		try {
			const response = await loginUser(form);
			const role = response?.role || response?.user?.role;
			window.location.href = role === "ADMIN" ? "/admin" : "/customer";
		} catch (submitError) {
			setError(submitError.message);
		} finally {
			setLoading(false);
		}
	}

	return (
		<div className='mx-auto w-full max-w-md'>
			<Card className='space-y-5'>
				<h1 className='text-2xl font-bold text-slate-900'>Sign in to GreenBite</h1>
				<p className='text-sm text-slate-600'>Continue to your food ordering dashboard.</p>
				<form onSubmit={handleSubmit} className='space-y-4'>
					<Input
						label='Email'
						type='email'
						value={form.email}
						onChange={(event) => setForm((prev) => ({ ...prev, email: event.target.value }))}
						required
					/>
					<Input
						label='Password'
						type='password'
						value={form.password}
						onChange={(event) => setForm((prev) => ({ ...prev, password: event.target.value }))}
						required
					/>
					{error ?
						<p className='text-sm text-red-600'>{error}</p>
					:	null}
					<Button type='submit' className='w-full' disabled={loading}>
						{loading ? "Signing in..." : "Sign In"}
					</Button>
				</form>
			</Card>
		</div>
	);
}
