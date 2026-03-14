import { NextResponse } from "next/server";

export const dynamic = "force-dynamic";

export function GET() {
	const apiBaseUrl = (process.env.NEXT_PUBLIC_API_BASE_URL || "").replace(/\/$/, "");

	return NextResponse.json({
		apiBaseUrl,
	});
}
