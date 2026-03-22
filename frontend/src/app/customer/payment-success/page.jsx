"use client";

import { useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Card from "@/components/common/Card";
import Button from "@/components/common/Button";
import { getPaymentByOrderId } from "@/lib/foodService";

function formatPrice(value) {
  const numeric = Number(value || 0);
  return `$${numeric.toFixed(2)}`;
}

export default function PaymentSuccessPage() {
  const router = useRouter();
  const params = useSearchParams();
  const orderId = params.get("orderId");
  const total = params.get("total");
  const [payment, setPayment] = useState(null);

    useEffect(() => {
    if (orderId) {
        getPaymentByOrderId(orderId)
        .then(setPayment)
        .catch(() => null);
    }
    // Clear any cached menu so customer page refetches fresh stock
    if (typeof window !== "undefined") {
        sessionStorage.removeItem("menuItems");
    }
    }, [orderId]);

  return (
    <div className="max-w-lg mx-auto py-12 space-y-6 text-center">
      {/* Success Icon */}
      <div className="flex justify-center">
        <div className="h-20 w-20 rounded-full bg-green-100 flex items-center justify-center">
          <svg className="h-10 w-10 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        </div>
      </div>

      <div>
        <h1 className="text-3xl font-bold text-slate-900">Payment Successful!</h1>
        <p className="mt-2 text-slate-500">Your order has been placed and payment confirmed.</p>
      </div>

      {/* Payment Details */}
      <Card className="text-left space-y-3">
        <h2 className="font-semibold text-slate-800">Payment Details</h2>
        {orderId && (
          <div className="flex justify-between text-sm">
            <span className="text-slate-500">Order ID</span>
            <span className="text-slate-800 font-mono text-xs">{orderId}</span>
          </div>
        )}
        {payment?.reference && (
          <div className="flex justify-between text-sm">
            <span className="text-slate-500">Reference</span>
            <span className="text-slate-800 font-semibold">{payment.reference}</span>
          </div>
        )}
        {total && (
          <div className="flex justify-between text-sm">
            <span className="text-slate-500">Amount Paid</span>
            <span className="text-green-700 font-bold">{formatPrice(total)}</span>
          </div>
        )}
        {payment?.paymentMethod && (
          <div className="flex justify-between text-sm">
            <span className="text-slate-500">Payment Method</span>
            <span className="text-slate-800">{payment.paymentMethod}</span>
          </div>
        )}
        {payment?.status && (
          <div className="flex justify-between text-sm">
            <span className="text-slate-500">Status</span>
            <span className="inline-flex items-center rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-semibold text-green-800">
              {payment.status}
            </span>
          </div>
        )}
        <p className="text-xs text-slate-400 pt-1">
          Stock has been updated in real-time in the catalog.
        </p>
      </Card>

      <div className="flex gap-3">
        <Button
          variant="secondary"
          className="flex-1"
          onClick={() => router.push("/customer")}
        >
          Back to Menu
        </Button>
        <Button
          className="flex-1"
          onClick={() => router.push("/customer")}
        >
          Place Another Order
        </Button>
      </div>
    </div>
  );
}