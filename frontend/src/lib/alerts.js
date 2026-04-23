const ALERT_EVENT = "greenbite-alert";
const ALERT_DISMISS_EVENT = "greenbite-alert-dismiss";

let alertSequence = 0;

export function notifyAlert({ variant = "info", title, message, duration = 4500, sticky = false } = {}) {
	if (typeof window === "undefined") return null;

	const id = `alert-${Date.now()}-${(alertSequence += 1)}`;
	window.dispatchEvent(
		new CustomEvent(ALERT_EVENT, {
			detail: {
				id,
				variant,
				title,
				message,
				duration,
				sticky,
			},
		}),
	);
	return id;
}

export function dismissAlert(id) {
	if (typeof window === "undefined" || !id) return;
	window.dispatchEvent(
		new CustomEvent(ALERT_DISMISS_EVENT, {
			detail: { id },
		}),
	);
}

export { ALERT_EVENT, ALERT_DISMISS_EVENT };
