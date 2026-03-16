import { writeFile, mkdir } from "fs/promises";
import { existsSync } from "fs";
import path from "path";
import { NextResponse } from "next/server";

export async function POST(request) {
  try {
    const data = await request.formData();
    const file = data.get("file");

    if (!file || typeof file === "string") {
      return NextResponse.json({ error: "No file provided" }, { status: 400 });
    }

    const allowedTypes = ["image/jpeg", "image/png", "image/webp", "image/gif"];
    if (!allowedTypes.includes(file.type)) {
      return NextResponse.json(
        { error: "Only JPEG, PNG, WebP and GIF images are allowed" },
        { status: 400 }
      );
    }

    if (file.size > 5 * 1024 * 1024) {
      return NextResponse.json(
        { error: "Image must be smaller than 5 MB" },
        { status: 400 }
      );
    }

    const ext = file.name.split(".").pop() || "jpg";
    const safeExt = ext.replace(/[^a-zA-Z0-9]/g, "");
    const filename = `${Date.now()}-${Math.random().toString(36).slice(2)}.${safeExt}`;

    const uploadsDir = path.join(process.cwd(), "public", "uploads");
    if (!existsSync(uploadsDir)) {
      await mkdir(uploadsDir, { recursive: true });
    }

    const bytes = await file.arrayBuffer();
    const buffer = Buffer.from(bytes);
    await writeFile(path.join(uploadsDir, filename), buffer);

    return NextResponse.json({ url: `/uploads/${filename}` });
  } catch (err) {
    console.error("Upload error:", err);
    return NextResponse.json({ error: "Upload failed" }, { status: 500 });
  }
}
