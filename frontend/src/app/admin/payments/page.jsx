"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Card from "@/components/common/Card";
import { getAllOrders } from "@/lib/foodService";
import { getAuthToken, isAdminUser } from "@/lib/storage";

function formatPrice(value) {
  return `$${Number(value || 0).toFixed(2)}`;
}

function StatusBadge({ status }) {
  const colors = {
    COMPLETED: "bg-green-100 text-green-800",
    PENDING: "bg-yellow-100 text-yellow-800",
    FAILED: "bg-red-100 text-red-800",
  };
  return (
    <span
      className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${colors[status] || "bg-slate-100 text-slate-700"}`}
    >
      {status}
    </span>
  );
}

export default function AdminPaymentsPage() {
  const router = useRouter();
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");

  useEffect(() => {
    if (!getAuthToken() || !isAdminUser()) {
      router.replace("/auth/login");
      return;
    }
    getAllOrders()
      .then(setPayments)
      .catch((e) => setError(e.message || "Failed to load payments"))
      .finally(() => setLoading(false));
  }, [router]);

  const filtered = payments.filter(
    (p) =>
      !search ||
      p.itemName?.toLowerCase().includes(search.toLowerCase()) ||
      p.paymentMethod?.toLowerCase().includes(search.toLowerCase()) ||
      p.status?.toLowerCase().includes(search.toLowerCase()) ||
      p.reference?.toLowerCase().includes(search.toLowerCase()) ||
      String(p.amount || "").includes(search),
  );

  const totalRevenue = payments
    .filter((p) => p.status === "COMPLETED")
    .reduce((sum, p) => sum + Number(p.amount || 0), 0);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-slate-900">Payment Dashboard</h1>
        <button
          onClick={() => router.push("/admin")}
          className="text-sm text-brand-600 hover:underline"
        >
          ← Back to Admin
        </button>
      </div>

      {/* Stats */}
      <div className="grid gap-4 sm:grid-cols-3">
        <Card className="text-center">
          <p className="text-xs font-semibold text-slate-500">Total Payments</p>
          <p className="text-3xl font-bold text-brand-700">{payments.length}</p>
        </Card>
        <Card className="text-center">
          <p className="text-xs font-semibold text-slate-500">Completed</p>
          <p className="text-3xl font-bold text-green-600">
            {payments.filter((p) => p.status === "COMPLETED").length}
          </p>
        </Card>
        <Card className="text-center">
          <p className="text-xs font-semibold text-slate-500">Total Revenue</p>
          <p className="text-3xl font-bold text-brand-700">
            {formatPrice(totalRevenue)}
          </p>
        </Card>
      </div>

      {/* Search */}
      <Card>
        <input
          type="text"
          placeholder="Search by item, method, status, or amount..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="w-full px-4 py-2 text-sm border rounded-xl border-brand-200 focus:border-brand-500 focus:outline-none"
        />
      </Card>

      {/* Table */}
      <Card>
        {loading && (
          <p className="text-sm text-slate-500">Loading payments...</p>
        )}
        {error && <p className="text-sm text-red-600">{error}</p>}
        {!loading && !error && (
          <div className="overflow-x-auto">
            <table className="min-w-full text-sm text-left">
              <thead>
                <tr className="text-xs font-semibold border-b border-brand-100 text-slate-500">
                  <th className="py-2 pr-4">Item</th>
                  <th className="py-2 pr-4">Amount</th>
                  <th className="py-2 pr-4">Method</th>
                  <th className="py-2 pr-4">Status</th>
                  <th className="py-2 pr-4">Date</th>
                </tr>
              </thead>
              <tbody>
                {filtered.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-6 text-center text-slate-400">
                      No payments found.
                    </td>
                  </tr>
                ) : (
                  filtered.map((p) => (
                    <tr
                      key={p.id}
                      className="border-b border-brand-50 text-slate-700 hover:bg-brand-50"
                    >
                      <td className="py-2 pr-4 text-xs">
                        {p.itemName || "Item"}
                      </td>
                      <td className="py-2 pr-4 font-semibold text-slate-900">
                        {formatPrice(p.amount)}
                      </td>
                      <td className="py-2 pr-4">{p.paymentMethod || "—"}</td>
                      <td className="py-2 pr-4">
                        <StatusBadge status={p.status} />
                      </td>
                      <td className="py-2 pr-4 text-xs text-slate-400">
                        {p.createdAt
                          ? new Date(p.createdAt).toLocaleDateString()
                          : "—"}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  );
}
