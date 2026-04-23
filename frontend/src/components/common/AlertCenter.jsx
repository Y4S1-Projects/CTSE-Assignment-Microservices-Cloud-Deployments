"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ALERT_DISMISS_EVENT, ALERT_EVENT } from "@/lib/alerts";

const variantStyles = {
	success: {
		label: "Success",
		wrapper: "border-emerald-200 bg-emerald-50/95 text-emerald-950 shadow-emerald-950/10",
		accent: "bg-emerald-500",
		icon: "+",
	},
	error: {
		label: "Error",
		wrapper: "border-rose-200 bg-rose-50/95 text-rose-950 shadow-rose-950/10",
		accent: "bg-rose-500",
		icon: "!",
	},
	warning: {
		label: "Warning",
		wrapper: "border-amber-200 bg-amber-50/95 text-amber-950 shadow-amber-950/10",
		accent: "bg-amber-500",
		icon: "!",
	},
	info: {
		label: "Info",
		wrapper: "border-sky-200 bg-sky-50/95 text-sky-950 shadow-sky-950/10",
		accent: "bg-sky-500",
		icon: "i",
	},
};

function normalizeVariant(value) {
	return Object.prototype.hasOwnProperty.call(variantStyles, value) ? value : "info";
}

export default function AlertCenter() {
	const [alerts, setAlerts] = useState([]);
	const timersRef = useRef(new Map());

	const removeAlert = useCallback((id) => {
		if (!id) return;
		setAlerts((current) => current.filter((alert) => alert.id !== id));
		const timeoutId = timersRef.current.get(id);
		if (timeoutId) {
			window.clearTimeout(timeoutId);
			timersRef.current.delete(id);
		}
	}, []);

	useEffect(() => {
		function handleAlert(event) {
			const detail = event?.detail || {};
			const id = detail.id || `alert-${Date.now()}`;
			const variant = normalizeVariant(detail.variant);
			const title = detail.title || variantStyles[variant].label;
			const message = detail.message || "";
			const sticky = Boolean(detail.sticky);
			const duration = typeof detail.duration === "number" ? detail.duration : 4500;

			setAlerts((current) => {
				const next = [...current.filter((alert) => alert.id !== id), { id, variant, title, message }];
				return next.slice(-4);
			});

			if (!sticky) {
				const timeoutId = window.setTimeout(() => removeAlert(id), duration);
				timersRef.current.set(id, timeoutId);
			}
		}

		function handleDismiss(event) {
			removeAlert(event?.detail?.id);
		}

		window.addEventListener(ALERT_EVENT, handleAlert);
		window.addEventListener(ALERT_DISMISS_EVENT, handleDismiss);

		return () => {
			window.removeEventListener(ALERT_EVENT, handleAlert);
			window.removeEventListener(ALERT_DISMISS_EVENT, handleDismiss);
			timersRef.current.forEach((timeoutId) => window.clearTimeout(timeoutId));
			timersRef.current.clear();
		};
	}, [removeAlert]);

	if (alerts.length === 0) return null;

	return (
		<div className='pointer-events-none fixed right-4 top-4 z-[80] flex w-full max-w-sm flex-col gap-3 px-4 sm:right-6 sm:top-6 sm:px-0'>
			{alerts.map((alert) => {
				const tone = variantStyles[alert.variant] || variantStyles.info;
				return (
					<div
						key={alert.id}
						role={alert.variant === "error" ? "alert" : "status"}
						aria-live={alert.variant === "error" ? "assertive" : "polite"}
						className={`pointer-events-auto overflow-hidden rounded-2xl border backdrop-blur-xl ${tone.wrapper}`}>
						<div className={`h-1 w-full ${tone.accent}`} />
						<div className='flex items-start gap-3 p-4'>
							<div
								className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-sm font-bold text-white ${tone.accent}`}>
								{tone.icon}
							</div>
							<div className='min-w-0 flex-1 space-y-1 pr-6'>
								<p className='text-sm font-semibold'>{alert.title}</p>
								{alert.message ?
									<p className='text-sm leading-5 text-slate-700'>{alert.message}</p>
								:	null}
							</div>
							<button
								type='button'
								onClick={() => removeAlert(alert.id)}
								className='rounded-full p-1 text-slate-500 transition hover:bg-white/80 hover:text-slate-900'
								aria-label='Dismiss alert'>
								×
							</button>
						</div>
					</div>
				);
			})}
		</div>
	);
}
