import { NextResponse } from "next/server";

// Force Next.js to evaluate this route at request time, not build time
export const dynamic = "force-dynamic";

export async function GET() {
  return NextResponse.json({
    cloudinaryCloudName: process.env.CLOUDINARY_CLOUD_NAME || "",
    cloudinaryUploadPreset: process.env.CLOUDINARY_UPLOAD_PRESET || "",
  });
}
