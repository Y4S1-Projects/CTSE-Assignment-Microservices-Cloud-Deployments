"use client";

import { useRef, useState } from "react";

/**
 * Reusable image upload widget.
 *
 * Props:
 *  value      – current imageUrl string (URL or /uploads/... path)
 *  onChange   – called with the new URL string whenever it changes
 *  label      – field label (default "Item Image")
 */
export default function ImageUpload({ value, onChange, label = "Item Image" }) {
  const inputRef = useRef(null);
  const [uploading, setUploading] = useState(false);
  const [dragOver, setDragOver] = useState(false);
  const [uploadError, setUploadError] = useState("");

  async function uploadFile(file) {
    setUploadError("");
    setUploading(true);
    try {
      const form = new FormData();
      form.append("file", file);
      const res = await fetch("/api/upload", { method: "POST", body: form });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Upload failed");
      onChange(data.url);
    } catch (err) {
      setUploadError(err.message);
    } finally {
      setUploading(false);
    }
  }

  function handleFileInput(e) {
    const file = e.target.files?.[0];
    if (file) uploadFile(file);
  }

  function handleDrop(e) {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file) uploadFile(file);
  }

  const hasImage = value && value.trim() !== "";

  return (
    <div className="flex flex-col gap-1.5">
      <label className="block text-sm font-medium text-slate-700">{label}</label>

      {/* Drop zone / preview */}
      <div
        onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={handleDrop}
        className={`relative flex items-center justify-center rounded-xl border-2 border-dashed transition
          ${dragOver ? "border-brand-500 bg-brand-50" : "border-slate-300 bg-slate-50"}
          ${hasImage ? "h-40" : "h-32"}`}
      >
        {hasImage ? (
          <>
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={value}
              alt="preview"
              className="h-full w-full rounded-xl object-cover"
            />
            {/* Remove button */}
            <button
              type="button"
              onClick={() => { onChange(""); if (inputRef.current) inputRef.current.value = ""; }}
              className="absolute right-2 top-2 flex h-6 w-6 items-center justify-center rounded-full bg-red-600 text-xs text-white shadow hover:bg-red-700"
              title="Remove image"
            >
              ✕
            </button>
          </>
        ) : (
          <div className="flex flex-col items-center gap-1 text-slate-400 pointer-events-none">
            <span className="text-3xl">🖼️</span>
            <p className="text-xs">Drop image here or click below</p>
          </div>
        )}
        {uploading && (
          <div className="absolute inset-0 flex items-center justify-center rounded-xl bg-white/70">
            <span className="text-sm font-medium text-brand-600">Uploading…</span>
          </div>
        )}
      </div>

      {uploadError && (
        <p className="text-xs text-red-600">{uploadError}</p>
      )}

      {/* Buttons row */}
      <div className="flex items-center gap-2">
        <button
          type="button"
          disabled={uploading}
          onClick={() => inputRef.current?.click()}
          className="rounded-lg bg-brand-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-brand-700 disabled:opacity-50 transition"
        >
          {uploading ? "Uploading…" : hasImage ? "Replace Image" : "Choose File"}
        </button>
        <span className="text-xs text-slate-400">or paste a URL:</span>
        <input
          type="text"
          placeholder="https://..."
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="flex-1 rounded-lg border border-slate-300 px-2 py-1.5 text-xs focus:outline-none focus:ring-2 focus:ring-brand-400"
        />
      </div>

      {/* Hidden file input */}
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp,image/gif"
        className="hidden"
        onChange={handleFileInput}
      />
    </div>
  );
}
